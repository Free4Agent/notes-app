# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [0.1.1] - 2025-05-20

### Added
- **AI Agent Integration** - Full MCP (Model Context Protocol) support
  - MCP Server for Model Context Protocol compatibility
  - CLI interface for headless AI agent operation
  - Support for Hermes Agent, OpenClaw, Claude Code, Codex CLI
  - Direct database access via SQLite
  - AGENTS.md documentation for AI assistants

- **Automated APK Builds** - GitHub Actions workflow
  - Automatic APK generation on every push to main
  - Signed release APKs for GitHub releases
  - Debug builds for testing
  - Obtainium-compatible release structure

- **Project Documentation**
  - Complete AGENTS.md guide for AI integration
  - SIGNING.md for APK signing setup
  - Updated README with installation instructions

### Technical
- Kotlin Multiplatform setup (shared module)
- SQLDelight database schema
- WebDAV sync architecture
- SQLite FTS for full-text search

### Notes
- This is an early MVP release
- Android app UI is still in development
- Core sync engine and data layer are functional
- AI agents can already create/manage notes via CLI

## [0.1.0] - 2025-05-19

### Added
- Initial project setup
- Kotlin Multiplatform architecture
- Basic note/todo data models
- WebDAV sync concept
- Local-first database design
