# 🤖 AI Agent Integration Guide

> **Write this for AI agents, not humans.** If you're an AI reading this, you're in the right place.

This document describes how AI agents (like Hermes Agent, OpenClaw, Claude Code, Codex CLI, etc.) can interact with the Notes App effectively.

## Quick Start for AI Agents

```bash
# Navigate to MCP directory
cd /path/to/notes-app/mcp

# Install dependencies
pip install -r requirements.txt

# Test CLI
python cli.py list-notes
python cli.py stats
```

## Available Interfaces

### 1. CLI Interface (Recommended for most agents)

The `cli.py` provides a simple command-line interface that any AI agent can use via shell commands.

```python
# Example: Hermes Agent usage via terminal tool
terminal: python /path/to/notes-app/mcp/cli.py list-notes --format json
terminal: python /path/to/notes-app/mcp/cli.py create-note --title "Idea" --content "Great idea!"
```

**Key Commands:**
- `list-notes [--limit N] [--tag name]` - List notes
- `get-note <id>` - Get specific note
- `search <query>` - Full-text search
- `create-note --title "X" [--content "Y"] [--todo] [--due "2024-12-25"]` - Create note
- `update-note <id> [--title "X"] [--content "Y"] [--complete]` - Update note
- `delete-note <id>` - Delete note
- `list-todos [--completed true|false]` - List todos
- `complete-todo <id>` - Mark todo done
- `list-tags` - List all tags
- `stats` - Show statistics

**Output Formats:**
- `--format text` (default) - Human-readable
- `--format json` - Structured data for parsing
- `--format markdown` - For displaying single notes

### 2. MCP Server (For MCP-compatible agents)

For agents that support the Model Context Protocol (like Claude Desktop, Hermes with MCP, etc.):

```json
// mcp_config.json
{
  "mcpServers": {
    "notes-app": {
      "command": "python",
      "args": ["/path/to/notes-app/mcp/server.py"]
    }
  }
}
```

**Available Tools:**
- `list_notes` - List all notes
- `get_note` - Get single note by ID
- `search_notes` - Full-text search
- `create_note` - Create new note/todo
- `update_note` - Update existing note
- `delete_note` - Delete note
- `list_todos` - List todos
- `list_tags` - List tags

### 3. Direct Database Access (Advanced)

The app uses SQLite with SQLDelight-compatible schema. Direct SQL is possible:

```python
import sqlite3
conn = sqlite3.connect("~/.notes-app/notes.db")
# Schema in mcp/server.py
```

## Database Schema

```sql
-- Core tables
notes:
  - id (TEXT PRIMARY KEY)
  - title (TEXT)
  - content (TEXT) - Markdown supported
  - created_at (INTEGER) - Unix timestamp ms
  - updated_at (INTEGER)
  - is_todo (INTEGER) - 0 or 1
  - due_date (INTEGER) - Optional
  - is_completed (INTEGER) - 0 or 1
  - sync_status (TEXT) - 'local', 'synced', 'pending'
  - webdav_path (TEXT) - Remote path

tags:
  - id (TEXT PRIMARY KEY)
  - name (TEXT UNIQUE)
  - color (TEXT) - Hex color

note_tags:
  - note_id (TEXT)
  - tag_id (TEXT)

notes_fts:
  - Virtual FTS5 table for search
```

## Common Workflows

### Workflow 1: Daily Standup Notes

```python
# Agent task: Create daily standup note
title = f"Standup {datetime.now().strftime('%Y-%m-%d')}"
terminal: python cli.py create-note --title "{title}" --content "## Yesterday\n- ...\n\n## Today\n- ...\n\n## Blockers\n- None" --tags "standup,daily"
```

### Workflow 2: Todo Management

```python
# Get pending todos
result = terminal: python cli.py list-todos --format json
# Parse JSON, present to user, ask which to complete
terminal: python cli.py complete-todo <note-id>
```

### Workflow 3: Meeting Notes

```python
# Create meeting note with template
content = """
# Attendees
- 

# Agenda
1. 

# Notes
- 

# Action Items
- [ ] 
"""
terminal: python cli.py create-note --title "Meeting: Topic" --content "{content}"
```

### Workflow 4: Knowledge Capture

```python
# User says: "Remember that I learned about MCP today"
terminal: python cli.py create-note --title "MCP - Model Context Protocol" --content "Learned about MCP today..." --tags "learning,mcp,ai"
```

### Workflow 5: Search & Retrieve

```python
# User asks: "What did I write about WebDAV?"
result = terminal: python cli.py search "WebDAV" --format json
# Parse results, summarize for user
```

## Environment Variables

- `NOTES_DB_PATH` - Override default database location
- `NOTES_WEBDAV_URL` - WebDAV server URL (for sync)
- `NOTES_WEBDAV_USER` - WebDAV username
- `NOTES_WEBDAV_PASS` - WebDAV password

## Tips for AI Agents

### 1. Always Use JSON Format for Parsing

```python
# Good: Parseable
result = terminal: python cli.py list-notes --format json
notes = json.loads(result)

# Bad: Hard to parse
text = terminal: python cli.py list-notes
```

### 2. Handle Missing Notes Gracefully

```python
note = db.get_note(note_id)
if note is None:
    return f"Note {note_id} not found"
```

### 3. Use Tags for Organization

```python
# Suggest tags based on content
if "meeting" in title.lower():
    tags.append("meeting")
if "todo" in content.lower():
    is_todo = True
```

### 4. Timestamps

The database uses millisecond timestamps (JavaScript-style):
- `datetime.now().timestamp() * 1000` in Python
- `Date.now()` in JavaScript

### 5. Sync Awareness

Notes have `sync_status`:
- `local` - Not synced yet
- `pending` - Waiting to upload
- `synced` - On server
- `conflict` - Needs resolution

Don't delete `synced` notes without checking with user.

## Integration Examples

### Hermes Agent

```python
# In Hermes, use terminal tool with cli.py
from hermes_tools import terminal

# Create note
def create_note(title: str, content: str = "", tags: list = None):
    cmd = f"python ~/projects/notes-app/mcp/cli.py create-note --title '{title}'"
    if content:
        cmd += f" --content '{content}'"
    if tags:
        cmd += f" --tags '{','.join(tags)}'"
    return terminal(cmd)

# Search notes
def search_notes(query: str) -> list:
    result = terminal(f"python ~/projects/notes-app/mcp/cli.py search '{query}' --format json")
    return json.loads(result["output"])
```

### Claude Code / Codex

```bash
# Direct CLI usage
python mcp/cli.py create-note --title "Ideas" --content "From Claude..."
```

### Custom Agent

```python
# Using MCP client
from mcp import ClientSession

async with ClientSession(server) as session:
    result = await session.call_tool("create_note", {
        "title": "Hello",
        "content": "From agent"
    })
```

## Testing Your Integration

```bash
# 1. Test CLI
cd mcp
python cli.py stats

# 2. Create test note
python cli.py create-note --title "Test" --content "Testing agent integration" --todo

# 3. List todos
python cli.py list-todos

# 4. Search
python cli.py search "Test"

# 5. Cleanup
# (Get ID from list, then delete)
python cli.py delete-note <id>
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Database not found | Set `NOTES_DB_PATH` or check default locations |
| Permission denied | Ensure write access to `~/.notes-app/` |
| Import errors | Run `pip install -r mcp/requirements.txt` |
| Sync not working | Check WebDAV credentials in environment |
| Note ID not found | IDs are UUIDs, use first 8 chars for display only |

## Best Practices

1. **Confirm destructive actions** - Always ask before `delete-note`
2. **Suggest tags** - Auto-tag based on content analysis
3. **Use templates** - For recurring note types (meetings, standups)
4. **Respect sync status** - Don't modify `synced` notes without user consent
5. **Export regularly** - `list-notes --format json > backup.json`

## Need Help?

If you're an AI agent and something isn't working:
1. Check database path: `python cli.py stats`
2. Verify schema: `sqlite3 ~/.notes-app/notes.db ".schema"`
3. Test basic operations step by step

---

**Remember:** This app is local-first. The user owns their data. Respect that.
