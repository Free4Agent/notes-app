# ADR 001: Kotlin Multiplatform for Cross-Platform Development

## Status
Accepted

## Context
We need to support Android (MVP) and later iOS/Desktop. Options:

1. **Native Android + later rewrite/port**
   - Pro: Best Android experience
   - Con: Duplicated logic, sync bugs, maintenance nightmare

2. **Flutter**
   - Pro: Single UI codebase, fast development
   - Con: Non-native feel, WebDAV plugin ecosystem weak

3. **React Native**
   - Pro: Large ecosystem
   - Con: JS bridge overhead, WebDAV libs problematic

4. **Kotlin Multiplatform**
   - Pro: Shared business logic, native UI, first-class WebDAV (Ktor client)
   - Con: iOS needs Mac for builds, smaller community than Flutter

## Decision
Use **Kotlin Multiplatform** with:
- `shared` module: Sync engine, database, markdown parsing
- `androidApp`: Jetpack Compose UI
- Future: `iosApp` with SwiftUI, sharing the same core

## Consequences

### Positive
- Sync logic identical across platforms → no sync bugs from platform differences
- Native UI feels right on each platform
- WebDAV implementation in pure Kotlin, no platform-specific quirks
- Can publish shared module as library for contributors

### Negative
- iOS development requires Mac hardware
- Team needs Kotlin + Swift knowledge
- Smaller stack overflow answers than Flutter/RN

## References
- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [SQLDelight Multiplatform](https://cashapp.github.io/sqldelight/2.0.0/multiplatform/)
- [Ktor Client WebDAV](https://ktor.io/)
