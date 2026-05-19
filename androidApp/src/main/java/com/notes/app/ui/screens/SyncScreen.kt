package com.notes.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.notes.app.domain.model.WebDavConfig
import com.notes.app.sync.SyncEngine
import com.notes.app.ui.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val config by viewModel.webDavConfig.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    val autoSync by viewModel.autoSyncEnabled.collectAsState()

    var showSetup by remember { mutableStateOf(config == null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Sync Settings") },
                actions = {
                    if (config != null) {
                        IconButton(onClick = { showSetup = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (showSetup || config == null) {
            WebDavSetupForm(
                existingConfig = config,
                testResult = testResult,
                isTesting = isTesting,
                onTest = viewModel::testConnection,
                onSave = {
                    viewModel.saveWebDavConfig(it)
                    showSetup = false
                    viewModel.clearTestResult()
                },
                onCancel = {
                    if (config != null) showSetup = false
                    else onNavigateBack()
                },
                modifier = Modifier.padding(padding)
            )
        } else {
            SyncStatusPanel(
                config = config!!,
                autoSync = autoSync,
                onAutoSyncChange = viewModel::setAutoSync,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun WebDavSetupForm(
    existingConfig: WebDavConfig?,
    testResult: SettingsViewModel.TestResult?,
    isTesting: Boolean,
    onTest: (WebDavConfig) -> Unit,
    onSave: (WebDavConfig) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var url by remember { mutableStateOf(existingConfig?.baseUrl ?: "") }
    var username by remember { mutableStateOf(existingConfig?.username ?: "") }
    var password by remember { mutableStateOf(existingConfig?.password ?: "") }
    var path by remember { mutableStateOf(existingConfig?.remotePath ?: "/Notes") }
    var allowSelfSigned by remember { mutableStateOf(existingConfig?.allowSelfSigned ?: false) }
    var wifiOnly by remember { mutableStateOf(existingConfig?.syncOnWifiOnly ?: true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "WebDAV Server",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Server URL") },
            placeholder = { Text("https://nextcloud.example.com/remote.php/dav/files/user/") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = path,
            onValueChange = { path = it },
            label = { Text("Remote path") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Switch(
                checked = allowSelfSigned,
                onCheckedChange = { allowSelfSigned = it }
            )
            Text("Allow self-signed certificates")
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Switch(
                checked = wifiOnly,
                onCheckedChange = { wifiOnly = it }
            )
            Text("Sync on WiFi only")
        }

        // Test result display
        when (testResult) {
            is SettingsViewModel.TestResult.Success -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connection successful!")
                    }
                }
            }
            is SettingsViewModel.TestResult.Error -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(testResult.message)
                    }
                }
            }
            null -> {}
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            OutlinedButton(
                onClick = {
                    onTest(WebDavConfig(
                        baseUrl = url,
                        username = username,
                        password = password,
                        remotePath = path,
                        allowSelfSigned = allowSelfSigned,
                        syncOnWifiOnly = wifiOnly
                    ))
                },
                enabled = !isTesting && url.isNotBlank() && username.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Test")
                }
            }

            Button(
                onClick = {
                    onSave(WebDavConfig(
                        baseUrl = url,
                        username = username,
                        password = password,
                        remotePath = path,
                        allowSelfSigned = allowSelfSigned,
                        syncOnWifiOnly = wifiOnly
                    ))
                },
                enabled = url.isNotBlank() && username.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun SyncStatusPanel(
    config: WebDavConfig,
    autoSync: Boolean,
    onAutoSyncChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Connected",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Server: ${config.baseUrl}")
                Text("Path: ${config.remotePath}")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Auto Sync",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Sync every ${config.autoSyncIntervalMinutes} minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Switch(
                    checked = autoSync,
                    onCheckedChange = onAutoSyncChange
                )
            }
        }

        Button(
            onClick = { /* Trigger manual sync */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Sync, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sync Now")
        }
    }
}
