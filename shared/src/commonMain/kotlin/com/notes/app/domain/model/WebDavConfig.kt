package com.notes.app.domain.model

import kotlinx.serialization.Serializable

/**
 * WebDAV server configuration.
 */
@Serializable
data class WebDavConfig(
    val baseUrl: String,           // e.g., "https://nextcloud.example.com/remote.php/dav/files/user/"
    val username: String,
    val password: String,          // Stored encrypted
    val remotePath: String = "/Notes", // Directory on server
    val allowSelfSigned: Boolean = false,
    val syncOnWifiOnly: Boolean = true,
    val autoSyncIntervalMinutes: Int = 30
) {
    init {
        require(baseUrl.isNotBlank()) { "Base URL cannot be empty" }
        require(baseUrl.startsWith("http")) { "Base URL must start with http:// or https://" }
    }
    
    /**
     * Full URL for a specific note file.
     */
    fun fileUrl(filename: String): String {
        val base = baseUrl.trimEnd('/')
        val path = remotePath.trimStart('/').trimEnd('/')
        return "$base/$path/$filename"
    }
    
    /**
     * URL for the notes directory.
     */
    fun directoryUrl(): String {
        val base = baseUrl.trimEnd('/')
        val path = remotePath.trimStart('/').trimEnd('/')
        return "$base/$path/"
    }
}

/**
 * Result of a sync operation.
 */
data class SyncResult(
    val success: Boolean,
    val uploaded: Int = 0,
    val downloaded: Int = 0,
    val conflicts: List<Conflict> = emptyList(),
    val error: String? = null
)

data class Conflict(
    val noteId: String,
    val localModified: kotlinx.datetime.Instant,
    val remoteModified: kotlinx.datetime.Instant,
    val localContent: String,
    val remoteContent: String
)
