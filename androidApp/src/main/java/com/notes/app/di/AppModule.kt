package com.notes.app.di

import com.notes.app.data.local.DriverFactory
import com.notes.app.data.remote.WebDavClient
import com.notes.app.data.remote.createHttpEngine
import com.notes.app.data.repository.NoteRepositoryImpl
import com.notes.app.sync.NoteRepository
import com.notes.app.sync.SyncEngine
import com.notes.app.sync.SyncEngineImpl
import com.notes.app.ui.viewmodel.NoteDetailViewModel
import com.notes.app.ui.viewmodel.NoteListViewModel
import com.notes.app.ui.viewmodel.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database
    single { DriverFactory(androidContext()) }
    single { NoteRepositoryImpl(get()) }
    single<NoteRepository> { get<NoteRepositoryImpl>() }

    // WebDAV
    factory { (config: com.notes.app.domain.model.WebDavConfig) ->
        WebDavClient(config, createHttpEngine(config.allowSelfSigned))
    }

    // Sync
    single<SyncEngine> {
        SyncEngineImpl(get()) { config ->
            WebDavClient(config, createHttpEngine(config.allowSelfSigned))
        }
    }

    // ViewModels
    viewModel { NoteListViewModel(get()) }
    viewModel { (noteId: String) ->
        NoteDetailViewModel(
            savedStateHandle = androidx.lifecycle.SavedStateHandle(mapOf("noteId" to noteId)),
            repository = get()
        )
    }
    viewModel { SettingsViewModel(androidContext(), get()) }
}
