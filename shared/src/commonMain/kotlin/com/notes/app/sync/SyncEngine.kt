package com.notes.app.sync

import com.notes.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Core sync engine that orchestrates two-way synchronization.
 */
interface SyncEngine {
    
    /**
     * Current sync state.
     */
    val syncState: StateFlow<SyncState>
    
    /**
     * Flow of sync events for UI feedback.
     */
    val syncEvents: Flow<SyncEvent>
    
    /**
     * Configure WebDAV server.
     */
    suspend fun configure(config: WebDavConfig)
    
    /**
     * Test connection to WebDAV server.
     */
    suspend fun testConnection(config: WebDavConfig): Result<Unit>
    
    /**
     * Trigger a manual sync.
     */
    suspend fun syncNow(): SyncResult
    
    /**
     * Enable/disable auto-sync.
     */
    fun setAutoSync(enabled: Boolean)
    
    /**
     * Resolve a conflict by choosing local or remote version.
     */
    suspend fun resolveConflict(noteId: String, useLocal: Boolean)
    
    /**
     * Queue a note for immediate upload.
     */
    suspend fun queueUpload(noteId: String)
}

sealed class SyncState {
    data object Idle : SyncState()
    data object Checking : SyncState()
    data class Downloading(val current: Int, val total: Int) : SyncState()
    data class Uploading(val current: Int, val total: Int) : SyncState()
    data class Error(val message: String) : SyncState()
}

sealed class SyncEvent {
    data class NoteDownloaded(val noteId: String, val title: String) : SyncEvent()
    data class NoteUploaded(val noteId: String, val title: String) : SyncEvent()
    data class ConflictDetected(val noteId: String, val title: String) : SyncEvent()
    data class Error(val message: String) : SyncEvent()
}
