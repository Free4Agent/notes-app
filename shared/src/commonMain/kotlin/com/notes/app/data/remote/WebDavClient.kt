package com.notes.app.data.remote

import com.notes.app.domain.model.WebDavConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.xml.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.*
import co.touchlab.kermit.Logger

/**
 * WebDAV client for syncing notes.
 * Implements RFC 4918 with support for common servers (Nextcloud, ownCloud, etc.)
 */
class WebDavClient(
    private val config: WebDavConfig,
    engine: HttpClientEngine
) {
    private val logger = Logger.withTag("WebDavClient")
    
    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            xml(format = XML {
                autoPolymorphism = true
                indentString = "  "
            })
        }
        
        install(Auth) {
            basic {
                credentials {
                    BasicAuthCredentials(
                        username = config.username,
                        password = config.password
                    )
                }
                sendWithoutRequest { true }
            }
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 10000
        }
        
        expectSuccess = false
        
        if (config.allowSelfSigned) {
            // Self-signed certs handled at engine level
        }
    }
    
    /**
     * Test connection by attempting to list directory.
     */
    suspend fun testConnection(): Result<Unit> {
        return try {
            logger.d { "Testing connection to ${config.baseUrl}" }
            val response = client.request(config.directoryUrl()) {
                method = HttpMethod("PROPFIND")
                header("Depth", "0")
                setBody(PROPFIND_BODY)
            }
            
            if (response.status.isSuccess()) {
                logger.i { "Connection successful" }
                Result.success(Unit)
            } else {
                val error = "HTTP ${response.status}"
                logger.e { "Connection failed: $error" }
                Result.failure(WebDavException(error))
            }
        } catch (e: Exception) {
            logger.e(e) { "Connection error" }
            Result.failure(e)
        }
    }
    
    /**
     * List files in the notes directory.
     */
    suspend fun listFiles(): Result<List<DavResource>> {
        return try {
            logger.d { "Listing files at ${config.directoryUrl()}" }
            
            val response = client.request(config.directoryUrl()) {
                method = HttpMethod("PROPFIND")
                header("Depth", "1")
                contentType(ContentType.Application.Xml)
                setBody(PROPFIND_BODY)
            }
            
            if (!response.status.isSuccess()) {
                return Result.failure(WebDavException("PROPFIND failed: ${response.status}"))
            }
            
            val body = response.bodyAsText()
            val resources = parsePropFindResponse(body)
            
            // Filter out the directory itself, only return .md files
            val files = resources
                .filter { it.href != config.remotePath && it.href != config.remotePath + "/" }
                .filter { it.href.endsWith(".md") }
            
            logger.i { "Found ${files.size} markdown files" }
            Result.success(files)
        } catch (e: Exception) {
            logger.e(e) { "Failed to list files" }
            Result.failure(e)
        }
    }
    
    /**
     * Download a file's content.
     */
    suspend fun downloadFile(remotePath: String): Result<String> {
        return try {
            logger.d { "Downloading $remotePath" }
            
            val url = if (remotePath.startsWith("http")) {
                remotePath
            } else {
                config.baseUrl.trimEnd('/') + "/" + remotePath.trimStart('/')
            }
            
            val response = client.get(url)
            
            if (!response.status.isSuccess()) {
                return Result.failure(WebDavException("GET failed: ${response.status}"))
            }
            
            val content = response.bodyAsText()
            logger.d { "Downloaded ${content.length} chars" }
            Result.success(content)
        } catch (e: Exception) {
            logger.e(e) { "Failed to download $remotePath" }
            Result.failure(e)
        }
    }
    
    /**
     * Upload content to a file.
     */
    suspend fun uploadFile(filename: String, content: String): Result<Unit> {
        return try {
            logger.d { "Uploading $filename (${content.length} chars)" }
            
            val url = config.fileUrl(filename)
            val response = client.put(url) {
                contentType(ContentType.Text.Plain)
                setBody(content)
            }
            
            if (!response.status.isSuccess()) {
                return Result.failure(WebDavException("PUT failed: ${response.status}"))
            }
            
            logger.i { "Upload successful: $filename" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(e) { "Failed to upload $filename" }
            Result.failure(e)
        }
    }
    
    /**
     * Delete a file from the server.
     */
    suspend fun deleteFile(filename: String): Result<Unit> {
        return try {
            logger.d { "Deleting $filename" }
            
            val url = config.fileUrl(filename)
            val response = client.delete(url)
            
            if (!response.status.isSuccess() && response.status != HttpStatusCode.NotFound) {
                return Result.failure(WebDavException("DELETE failed: ${response.status}"))
            }
            
            logger.i { "Delete successful: $filename" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(e) { "Failed to delete $filename" }
            Result.failure(e)
        }
    }
    
    /**
     * Create the notes directory if it doesn't exist.
     */
    suspend fun ensureDirectory(): Result<Unit> {
        return try {
            logger.d { "Creating directory ${config.directoryUrl()}" }
            
            val response = client.request(config.directoryUrl()) {
                method = HttpMethod("MKCOL")
            }
            
            // 201 = Created, 405 = Already exists (Method Not Allowed)
            if (response.status.isSuccess() || response.status == HttpStatusCode.MethodNotAllowed) {
                Result.success(Unit)
            } else {
                Result.failure(WebDavException("MKCOL failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parsePropFindResponse(xml: String): List<DavResource> {
        return try {
            // Simple XML parsing without full DOM
            val resources = mutableListOf<DavResource>()
            val responseRegex = Regex("<d:response>(.*?)</d:response>", RegexOption.DOT_MATCHES_ALL)
            val hrefRegex = Regex("<d:href>(.*?)</d:href>")
            val lastmodRegex = Regex("<d:getlastmodified>(.*?)</d:getlastmodified>")
            val contentLengthRegex = Regex("<d:getcontentlength>(\\d+)</d:getcontentlength>")
            val etagRegex = Regex("<d:getetag>(.*?)</d:getetag>")
            
            responseRegex.findAll(xml).forEach { match ->
                val responseXml = match.groupValues[1]
                val href = hrefRegex.find(responseXml)?.groupValues?.get(1) ?: return@forEach
                val lastModified = lastmodRegex.find(responseXml)?.groupValues?.get(1)
                val contentLength = contentLengthRegex.find(responseXml)?.groupValues?.get(1)?.toLongOrNull() ?: 0
                val etag = etagRegex.find(responseXml)?.groupValues?.get(1)
                
                resources.add(DavResource(
                    href = href,
                    lastModified = lastModified,
                    contentLength = contentLength,
                    etag = etag
                ))
            }
            
            resources
        } catch (e: Exception) {
            logger.e(e) { "Failed to parse PROPFIND response" }
            emptyList()
        }
    }
    
    companion object {
        private const val PROPFIND_BODY = """<?xml version="1.0" encoding="utf-8"?>
<d:propfind xmlns:d="DAV:">
  <d:prop>
    <d:getlastmodified/>
    <d:getcontentlength/>
    <d:getetag/>
  </d:prop>
</d:propfind>"""
    }
}

data class DavResource(
    val href: String,
    val lastModified: String?,
    val contentLength: Long,
    val etag: String?
) {
    val filename: String
        get() = href.substringAfterLast("/")
    
    val isDirectory: Boolean
        get() = href.endsWith("/")
}

class WebDavException(message: String) : Exception(message)
