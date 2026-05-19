package com.notes.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.notes.app.data.local.DriverFactory
import com.notes.app.database.NotesDatabase
import com.notes.app.domain.model.*
import com.notes.app.sync.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class NoteRepositoryImpl(driverFactory: DriverFactory) : NoteRepository {
    
    private val database = NotesDatabase(driverFactory.createDriver())
    private val queries = database.noteEntityQueries
    private val todoQueries = database.todoQueries
    
    override suspend fun getAllNotes(): List<Note> {
        return queries.getAllNotes().executeAsList().map { it.toNote() }
    }
    
    fun getAllNotesFlow(): Flow<List<Note>> {
        return queries.getAllNotes()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toNote() } }
    }
    
    override suspend fun getNoteById(id: String): Note? {
        return queries.getNoteById(id).executeAsOneOrNull()?.toNote()
    }
    
    override suspend fun getNotesByStatus(status: SyncStatus): List<Note> {
        return queries.getNotesByStatus(status.name).executeAsList().map { it.toNote() }
    }
    
    fun searchNotes(query: String): Flow<List<Note>> {
        return queries.searchNotes(query, query)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toNote() } }
    }
    
    fun getNotesByTag(tag: String): Flow<List<Note>> {
        return queries.getNotesByTag(tag)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toNote() } }
    }
    
    override suspend fun saveNote(note: Note) {
        val existing = queries.getNoteById(note.id).executeAsOneOrNull()
        
        if (existing == null) {
            queries.insertNote(
                id = note.id,
                title = note.title,
                content = note.content,
                created_at = note.createdAt.toEpochMilliseconds(),
                modified_at = note.modifiedAt.toEpochMilliseconds(),
                tags = note.tags.joinToString(","),
                sync_status = note.syncStatus.name
            )
        } else {
            queries.updateNote(
                title = note.title,
                content = note.content,
                modified_at = note.modifiedAt.toEpochMilliseconds(),
                tags = note.tags.joinToString(","),
                sync_status = note.syncStatus.name,
                id = note.id
            )
        }
    }
    
    override suspend fun updateSyncStatus(noteId: String, status: SyncStatus) {
        queries.updateSyncStatus(status.name, noteId)
    }
    
    suspend fun deleteNote(noteId: String) {
        queries.softDeleteNote(noteId)
    }
    
    suspend fun permanentDelete(noteId: String) {
        queries.deleteNotePermanently(noteId)
        todoQueries.deleteTodosByNote(noteId)
    }
    
    // Todo operations
    fun getAllTodosFlow(): Flow<List<Todo>> {
        return todoQueries.getAllTodos()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toTodo() } }
    }
    
    fun getPendingTodosFlow(): Flow<List<Todo>> {
        return todoQueries.getPendingTodos()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toTodo() } }
    }
    
    suspend fun saveTodo(todo: Todo) {
        todoQueries.insertTodo(
            id = todo.id,
            content = todo.content,
            is_completed = if (todo.isCompleted) 1L else 0L,
            created_at = todo.createdAt.toEpochMilliseconds(),
            completed_at = todo.completedAt?.toEpochMilliseconds(),
            due_date = todo.dueDate?.let { 
                Instant.parse("${it}T00:00:00Z").toEpochMilliseconds() 
            },
            note_id = todo.noteId,
            priority = todo.priority.name
        )
    }
    
    suspend fun updateTodo(todo: Todo) {
        todoQueries.updateTodo(
            content = todo.content,
            is_completed = if (todo.isCompleted) 1L else 0L,
            completed_at = todo.completedAt?.toEpochMilliseconds(),
            due_date = todo.dueDate?.let { 
                Instant.parse("${it}T00:00:00Z").toEpochMilliseconds() 
            },
            priority = todo.priority.name,
            id = todo.id
        )
    }
    
    suspend fun deleteTodo(todoId: String) {
        todoQueries.deleteTodo(todoId)
    }
    
    private fun com.notes.app.database.Note.toNote(): Note {
        return Note(
            id = id,
            title = title,
            content = content,
            createdAt = Instant.fromEpochMilliseconds(created_at),
            modifiedAt = Instant.fromEpochMilliseconds(modified_at),
            tags = tags.split(",").filter { it.isNotBlank() },
            isDeleted = is_deleted == 1L,
            syncStatus = SyncStatus.valueOf(sync_status)
        )
    }
    
    private fun com.notes.app.database.Todo.toTodo(): Todo {
        return Todo(
            id = id,
            content = content,
            isCompleted = is_completed == 1L,
            createdAt = Instant.fromEpochMilliseconds(created_at),
            completedAt = completed_at?.let { Instant.fromEpochMilliseconds(it) },
            dueDate = due_date?.let { 
                Instant.fromEpochMilliseconds(it)
                    .toLocalDateTime(TimeZone.UTC).date 
            },
            noteId = note_id,
            priority = priority?.let { Priority.valueOf(it) } ?: Priority.NONE
        )
    }
}
