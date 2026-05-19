# Notes App

> A minimalist, open-source note-taking & todo app with self-hosted WebDAV sync.

## Vision

**Your notes. Your server. No compromises.**

A truly local-first note app that syncs on *your* terms. Use it offline forever, or plug in your own WebDAV server (Nextcloud, ownCloud, Synology, nginx, whatever) for seamless sync across devices.

No cloud lock-in. No subscription. No tracking. Just Markdown files on your device, optionally mirrored to your server.

## Core Principles

| Principle | What it means |
|-----------|---------------|
| **Local-first** | Works 100% offline. Data lives on device. |
| **Self-hosted sync** | You control the server. WebDAV is the universal glue. |
| **Plain Markdown** | Files you can read with any text editor, forever. |
| **Fast & Native** | 60fps, native UI, instant startup. |
| **Privacy by default** | No accounts, no analytics, no phoning home. |

## Tech Stack

```
Shared (Kotlin Multiplatform):
  - Sync Engine (WebDAV client)
  - Local Database (SQLDelight)
  - Markdown Parser (Multiplatform Markdown)
  - Search (SQLite FTS)

Android (Jetpack Compose):
  - Material You design
  - Native share/save intents
  - Widget support

Future: iOS (SwiftUI), Desktop (Compose Desktop)
```

## Architecture

```
┌─────────────────────────────────────────┐
│           Presentation Layer            │
│    (Compose UI / SwiftUI / etc)         │
├─────────────────────────────────────────┤
│            Domain Layer                 │
│  (Notes, Todos, Sync, Search Use Cases) │
├─────────────────────────────────────────┤
│           Data Layer                    │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐ │
│  │ Local   │  │ WebDAV  │  │ Search  │ │
│  │ SQLite  │  │ Client  │  │  FTS    │ │
│  └─────────┘  └─────────┘  └─────────┘ │
└─────────────────────────────────────────┘
```

## Sync Strategy

**Conflict Resolution**: Last-write-wins with conflict markers

```
Sync Flow:
1. Local change → Queue sync job
2. Every 30s or on app resume → Check remote
3. Download remote changes → Apply locally
4. Upload local changes → Mark synced
5. On conflict: Keep both, mark conflict, user resolves
```

**WebDAV Requirements**:
- Any RFC 4918 compliant server
- Tested with: Nextcloud, ownCloud, Synology, nginx-webdav, Apache mod_dav
- Supports: HTTP Basic Auth, Bearer tokens (optional)

## Features

### MVP (Android)
- [ ] Create/edit/delete notes (Markdown)
- [ ] Todo lists with due dates
- [ ] Tag organization
- [ ] Full-text search
- [ ] WebDAV sync setup
- [ ] Offline mode (works without sync)
- [ ] Material You theming
- [ ] Export/import Markdown files

### v1.0
- [ ] Biometric app lock
- [ ] Note widgets
- [ ] Quick capture from share sheet
- [ ] Daily notes / journaling
- [ ] Note templates
- [ ] Backlinks
- [ ] Import from Obsidian

### Future
- [ ] iOS app (share KMP core)
- [ ] Desktop app (Linux/macOS/Windows)
- [ ] End-to-end encryption option
- [ ] Collaborative editing (WebDAV locks)

## Project Structure

```
notes-app/
├── shared/                     # Kotlin Multiplatform module
│   ├── src/commonMain/kotlin/
│   │   ├── data/
│   │   │   ├── local/         # SQLDelight database
│   │   │   ├── remote/        # WebDAV client
│   │   │   └── repository/    # Repository pattern
│   │   ├── domain/
│   │   │   ├── model/         # Note, Todo, Tag, etc.
│   │   │   └── usecase/       # Business logic
│   │   └── sync/
│   │       ├── SyncEngine.kt
│   │       └── ConflictResolver.kt
│   └── src/androidMain/
│   └── src/iosMain/           # (future)
│
├── androidApp/                # Android-specific code
│   ├── src/main/
│   │   ├── java/com/notes/app/
│   │   │   ├── ui/           # Compose screens
│   │   │   ├── di/           # Dependency injection (Koin)
│   │   │   └── MainActivity.kt
│   │   └── res/              # Resources
│   └── build.gradle.kts
│
├── docs/
│   ├── ADR/                  # Architecture Decision Records
│   ├── WEBDAV_SETUP.md       # Server setup guides
│   └── API.md
│
├── build.gradle.kts          # Root build script
└── settings.gradle.kts
```

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17+
- Kotlin 1.9+

### Build & Run

```bash
# Clone
git clone https://github.com/Free4Agent/notes-app.git
cd notes-app

# Build shared module
./gradlew :shared:build

# Install debug APK
./gradlew :androidApp:installDebug

# Or open in Android Studio and run
```

### WebDAV Test Server

```bash
# Quick local test with nginx
docker run -d -p 8080:80 -v $(pwd)/test-dav:/var/lib/dav \
  -e USERNAME=test -e PASSWORD=test \
  --name webdav morrisjobke/webdav

# Then in app: http://localhost:8080, user: test, pass: test
```

## Contributing

1. Fork the repo
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit with [Conventional Commits](https://www.conventionalcommits.org/)
4. Push and open a PR

## License

MIT — see [LICENSE](LICENSE)

## Acknowledgments

- Inspired by Obsidian, Standard Notes, and the local-first software movement
- WebDAV sync inspired by Joplin and KeepassDX
