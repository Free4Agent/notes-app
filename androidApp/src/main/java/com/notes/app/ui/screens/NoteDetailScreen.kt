package com.notes.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notes.app.ui.components.MarkdownPreview
import com.notes.app.ui.viewmodel.NoteDetailViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: String,
    onNavigateBack: () -> Unit,
    onNoteDeleted: () -> Unit
) {
    val viewModel: NoteDetailViewModel = koinViewModel { org.koin.core.parameter.parametersOf(noteId) }
    val note by viewModel.note.collectAsStateWithLifecycle()
    val hasChanges by viewModel.hasChanges.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var isPreviewMode by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            if (hasChanges) {
                viewModel.saveNote()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges) viewModel.saveNote()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    note?.let {
                        Text(
                            text = it.title.ifBlank { "Untitled" },
                            maxLines = 1
                        )
                    }
                },
                actions = {
                    // Preview toggle
                    IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                        Icon(
                            imageVector = if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = if (isPreviewMode) "Edit" else "Preview"
                        )
                    }
                    
                    if (hasChanges) {
                        TextButton(onClick = viewModel::saveNote) {
                            Text("SAVE")
                        }
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { padding ->
        note?.let { currentNote ->
            if (isPreviewMode) {
                // Preview Mode
                MarkdownPreview(
                    markdown = buildString {
                        if (currentNote.title.isNotBlank()) {
                            appendLine("# ${currentNote.title}")
                            appendLine()
                        }
                        if (currentNote.tags.isNotEmpty()) {
                            appendLine(currentNote.tags.joinToString(" ") { "#$it" })
                            appendLine()
                        }
                        append(currentNote.content)
                    },
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                )
            } else {
                // Edit Mode
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    // Title field
                    BasicTextField(
                        value = currentNote.title,
                        onValueChange = viewModel::updateTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        textStyle = TextStyle(
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (currentNote.title.isEmpty()) {
                                Text(
                                    text = "Note title",
                                    style = TextStyle(fontSize = 24.sp),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            innerTextField()
                        }
                    )

                    // Tags row
                    if (currentNote.tags.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            currentNote.tags.forEach { tag ->
                                InputChip(
                                    selected = false,
                                    onClick = { },
                                    label = { Text(tag) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove tag",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Content field
                    BasicTextField(
                        value = currentNote.content,
                        onValueChange = viewModel::updateContent,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .verticalScroll(rememberScrollState()),
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (currentNote.content.isEmpty()) {
                                Text(
                                    text = "Start writing in Markdown...\n\n# Heading\n**bold** *italic*\n- list item\n- another item",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete note?") },
            text = { Text("This note will be moved to trash. You can restore it later.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNote(onNoteDeleted)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
