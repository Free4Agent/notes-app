package com.notes.app.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Core Note entity.
 * 
 * Notes are stored as Markdown files with YAML frontmatter.
 * File format: /notes/{id}.md
 */
@Serializable
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Instant,
    val modifiedAt: Instant,
    val tags: List<String> = emptyList(),
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
) {
    /**
     * Generates the filename for this note.
     * Format: {sanitized-title}-{id}.md
     */
    fun filename(): String {
        val sanitized = title
            .replace(Regex("[^a-zA-Z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .lowercase()
            .take(30)
        return "$sanitized-$id.md"
    }
    
    /**
     * Renders note as Markdown with YAML frontmatter.
     */
    fun toMarkdown(): String {
        val tagsYaml = if (tags.isEmpty()) "[]" else tags.joinToString(", ", "[", "]") { "\"$it\"" }
        
        return buildString {
            appendLine("---")
            appendLine("id: $id")
            appendLine("created: $createdAt")
            appendLine("modified: $modifiedAt")
            appendLine("tags: $tagsYaml")
            appendLine("---")
            appendLine()
            appendLine(content)
        }
    }
    
    companion object {
        /**
         * Parses a note from Markdown with YAML frontmatter.
         */
        fun fromMarkdown(filename: String, markdown: String): Note? {
            // TODO: Implement parsing
            return null
        }
    }
}

enum class SyncStatus {
    SYNCED,           // Local and remote are in sync
    PENDING_UPLOAD,   // Local changes need to be uploaded
    PENDING_DOWNLOAD, // Remote changes need to be downloaded
    CONFLICT          // Both local and remote have changes
}
