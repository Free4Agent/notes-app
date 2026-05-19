package com.notes.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.notes.app.domain.model.Priority
import com.notes.app.domain.model.Todo
import com.notes.app.ui.viewmodel.TodoViewModel
import com.notes.app.ui.viewmodel.nextWeek
import com.notes.app.ui.viewmodel.tomorrow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.DateTimeComponents
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    onNavigateBack: () -> Unit,
    viewModel: TodoViewModel = koinViewModel()
) {
    val todos by viewModel.todos.collectAsState()
    val pendingTodos by viewModel.pendingTodos.collectAsState()
    val overdueTodos by viewModel.overdueTodos.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(TodoFilter.ALL) }

    val filteredTodos = when (filter) {
        TodoFilter.ALL -> todos
        TodoFilter.PENDING -> pendingTodos
        TodoFilter.COMPLETED -> todos.filter { it.isCompleted }
        TodoFilter.OVERDUE -> overdueTodos
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Todos") },
                actions = {
                    // Filter dropdown
                    var showFilterMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        TodoFilter.entries.forEach { f ->
                            DropdownMenuItem(
                                text = { Text(f.label) },
                                onClick = {
                                    filter = f
                                    showFilterMenu = false
                                },
                                trailingIcon = if (filter == f) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Todo")
            }
        }
    ) { padding ->
        if (todos.isEmpty()) {
            EmptyTodoState(
                modifier = Modifier.padding(padding),
                onCreateClick = { showAddDialog = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Stats card
                item {
                    TodoStats(
                        total = todos.size,
                        pending = pendingTodos.size,
                        overdue = overdueTodos.size,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Filter chips
                item {
                    ScrollableFilterChips(
                        selected = filter,
                        onSelect = { filter = it },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Todo list
                items(filteredTodos, key = { it.id }) { todo ->
                    TodoItem(
                        todo = todo,
                        onToggle = { viewModel.toggleTodo(todo) },
                        onDelete = { viewModel.deleteTodo(todo.id) },
                        onPriorityChange = { viewModel.updateTodoPriority(todo.id, it) },
                        onDateChange = { viewModel.updateDueDate(todo.id, it) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddTodoDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { content, dueDate, priority ->
                viewModel.createTodo(content, dueDate, priority)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun TodoStats(
    total: Int,
    pending: Int,
    overdue: Int,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("Total", total, MaterialTheme.colorScheme.primary)
            StatItem("Pending", pending, MaterialTheme.colorScheme.secondary)
            StatItem("Overdue", overdue, MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun StatItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun ScrollableFilterChips(
    selected: TodoFilter,
    onSelect: (TodoFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TodoFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(filter.label) }
            )
        }
    }
}

@Composable
private fun TodoItem(
    todo: Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onPriorityChange: (Priority) -> Unit,
    onDateChange: (LocalDate?) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val priorityColor = when (todo.priority) {
        Priority.HIGH -> MaterialTheme.colorScheme.error
        Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary
        Priority.LOW -> MaterialTheme.colorScheme.secondary
        Priority.NONE -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (todo.isOverdue && !todo.isCompleted) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = { onToggle() }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (todo.priority != Priority.NONE) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .padding(end = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = priorityColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = todo.content,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (todo.isCompleted) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        },
                        color = if (todo.isCompleted) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                todo.dueDate?.let { date ->
                    val dateText = remember(date) {
                        date.toString() // Format as needed
                    }
                    Text(
                        text = "Due: $dateText",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (todo.isOverdue && !todo.isCompleted) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Set due date") },
                        onClick = {
                            showDatePicker = true
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("High priority") },
                        onClick = {
                            onPriorityChange(Priority.HIGH)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateSelected = {
                onDateChange(it)
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun AddTodoDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDate?, Priority) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.NONE) }
    var dueDate by remember { mutableStateOf<LocalDate?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Todo") },
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("What needs to be done?") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Priority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Due date", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = dueDate == null,
                        onClick = { dueDate = null },
                        label = { Text("None") }
                    )
                    FilterChip(
                        selected = dueDate == tomorrow(),
                        onClick = { dueDate = tomorrow() },
                        label = { Text("Tomorrow") }
                    )
                    FilterChip(
                        selected = dueDate == nextWeek(),
                        onClick = { dueDate = nextWeek() },
                        label = { Text("Next week") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(content, dueDate, priority) },
                enabled = content.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    // Simplified date picker - in real app use proper date picker
    val today = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select due date") },
        text = {
            Column {
                listOf(
                    today to "Today",
                    tomorrow() to "Tomorrow",
                    nextWeek() to "Next week"
                ).forEach { (date, label) ->
                    TextButton(
                        onClick = { onDateSelected(date) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EmptyTodoState(
    modifier: Modifier = Modifier,
    onCreateClick: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No todos yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add your first task to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Todo")
        }
    }
}

enum class TodoFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    COMPLETED("Completed"),
    OVERDUE("Overdue")
}
