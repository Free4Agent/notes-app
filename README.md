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
| **AI-Agent compatible** | Built-in MCP server & CLI for AI assistants. |

## 🤖 AI Agent Support

This app is designed to work seamlessly with AI agents like **Hermes Agent**, **OpenClaw**, **Claude Code**, **Codex CLI**, and any MCP-compatible agent.

### Quick Start for AI Agents

```bash
# Use the CLI from anywhere
python mcp/cli.py list-notes
python mcp/cli.py create-note --title "Idea from AI" --content "..."
python mcp/cli.py search "meeting notes"

# Or use the MCP Server for deeper integration
# See AGENTS.md for full documentation
```

### What AI Agents Can Do

- 📝 **Create notes** from conversations, meetings, ideas
- ✅ **Manage todos** - add, complete, organize tasks
- 🔍 **Search** your knowledge base instantly
- 🏷️ **Auto-tag** notes based on content
- 📊 **Generate summaries** and reports
- 🔄 **Sync** with your WebDAV server

**Full guide:** [AGENTS.md](AGENTS.md)

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
│   │   ├── data/         # SQLDelight database, WebDAV client
│   │   ├── domain/       # Models and use cases
│   │   └── sync/         # Sync engine
│   └── src/androidMain/
│   └── src/iosMain/           # (future)
│
├── androidApp/                # Android-specific code (Compose UI)
├── mcp/                       # 🤖 AI Agent Integration
│   ├── server.py               # MCP Server (Model Context Protocol)
│   ├── cli.py                  # Command-line interface for agents
│   └── requirements.txt
│
├── docs/                      # Documentation
├── AGENTS.md                  # 🤖 Guide for AI agents
├── build.gradle.kts
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
