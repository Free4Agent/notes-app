# ADR 002: WebDAV as Primary Sync Protocol

## Status
Accepted

## Context
User requirement: "own server via WebDAV as sync server"

Options considered:
1. **Custom protocol over WebSocket**
   - Pro: Optimized for our use case
   - Con: Users need our server software

2. **iCloud/Dropbox/Google Drive SDKs**
   - Pro: Easy for users
   - Con: Vendor lock-in, no self-hosting

3. **S3-compatible (MinIO, etc)**
   - Pro: Simple, scalable
   - Con: No partial file sync, no conflict detection

4. **WebDAV**
   - Pro: Universal standard, supported by all NAS/cloud software
   - Con: Verbose XML, older protocol

## Decision
**WebDAV** as primary sync protocol with S3 as future option.

## Implementation Details

### Sync Algorithm
```kotlin
// Simplified two-way sync
suspend fun sync() {
    val localChanges = db.getUnsyncedNotes()
    val remoteFiles = webdav.list("/notes/")
    
    // Download remote changes
    for (file in remoteFiles) {
        val local = db.getByPath(file.path)
        when {
            local == null -> download(file)
            file.modified > local.modified -> download(file)
            file.modified < local.modified -> upload(local) // local newer
            else -> { /* equal, skip */ }
        }
    }
    
    // Upload local-only changes
    for (note in localChanges) {
        if (note.path !in remoteFiles.map { it.path }) {
            upload(note)
        }
    }
}
```

### Conflict Resolution
- Timestamp-based last-write-wins
- Conflict file created: `Note.md.conflict.2024-01-15T10-30-00`
- User notification to resolve

### File Format
```markdown
---
id: uuid-here
created: 2024-01-15T10:00:00Z
modified: 2024-01-15T10:30:00Z
tags: [idea, work]
---

# Note Title

Content here...
```

## Consequences

### Positive
- Users can use existing Nextcloud/ownCloud/Synology
- Zero server maintenance for us
- Future-proof: WebDAV isn't going away

### Negative
- WebDAV libraries vary in quality
- Some servers have quirks (we'll maintain compatibility list)
- No real-time sync (polling-based)

## Tested Servers
- ✅ Nextcloud 25+
- ✅ ownCloud 10+
- ✅ Synology DSM 7+
- ✅ nginx + mod_dav
- ✅ Apache mod_dav
- ✅ ownCloud Infinite Scale
- ✅ Seafile (WebDAV extension)
