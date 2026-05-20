#!/usr/bin/env python3
"""
Notes App CLI - For AI Agents and Headless Operation

Provides command-line access to the Notes App database.
Can be used by AI agents like Hermes, OpenClaw, Claude Code, etc.

Usage:
    python cli.py list-notes
    python cli.py get-note <id>
    python cli.py create-note --title "Hello" --content "World"
    python cli.py search "query"
    python cli.py list-todos
    python cli.py complete-todo <id>
"""

import argparse
import json
import sys
import os
from datetime import datetime, timezone

# Add parent dir to path for imports
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from server import NotesDatabase


def format_timestamp(ts: int | None) -> str:
    """Convert millisecond timestamp to readable string."""
    if not ts:
        return ""
    try:
        dt = datetime.fromtimestamp(ts / 1000, timezone.utc)
        return dt.strftime("%Y-%m-%d %H:%M")
    except:
        return str(ts)


def print_json(data):
    """Print data as formatted JSON."""
    print(json.dumps(data, indent=2, default=str))


def print_markdown_note(note: dict):
    """Print a note in markdown format for easy reading."""
    print(f"# {note.get('title', 'Untitled')}\n")
    print(f"**ID:** `{note.get('id')}`")
    print(f"**Updated:** {format_timestamp(note.get('updated_at'))}")
    if note.get('is_todo'):
        status = "✓" if note.get('is_completed') else "○"
        print(f"**Status:** {status} {'Completed' if note.get('is_completed') else 'Pending'}")
    if note.get('due_date'):
        print(f"**Due:** {format_timestamp(note.get('due_date'))}")
    print(f"**Sync:** {note.get('sync_status', 'unknown')}")
    print()
    print(note.get('content', ''))


def cmd_list_notes(db: NotesDatabase, args):
    """List all notes."""
    notes = db.list_notes(limit=args.limit, offset=args.offset, tag=args.tag)
    if args.format == 'json':
        print_json(notes)
    else:
        print(f"Found {len(notes)} notes:\n")
        for note in notes:
            prefix = "[✓]" if note.get('is_completed') else "[ ]" if note.get('is_todo') else "   "
            date = format_timestamp(note.get('updated_at'))
            print(f"{prefix} {note.get('id')[:8]} │ {date} │ {note.get('title', 'Untitled')[:50]}")


def cmd_get_note(db: NotesDatabase, args):
    """Get a single note."""
    note = db.get_note(args.note_id)
    if not note:
        print(f"Error: Note not found: {args.note_id}", file=sys.stderr)
        sys.exit(1)

    if args.format == 'json':
        print_json(note)
    elif args.format == 'markdown':
        print_markdown_note(note)
    else:
        print(f"Title: {note.get('title')}")
        print(f"ID: {note.get('id')}")
        print(f"Updated: {format_timestamp(note.get('updated_at'))}")
        print(f"\n{note.get('content', '')}")


def cmd_search(db: NotesDatabase, args):
    """Search notes."""
    results = db.search_notes(args.query, limit=args.limit)
    if args.format == 'json':
        print_json(results)
    else:
        print(f"Search results for '{args.query}':\n")
        for note in results:
            date = format_timestamp(note.get('updated_at'))
            print(f"  {note.get('id')[:8]} │ {date} │ {note.get('title', 'Untitled')[:50]}")


def cmd_create_note(db: NotesDatabase, args):
    """Create a new note."""
    tags = args.tags.split(',') if args.tags else None
    due_date = None
    if args.due:
        try:
            dt = datetime.fromisoformat(args.due.replace('Z', '+00:00'))
            due_date = int(dt.timestamp() * 1000)
        except:
            print(f"Error: Invalid date format: {args.due}", file=sys.stderr)
            sys.exit(1)

    note = db.create_note(
        title=args.title,
        content=args.content or "",
        tags=tags,
        is_todo=args.todo,
        due_date=due_date
    )

    if args.format == 'json':
        print_json(note)
    else:
        print(f"Created note: {note.get('id')}")
        print(f"Title: {note.get('title')}")


def cmd_update_note(db: NotesDatabase, args):
    """Update a note."""
    note = db.update_note(
        note_id=args.note_id,
        title=args.title,
        content=args.content,
        is_completed=args.complete,
        due_date=None  # Not supported via CLI for simplicity
    )
    if not note:
        print(f"Error: Note not found: {args.note_id}", file=sys.stderr)
        sys.exit(1)

    if args.format == 'json':
        print_json(note)
    else:
        print(f"Updated note: {note.get('id')}")


def cmd_delete_note(db: NotesDatabase, args):
    """Delete a note."""
    success = db.delete_note(args.note_id)
    if not success:
        print(f"Error: Note not found: {args.note_id}", file=sys.stderr)
        sys.exit(1)
    print(f"Deleted note: {args.note_id}")


def cmd_list_todos(db: NotesDatabase, args):
    """List todos."""
    todos = db.list_todos(
        completed=args.completed if args.completed is not None else None
    )
    if args.format == 'json':
        print_json(todos)
    else:
        print(f"Found {len(todos)} todos:\n")
        for todo in todos:
            status = "✓" if todo.get('is_completed') else "○"
            due = format_timestamp(todo.get('due_date'))
            due_str = f" (due: {due})" if due else ""
            print(f"{status} {todo.get('title', 'Untitled')[:50]}{due_str}")


def cmd_complete_todo(db: NotesDatabase, args):
    """Mark a todo as complete."""
    note = db.update_note(
        note_id=args.note_id,
        is_completed=True
    )
    if not note:
        print(f"Error: Note not found: {args.note_id}", file=sys.stderr)
        sys.exit(1)
    print(f"Completed: {note.get('title')}")


def cmd_list_tags(db: NotesDatabase, args):
    """List all tags."""
    tags = db.list_tags()
    if args.format == 'json':
        print_json(tags)
    else:
        print("Tags:\n")
        for tag in tags:
            print(f"  {tag.get('name')} ({tag.get('note_count', 0)} notes)")


def cmd_stats(db: NotesDatabase, args):
    """Show database statistics."""
    all_notes = db.list_notes(limit=10000)
    todos = [n for n in all_notes if n.get('is_todo')]
    completed = [n for n in todos if n.get('is_completed')]

    stats = {
        "total_notes": len(all_notes),
        "total_todos": len(todos),
        "completed_todos": len(completed),
        "pending_todos": len(todos) - len(completed),
        "tags": len(db.list_tags())
    }

    if args.format == 'json':
        print_json(stats)
    else:
        print("Notes App Statistics")
        print("=" * 30)
        print(f"Total notes: {stats['total_notes']}")
        print(f"Todos: {stats['total_todos']} ({stats['completed_todos']} done, {stats['pending_todos']} pending)")
        print(f"Tags: {stats['tags']}")


def main():
    parser = argparse.ArgumentParser(
        description="Notes App CLI - For AI agents and headless operation",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # List all notes
  %(prog)s list-notes

  # Create a note
  %(prog)s create-note --title "Meeting Notes" --content "Discussed Q3 goals"

  # Create a todo
  %(prog)s create-note --title "Buy milk" --todo --due "2024-12-25"

  # Search notes
  %(prog)s search "meeting"

  # Get note details
  %(prog)s get-note <note-id>

  # Export all notes as JSON
  %(prog)s list-notes --format json > notes_backup.json

Environment Variables:
  NOTES_DB_PATH    Path to SQLite database (optional)
        """
    )

    parser.add_argument(
        '--db', '-d',
        default=os.environ.get('NOTES_DB_PATH'),
        help='Path to SQLite database'
    )
    parser.add_argument(
        '--format', '-f',
        choices=['text', 'json', 'markdown'],
        default='text',
        help='Output format (default: text)'
    )

    subparsers = parser.add_subparsers(dest='command', help='Commands')

    # list-notes
    list_parser = subparsers.add_parser('list-notes', help='List all notes')
    list_parser.add_argument('--limit', '-l', type=int, default=50)
    list_parser.add_argument('--offset', '-o', type=int, default=0)
    list_parser.add_argument('--tag', '-t', help='Filter by tag')

    # get-note
    get_parser = subparsers.add_parser('get-note', help='Get a single note')
    get_parser.add_argument('note_id', help='Note ID')

    # search
    search_parser = subparsers.add_parser('search', help='Search notes')
    search_parser.add_argument('query', help='Search query')
    search_parser.add_argument('--limit', '-l', type=int, default=20)

    # create-note
    create_parser = subparsers.add_parser('create-note', help='Create a new note')
    create_parser.add_argument('--title', '-t', required=True, help='Note title')
    create_parser.add_argument('--content', '-c', help='Note content')
    create_parser.add_argument('--tags', help='Comma-separated tags')
    create_parser.add_argument('--todo', action='store_true', help='Create as todo')
    create_parser.add_argument('--due', help='Due date (ISO 8601 format)')

    # update-note
    update_parser = subparsers.add_parser('update-note', help='Update a note')
    update_parser.add_argument('note_id', help='Note ID')
    update_parser.add_argument('--title', '-t', help='New title')
    update_parser.add_argument('--content', '-c', help='New content')
    update_parser.add_argument('--complete', action='store_true', help='Mark as complete')

    # delete-note
    delete_parser = subparsers.add_parser('delete-note', help='Delete a note')
    delete_parser.add_argument('note_id', help='Note ID')

    # list-todos
    todos_parser = subparsers.add_parser('list-todos', help='List todos')
    todos_parser.add_argument('--completed', type=bool, default=None,
                             help='Filter by completion status')

    # complete-todo
    complete_parser = subparsers.add_parser('complete-todo', help='Mark todo as complete')
    complete_parser.add_argument('note_id', help='Todo note ID')

    # list-tags
    subparsers.add_parser('list-tags', help='List all tags')

    # stats
    subparsers.add_parser('stats', help='Show statistics')

    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        sys.exit(1)

    # Initialize database
    db = NotesDatabase(args.db)

    # Route to command handler
    commands = {
        'list-notes': cmd_list_notes,
        'get-note': cmd_get_note,
        'search': cmd_search,
        'create-note': cmd_create_note,
        'update-note': cmd_update_note,
        'delete-note': cmd_delete_note,
        'list-todos': cmd_list_todos,
        'complete-todo': cmd_complete_todo,
        'list-tags': cmd_list_tags,
        'stats': cmd_stats,
    }

    handler = commands.get(args.command)
    if handler:
        handler(db, args)
    else:
        print(f"Unknown command: {args.command}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()