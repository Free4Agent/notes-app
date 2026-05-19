package com.notes.app.sync

import com.notes.app.data.remote.*
import com.notes.app.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import co.touchlab.kermit.Logger

/**
 * Implementation of the sync engine with WebDAV backend.
 */
class SyncEngineImpl(
    private val noteRepository: NoteRepository,
    private val webDavClientFactory: (com.notes.app.domain.model.WebDavConfig) -> WebDavClient
) : SyncEngine {
    
    private lateinit var webDavClient: WebDavClient
    
    private val logger = Logger.withTag("SyncEngine")
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private val _syncEvents = MutableSharedFlow<SyncEvent>()
    override val syncEvents: Flow<SyncEvent> = _syncEvents.asSharedFlow()
    
    private var autoSyncJob: Job? = null
    private var config: WebDavConfig? = null
    
    override suspend fun configure(newConfig: WebDavConfig) {
        this.config = newConfig
        this.webDavClient = webDavClientFactory(newConfig)
        logger.i { "Sync configured with server: ${newConfig.baseUrl}" }
        ensureDirectory()
    }
    
    override suspend fun testConnection(newConfig: WebDavConfig): Result<Unit> {
        val testClient = webDavClientFactory(newConfig)
        return testClient.testConnection()
    }
    
    override suspend fun syncNow(): SyncResult {
        if (_syncState.value is SyncState.Checking || 
            _syncState.value is SyncState.Downloading || 
            _syncState.value is SyncState.Uploading) {
            logger.w { "Sync already in progress, skipping" }
            return SyncResult(success = false, error = "Sync already in progress")
        }
        
        return try {
            _syncState.value = SyncState.Checking
            logger.i { "Starting sync" }
            
            // Get remote files
            val remoteFilesResult = webDavClient.listFiles()
            if (remoteFilesResult.isFailure) {
                val error = remoteFilesResult.exceptionOrNull()?.message ?: "Unknown error"
                _syncState.value = SyncState.Error(error)
                _syncEvents.emit(SyncEvent.Error(error))
                return SyncResult(success = false, error = error)
            }
            
            val remoteFiles = remoteFilesResult.getOrThrow()
            val localNotes = noteRepository.getAllNotes()
            
            var uploaded = 0
            var downloaded = 0
            val conflicts = mutableListOf<Conflict>()
            
            // Download phase
            _syncState.value = SyncState.Downloading(0, remoteFiles.size)
            remoteFiles.forEachIndexed { index, remoteFile ->
                _syncState.value = SyncState.Downloading(index + 1, remoteFiles.size)
                
                val localNote = localNotes.find { it.filename() == remoteFile.filename }
                
                when {
                    localNote == null -> {
                        // New file on server, download it
                        downloadNote(remoteFile)?.let {
                            downloaded++
                            _syncEvents.emit(SyncEvent.NoteDownloaded(it.id, it.title))
                        }
                    }
                    remoteFile.lastModified != null -> {
                        val remoteTime = parseWebDavDate(remoteFile.lastModified)
                        if (remoteTime > localNote.modifiedAt) {
                            // Remote is newer, download
                            downloadNote(remoteFile)?.let {
                                downloaded++
                                _syncEvents.emit(SyncEvent.NoteDownloaded(it.id, it.title))
                            }
                        } else if (remoteTime < localNote.modifiedAt && 
                                   localNote.syncStatus == SyncStatus.PENDING_UPLOAD) {
                            // Local is newer, will upload later
                            // Do nothing here
                        } else if (remoteTime < localNote.modifiedAt) {
                            // Conflict: both changed
                            handleConflict(localNote, remoteFile)?.let { conflict ->
                                conflicts.add(conflict)
                                _syncEvents.emit(SyncEvent.ConflictDetected(localNote.id, localNote.title))
                            }
                        }
                    }
                }
            }
            
            // Upload phase
            val pendingUploads = noteRepository.getNotesByStatus(SyncStatus.PENDING_UPLOAD)
            _syncState.value = SyncState.Uploading(0, pendingUploads.size)
            
            pendingUploads.forEachIndexed { index, note ->
                _syncState.value = SyncState.Uploading(index + 1, pendingUploads.size)
                
                val result = webDavClient.uploadFile(
                    note.filename(),
                    note.toMarkdown()
                )
                
                if (result.isSuccess) {
                    noteRepository.updateSyncStatus(note.id, SyncStatus.SYNCED)
                    uploaded++
                    _syncEvents.emit(SyncEvent.NoteUploaded(note.id, note.title))
                } else {
                    logger.e { "Failed to upload ${note.filename()}" }
                }
            }
            
            _syncState.value = SyncState.Idle
            logger.i { "Sync complete: $uploaded uploaded, $downloaded downloaded, ${conflicts.size} conflicts" }
            
            SyncResult(
                success = conflicts.isEmpty(),
                uploaded = uploaded,
                downloaded = downloaded,
                conflicts = conflicts
            )
            
        } catch (e: Exception) {
            logger.e(e) { "Sync failed" }
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            _syncEvents.emit(SyncEvent.Error(e.message ?: "Unknown error"))
            SyncResult(success = false, error = e.message)
        }
    }
    
    override fun setAutoSync(enabled: Boolean) {
        autoSyncJob?.cancel()
        
        if (enabled) {
            autoSyncJob = scope.launch {
                while (isActive) {
                    delay((config?.autoSyncIntervalMinutes ?: 30) * 60 * 1000L)
                    syncNow()
                }
            }
        }
    }
    
    override suspend fun resolveConflict(noteId: String, useLocal: Boolean) {
        val note = noteRepository.getNoteById(noteId) ?: return
        
        if (useLocal) {
            // Re-upload local version
            webDavClient.uploadFile(note.filename(), note.toMarkdown())
            noteRepository.updateSyncStatus(noteId, SyncStatus.SYNCED)
        } else {
            // Re-download remote version
            val remoteFiles = webDavClient.listFiles().getOrNull() ?: return
            val remoteFile = remoteFiles.find { it.filename == note.filename() } ?: return
            downloadNote(remoteFile)
        }
    }
    
    override suspend fun queueUpload(noteId: String) {
        noteRepository.updateSyncStatus(noteId, SyncStatus.PENDING_UPLOAD)
    }
    
    private suspend fun ensureDirectory() {
        webDavClient.ensureDirectory()
    }
    
    private suspend fun downloadNote(remoteFile: DavResource): Note? {
        val contentResult = webDavClient.downloadFile(remoteFile.href)
        if (contentResult.isFailure) {
            logger.e { "Failed to download ${remoteFile.filename}" }
            return null
        }
        
        val content = contentResult.getOrThrow()
        val note = Note.fromMarkdown(remoteFile.filename, content) ?: run {
            // Fallback: create simple note if parsing fails
            Note(
                id = generateId(),
                title = remoteFile.filename.removeSuffix(".md"),
                content = content,
                createdAt = parseWebDavDate(remoteFile.lastModified) ?: Clock.System.now(),
                modifiedAt = parseWebDavDate(remoteFile.lastModified) ?: Clock.System.now(),
                syncStatus = SyncStatus.SYNCED
            )
        }
        
        noteRepository.saveNote(note)
        return note
    }
    
    private suspend fun handleConflict(localNote: Note, remoteFile: DavResource): Conflict? {
        val remoteContentResult = webDavClient.downloadFile(remoteFile.href)
        if (remoteContentResult.isFailure) return null
        
        val remoteContent = remoteContentResult.getOrThrow()
        
        noteRepository.updateSyncStatus(localNote.id, SyncStatus.CONFLICT)
        
        return Conflict(
            noteId = localNote.id,
            localModified = localNote.modifiedAt,
            remoteModified = parseWebDavDate(remoteFile.lastModified) ?: Clock.System.now(),
            localContent = localNote.toMarkdown(),
            remoteContent = remoteContent
        )
    }
    
    private fun parseWebDavDate(dateString: String?): kotlinx.datetime.Instant? {
        if (dateString == null) return null
        
        // WebDAV dates are typically RFC 1123 format
        return try {
            kotlinx.datetime.Instant.parse(dateString)
        } catch (e: Exception) {
            try {
                // Try other common formats
                kotlinx.datetime.LocalDateTime.parse(dateString.replace("Z", ""))
                    .toInstant(kotlinx.datetime.TimeZone.UTC)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private fun generateId(): String {
        return Clock.System.now().toEpochMilliseconds().toString(36)
    }
    

}

interface NoteRepository {
    suspend fun getAllNotes(): List<Note>
    suspend fun getNoteById(id: String): Note?
    suspend fun getNotesByStatus(status: SyncStatus): List<Note>
    suspend fun saveNote(note: Note)
    suspend fun updateSyncStatus(noteId: String, status: SyncStatus)
}
