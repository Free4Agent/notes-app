#!/usr/bin/env python3
"""
Notes App MCP Server

Provides Model Context Protocol tools for AI agents to interact with
the Notes App database directly.

Run: python server.py
"""

import json
import sqlite3
import os
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from mcp.server import Server
from mcp.server.stdio import stdio_server
from mcp.types import Tool, TextContent


class NotesDatabase:
    """Direct access to Notes App SQLite database."""

    def __init__(self, db_path: str = None):
        if db_path is None:
            # Default: look for database in standard locations
            home = Path.home()
            possible_paths = [
                home / ".notes-app" / "notes.db",
                home / ".local" / "share" / "notes-app" / "notes.db",
                Path("/data/data/com.notes.app/databases/notes.db"),  # Android
            ]
            for p in possible_paths:
                if p.exists():
                    db_path = str(p)
                    break
            else:
                # Create default location
                db_path = str(home / ".notes-app" / "notes.db")
                os.makedirs(os.path.dirname(db_path), exist_ok=True)

        self.db_path = db_path
        self._ensure_tables()

    def _ensure_tables(self):
        """Ensure database has required tables (SQLDelight compatible)."""
        with sqlite3.connect(self.db_path) as conn:
            conn.executescript("""
                CREATE TABLE IF NOT EXISTS notes (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    content TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    is_todo INTEGER DEFAULT 0,
                    due_date INTEGER,
                    is_completed INTEGER DEFAULT 0,
                    sync_status TEXT DEFAULT 'local',
                    webdav_path TEXT,
                    tags TEXT DEFAULT '[]'
                );

                CREATE TABLE IF NOT EXISTS tags (
                    id TEXT PRIMARY KEY,
                    name TEXT UNIQUE NOT NULL,
                    color TEXT DEFAULT '#2196F3',
                    created_at INTEGER NOT NULL
                );

                CREATE TABLE IF NOT EXISTS note_tags (
                    note_id TEXT,
                    tag_id TEXT,
                    PRIMARY KEY (note_id, tag_id),
                    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE,
                    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
                );

                CREATE VIRTUAL TABLE IF NOT EXISTS notes_fts USING fts5(
                    title, content, content='notes', content_rowid='rowid'
                );

                CREATE INDEX IF NOT EXISTS idx_notes_updated ON notes(updated_at DESC);
                CREATE INDEX IF NOT EXISTS idx_notes_todo ON notes(is_todo, is_completed);
                CREATE INDEX IF NOT EXISTS idx_notes_due ON notes(due_date);
            """)

    def _now(self) -> int:
        return int(datetime.now(timezone.utc).timestamp() * 1000)

    def list_notes(self, limit: int = 50, offset: int = 0, tag: str = None) -> list[dict]:
        """List all notes, optionally filtered by tag."""
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            if tag:
                cursor = conn.execute("""
                    SELECT n.* FROM notes n
                    JOIN note_tags nt ON n.id = nt.note_id
                    JOIN tags t ON nt.tag_id = t.id
                    WHERE t.name = ?
                    ORDER BY n.updated_at DESC
                    LIMIT ? OFFSET ?
                """, (tag, limit, offset))
            else:
                cursor = conn.execute("""
                    SELECT * FROM notes
                    ORDER BY updated_at DESC
                    LIMIT ? OFFSET ?
                """, (limit, offset))
            return [dict(row) for row in cursor.fetchall()]

    def get_note(self, note_id: str) -> dict | None:
        """Get a single note by ID."""
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.execute("SELECT * FROM notes WHERE id = ?", (note_id,))
            row = cursor.fetchone()
            return dict(row) if row else None

    def search_notes(self, query: str, limit: int = 20) -> list[dict]:
        """Full-text search notes."""
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            # Use FTS if available, fallback to LIKE
            try:
                cursor = conn.execute("""
                    SELECT n.* FROM notes n
                    JOIN notes_fts fts ON n.rowid = fts.rowid
                    WHERE notes_fts MATCH ?
                    ORDER BY rank
                    LIMIT ?
                """, (query, limit))
            except sqlite3.OperationalError:
                # FTS not available, use LIKE
                cursor = conn.execute("""
                    SELECT * FROM notes
                    WHERE title LIKE ? OR content LIKE ?
                    ORDER BY updated_at DESC
                    LIMIT ?
                """, (f"%{query}%", f"%{query}%", limit))
            return [dict(row) for row in cursor.fetchall()]

    def create_note(self, title: str, content: str = "", tags: list[str] = None,
                    is_todo: bool = False, due_date: int = None) -> dict:
        """Create a new note."""
        import uuid
        now = self._now()
        note_id = str(uuid.uuid4())

        with sqlite3.connect(self.db_path) as conn:
            conn.execute("""
                INSERT INTO notes (id, title, content, created_at, updated_at,
                                 is_todo, due_date, sync_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'local')
            """, (note_id, title, content, now, now, int(is_todo), due_date))

            # Add tags if provided
            if tags:
                for tag_name in tags:
                    tag_id = self._ensure_tag(conn, tag_name)
                    conn.execute("""
                        INSERT OR IGNORE INTO note_tags (note_id, tag_id)
                        VALUES (?, ?)
                    """, (note_id, tag_id))

        return self.get_note(note_id)

    def _ensure_tag(self, conn, tag_name: str) -> str:
        """Get or create a tag, return tag ID."""
        import uuid
        cursor = conn.execute("SELECT id FROM tags WHERE name = ?", (tag_name,))
        row = cursor.fetchone()
        if row:
            return row[0]

        tag_id = str(uuid.uuid4())
        conn.execute("""
            INSERT INTO tags (id, name, created_at)
            VALUES (?, ?, ?)
        """, (tag_id, tag_name, self._now()))
        return tag_id

    def update_note(self, note_id: str, title: str = None, content: str = None,
                    is_completed: bool = None, due_date: int = None) -> dict | None:
        """Update a note."""
        note = self.get_note(note_id)
        if not note:
            return None

        updates = ["updated_at = ?"]
        params = [self._now()]

        if title is not None:
            updates.append("title = ?")
            params.append(title)
        if content is not None:
            updates.append("content = ?")
            params.append(content)
        if is_completed is not None:
            updates.append("is_completed = ?")
            params.append(int(is_completed))
        if due_date is not None:
            updates.append("due_date = ?")
            params.append(due_date)

        params.append(note_id)

        with sqlite3.connect(self.db_path) as conn:
            conn.execute(f"""
                UPDATE notes SET {', '.join(updates)}
                WHERE id = ?
            """, params)

        return self.get_note(note_id)

    def delete_note(self, note_id: str) -> bool:
        """Delete a note."""
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.execute("DELETE FROM notes WHERE id = ?", (note_id,))
            return cursor.rowcount > 0

    def list_todos(self, completed: bool = None, due_before: int = None) -> list[dict]:
        """List todo items."""
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            query = "SELECT * FROM notes WHERE is_todo = 1"
            params = []

            if completed is not None:
                query += " AND is_completed = ?"
                params.append(int(completed))
            if due_before is not None:
                query += " AND due_date <= ?"
                params.append(due_before)

            query += " ORDER BY due_date ASC, updated_at DESC"

            cursor = conn.execute(query, params)
            return [dict(row) for row in cursor.fetchall()]

    def list_tags(self) -> list[dict]:
        """List all tags with note counts."""
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            cursor = conn.execute("""
                SELECT t.*, COUNT(nt.note_id) as note_count
                FROM tags t
                LEFT JOIN note_tags nt ON t.id = nt.tag_id
                GROUP BY t.id
                ORDER BY note_count DESC
            """)
            return [dict(row) for row in cursor.fetchall()]


# MCP Server Setup
app = Server("notes-app")
db = None


@app.list_tools()
async def list_tools() -> list[Tool]:
    return [
        Tool(
            name="list_notes",
            description="List all notes, optionally filtered by tag",
            inputSchema={
                "type": "object",
                "properties": {
                    "limit": {"type": "integer", "default": 50},
                    "offset": {"type": "integer", "default": 0},
                    "tag": {"type": "string", "description": "Filter by tag name"}
                }
            }
        ),
        Tool(
            name="get_note",
            description="Get a single note by its ID",
            inputSchema={
                "type": "object",
                "properties": {
                    "note_id": {"type": "string", "description": "The note ID"}
                },
                "required": ["note_id"]
            }
        ),
        Tool(
            name="search_notes",
            description="Search notes by text content",
            inputSchema={
                "type": "object",
                "properties": {
                    "query": {"type": "string", "description": "Search query"},
                    "limit": {"type": "integer", "default": 20}
                },
                "required": ["query"]
            }
        ),
        Tool(
            name="create_note",
            description="Create a new note or todo",
            inputSchema={
                "type": "object",
                "properties": {
                    "title": {"type": "string", "description": "Note title"},
                    "content": {"type": "string", "description": "Note content (Markdown supported)"},
                    "tags": {"type": "array", "items": {"type": "string"}, "description": "Tags to apply"},
                    "is_todo": {"type": "boolean", "default": False},
                    "due_date": {"type": "integer", "description": "Due date as Unix timestamp (ms)"}
                },
                "required": ["title"]
            }
        ),
        Tool(
            name="update_note",
            description="Update an existing note",
            inputSchema={
                "type": "object",
                "properties": {
                    "note_id": {"type": "string"},
                    "title": {"type": "string"},
                    "content": {"type": "string"},
                    "is_completed": {"type": "boolean"},
                    "due_date": {"type": "integer"}
                },
                "required": ["note_id"]
            }
        ),
        Tool(
            name="delete_note",
            description="Delete a note",
            inputSchema={
                "type": "object",
                "properties": {
                    "note_id": {"type": "string"}
                },
                "required": ["note_id"]
            }
        ),
        Tool(
            name="list_todos",
            description="List todo items",
            inputSchema={
                "type": "object",
                "properties": {
                    "completed": {"type": "boolean", "description": "Filter by completion status"},
                    "due_before": {"type": "integer", "description": "Filter by due date (Unix timestamp ms)"}
                }
            }
        ),
        Tool(
            name="list_tags",
            description="List all tags with note counts"
        ),
    ]


@app.call_tool()
async def call_tool(name: str, arguments: Any) -> list[TextContent]:
    global db
    if db is None:
        db = NotesDatabase()

    result = None

    if name == "list_notes":
        result = db.list_notes(
            limit=arguments.get("limit", 50),
            offset=arguments.get("offset", 0),
            tag=arguments.get("tag")
        )
    elif name == "get_note":
        result = db.get_note(arguments["note_id"])
    elif name == "search_notes":
        result = db.search_notes(arguments["query"], arguments.get("limit", 20))
    elif name == "create_note":
        result = db.create_note(
            title=arguments["title"],
            content=arguments.get("content", ""),
            tags=arguments.get("tags"),
            is_todo=arguments.get("is_todo", False),
            due_date=arguments.get("due_date")
        )
    elif name == "update_note":
        result = db.update_note(
            note_id=arguments["note_id"],
            title=arguments.get("title"),
            content=arguments.get("content"),
            is_completed=arguments.get("is_completed"),
            due_date=arguments.get("due_date")
        )
    elif name == "delete_note":
        result = db.delete_note(arguments["note_id"])
    elif name == "list_todos":
        result = db.list_todos(
            completed=arguments.get("completed"),
            due_before=arguments.get("due_before")
        )
    elif name == "list_tags":
        result = db.list_tags()
    else:
        raise ValueError(f"Unknown tool: {name}")

    return [TextContent(type="text", text=json.dumps(result, indent=2, default=str))]


async def main():
    async with stdio_server() as (read_stream, write_stream):
        await app.run(read_stream, write_stream, app.create_initialization_options())


if __name__ == "__main__":
    import asyncio
    asyncio.run(main())