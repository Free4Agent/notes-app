package com.notes.app

import android.app.Application
import com.notes.app.di.appModule
import com.notes.app.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class NotesApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        initKoin(appModule) {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@NotesApplication)
        }
    }
}
