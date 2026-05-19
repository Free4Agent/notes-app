package com.notes.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.notes.app.data.repository.NoteRepositoryImpl
import com.notes.app.domain.model.Note
import com.notes.app.domain.model.SyncStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class NoteDetailViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: NoteRepositoryImpl
) : ViewModel() {

    private val noteId: String = checkNotNull(savedStateHandle["noteId"])

    private val _note = MutableStateFlow<Note?>(null)
    val note = _note.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _hasChanges = MutableStateFlow(false)
    val hasChanges = _hasChanges.asStateFlow()

    init {
        loadNote()
    }

    private fun loadNote() {
        viewModelScope.launch {
            _isLoading.value = true
            _note.value = repository.getNoteById(noteId)
            _isLoading.value = false
        }
    }

    fun updateTitle(title: String) {
        _note.value?.let { currentNote ->
            _note.value = currentNote.copy(
                title = title,
                modifiedAt = Clock.System.now(),
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
            _hasChanges.value = true
        }
    }

    fun updateContent(content: String) {
        _note.value?.let { currentNote ->
            _note.value = currentNote.copy(
                content = content,
                modifiedAt = Clock.System.now(),
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
            _hasChanges.value = true
        }
    }

    fun updateTags(tags: List<String>) {
        _note.value?.let { currentNote ->
            _note.value = currentNote.copy(
                tags = tags,
                modifiedAt = Clock.System.now(),
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
            _hasChanges.value = true
        }
    }

    fun saveNote() {
        viewModelScope.launch {
            _note.value?.let { note ->
                repository.saveNote(note)
                _hasChanges.value = false
            }
        }
    }

    fun deleteNote(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            onDeleted()
        }
    }

    companion object {
        fun factory(repository: NoteRepositoryImpl) = viewModelFactory {
            initializer {
                NoteDetailViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    repository = repository
                )
            }
        }
    }
}
