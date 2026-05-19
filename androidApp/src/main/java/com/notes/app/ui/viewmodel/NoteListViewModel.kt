package com.notes.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notes.app.data.repository.NoteRepositoryImpl
import com.notes.app.domain.model.Note
import com.notes.app.domain.model.SyncStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class NoteListViewModel(
    private val repository: NoteRepositoryImpl
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val notes: StateFlow<List<Note>> = repository.getAllNotesFlow()
        .combine(_searchQuery) { allNotes, query ->
            if (query.isBlank()) {
                allNotes
            } else {
                allNotes.filter { note ->
                    note.title.contains(query, ignoreCase = true) ||
                            note.content.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = repository.getAllNotesFlow()
        .map { notes -> notes.count { it.syncStatus != SyncStatus.SYNCED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun createNote(): String {
        val id = generateId()
        val now = Clock.System.now()
        val note = Note(
            id = id,
            title = "",
            content = "",
            createdAt = now,
            modifiedAt = now,
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
        viewModelScope.launch {
            repository.saveNote(note)
        }
        return id
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }

    private fun generateId(): String {
        return Clock.System.now().toEpochMilliseconds().toString(36)
    }
}
