package com.notes.app.di

import org.koin.dsl.module

actual val platformModule = module {
    // Android-specific dependencies
    // single { createAndroidDatabaseDriver(context) }
}
