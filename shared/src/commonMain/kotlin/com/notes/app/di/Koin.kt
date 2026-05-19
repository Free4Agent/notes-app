package com.notes.app.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Initialize Koin dependency injection.
 */
fun initKoin(appModule: Module = module {}): KoinApplication {
    return startKoin {
        modules(
            commonModule,
            platformModule,
            appModule
        )
    }
}

/**
 * Common module with platform-agnostic dependencies.
 */
expect val platformModule: Module

val commonModule = module {
    // TODO: Add shared dependencies
    // single<SyncEngine> { SyncEngineImpl(get(), get()) }
    // single<NoteRepository> { NoteRepositoryImpl(get()) }
}
