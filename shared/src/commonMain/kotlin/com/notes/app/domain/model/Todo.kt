package com.notes.app.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Todo item that can be embedded in notes or standalone.
 */
@Serializable
data class Todo(
    val id: String,
    val content: String,
    val isCompleted: Boolean = false,
    val createdAt: Instant,
    val completedAt: Instant? = null,
    val dueDate: LocalDate? = null,
    val recurrence: Recurrence? = null,
    val noteId: String? = null,  // Optional link to parent note
    val priority: Priority = Priority.NONE
) {
    val isOverdue: Boolean
        get() = dueDate?.let { 
            !isCompleted && it < kotlinx.datetime.Clock.System.now().toLocalDate() 
        } ?: false
    
    /**
     * Marks as complete. If recurring, returns the next occurrence.
     */
    fun complete(): Pair<Todo, Todo?> {
        val now = kotlinx.datetime.Clock.System.now()
        val completed = copy(
            isCompleted = true,
            completedAt = now
        )
        
        val next = recurrence?.nextOccurrence(dueDate ?: now.toLocalDate())?.let { nextDate ->
            Todo(
                id = generateId(),
                content = content,
                isCompleted = false,
                createdAt = now,
                dueDate = nextDate,
                recurrence = recurrence,
                noteId = noteId,
                priority = priority
            )
        }
        
        return completed to next
    }
}

enum class Priority {
    NONE, LOW, MEDIUM, HIGH
}

@Serializable
sealed class Recurrence {
    abstract fun nextOccurrence(from: LocalDate): LocalDate?
    
    @Serializable
    data class Daily(val interval: Int = 1) : Recurrence() {
        override fun nextOccurrence(from: LocalDate): LocalDate = 
            from.plus(kotlinx.datetime.DatePeriod(days = interval))
    }
    
    @Serializable
    data class Weekly(val interval: Int = 1, val daysOfWeek: List<Int> = emptyList()) : Recurrence() {
        override fun nextOccurrence(from: LocalDate): LocalDate {
            // TODO: Implement proper weekly recurrence
            return from.plus(kotlinx.datetime.DatePeriod(days = 7 * interval))
        }
    }
    
    @Serializable
    data class Monthly(val interval: Int = 1, val dayOfMonth: Int? = null) : Recurrence() {
        override fun nextOccurrence(from: LocalDate): LocalDate {
            // TODO: Implement proper monthly recurrence
            return from.plus(kotlinx.datetime.DatePeriod(months = interval))
        }
    }
}

private fun generateId(): String {
    // TODO: Use proper UUID generation
    return randomId()
}

private fun randomId(): String = 
    kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString(36)

private fun Instant.toLocalDate(): LocalDate =
    kotlinx.datetime.TimeZone.currentSystemDefault().let { tz ->
        toLocalDateTime(tz).date
    }
