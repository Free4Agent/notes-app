package com.notes.app.ui.viewmodel

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notes.app.data.remote.WebDavClient
import com.notes.app.domain.model.WebDavConfig
import com.notes.app.sync.SyncEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit
import com.notes.app.data.remote.createHttpEngine

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsViewModel(
    private val context: Context,
    private val syncEngine: SyncEngine
) : ViewModel() {

    private val dataStore = context.dataStore

    private val WEBDAV_URL = stringPreferencesKey("webdav_url")
    private val WEBDAV_USER = stringPreferencesKey("webdav_user")
    private val WEBDAV_PASS = stringPreferencesKey("webdav_pass")
    private val WEBDAV_PATH = stringPreferencesKey("webdav_path")
    private val ALLOW_SELF_SIGNED = booleanPreferencesKey("allow_self_signed")
    private val WIFI_ONLY = booleanPreferencesKey("wifi_only")
    private val AUTO_SYNC = booleanPreferencesKey("auto_sync")

    val webDavConfig: StateFlow<WebDavConfig?> = dataStore.data
        .map { prefs ->
            val url = prefs[WEBDAV_URL]
            if (url.isNullOrBlank()) return@map null

            WebDavConfig(
                baseUrl = url,
                username = prefs[WEBDAV_USER] ?: "",
                password = prefs[WEBDAV_PASS] ?: "",
                remotePath = prefs[WEBDAV_PATH] ?: "/Notes",
                allowSelfSigned = prefs[ALLOW_SELF_SIGNED] ?: false,
                syncOnWifiOnly = prefs[WIFI_ONLY] ?: true,
                autoSyncIntervalMinutes = 30
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val autoSyncEnabled: StateFlow<Boolean> = dataStore.data
        .map { it[AUTO_SYNC] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _testResult = MutableStateFlow<TestResult?>(null)
    val testResult = _testResult.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting = _isTesting.asStateFlow()

    fun saveWebDavConfig(config: WebDavConfig) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[WEBDAV_URL] = config.baseUrl
                prefs[WEBDAV_USER] = config.username
                prefs[WEBDAV_PASS] = config.password
                prefs[WEBDAV_PATH] = config.remotePath
                prefs[ALLOW_SELF_SIGNED] = config.allowSelfSigned
                prefs[WIFI_ONLY] = config.syncOnWifiOnly
            }
            syncEngine.configure(config)
        }
    }

    fun testConnection(config: WebDavConfig) {
        viewModelScope.launch {
            _isTesting.value = true
            _testResult.value = null

            val result = syncEngine.testConnection(config)

            _testResult.value = if (result.isSuccess) {
                TestResult.Success
            } else {
                TestResult.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
            _isTesting.value = false
        }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AUTO_SYNC] = enabled
            }
            syncEngine.setAutoSync(enabled)
        }
    }

    fun clearTestResult() {
        _testResult.value = null
    }

    sealed class TestResult {
        object Success : TestResult()
        data class Error(val message: String) : TestResult()
    }
}
