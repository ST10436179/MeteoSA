package com.meteosa.app

import android.app.Application
import android.util.Log
import com.meteosa.app.di.AppContainer

class MeteoSaApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Log.i(TAG, "MeteoSA application started (backend=${BuildConfig.BACKEND_BASE_URL})")
    }

    companion object {
        private const val TAG = "MeteoSaApplication"
    }
}
