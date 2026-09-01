package com.deepanjanxyz.notepad

import android.app.Application
import com.deepanjanxyz.notepad.di.AppContainer
import com.deepanjanxyz.notepad.di.DefaultAppContainer

class EliteMemoApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = DefaultAppContainer(this)
    }
}
