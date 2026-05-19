package com.notes.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notes.app.data.repository.NoteRepositoryImpl
import com.notes.app.domain.model.Priority
import com.notes.app.domain.model.Todo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

class TodoViewModel(
    private val repository: NoteRepositoryImpl
) : ViewModel() {

    val todos: StateFlow<List<Todo>> = repository.getAllTodosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTodos: StateFlow<List<Todo>> = repository.getPendingTodosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overdueTodos: StateFlow<List<Todo>> = flow {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        emitAll(repository.getAllTodosFlow().map { todos ->
            todos.filter { it.isOverdue }
        })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createTodo(
        content: String,
        dueDate: LocalDate? = null,
        priority: Priority = Priority.NONE
    ) {
        val todo = Todo(
            id = generateId(),
            content = content,
            createdAt = Clock.System.now(),
            dueDate = dueDate,
            priority = priority
        )
        viewModelScope.launch {
            repository.saveTodo(todo)
        }
    }

    fun toggleTodo(todo: Todo) {
        viewModelScope.launch {
            if (todo.isCompleted) {
                // Uncomplete
                repository.updateTodo(todo.copy(
                    isCompleted = false,
                    completedAt = null
                ))
            } else {
                // Complete - handle recurrence
                val (completed, next) = todo.complete()
                repository.updateTodo(completed)
                next?.let { repository.saveTodo(it) }
            }
        }
    }

    fun deleteTodo(todoId: String) {
        viewModelScope.launch {
            repository.deleteTodo(todoId)
        }
    }

    fun updateTodoPriority(todoId: String, priority: Priority) {
        viewModelScope.launch {
            // Get todo from current list and update
            val todo = todos.value.find { it.id == todoId } ?: return@launch
            repository.updateTodo(todo.copy(priority = priority))
        }
    }

    fun updateDueDate(todoId: String, dueDate: LocalDate?) {
        viewModelScope.launch {
            val todo = todos.value.find { it.id == todoId } ?: return@launch
            repository.updateTodo(todo.copy(dueDate = dueDate))
        }
    }

    private fun generateId(): String {
        return Clock.System.now().toEpochMilliseconds().toString(36)
    }
}

// Extension for quick date creation
fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
fun tomorrow(): LocalDate = today().plus(DatePeriod(days = 1))
fun nextWeek(): LocalDate = today().plus(DatePeriod(days = 7))
