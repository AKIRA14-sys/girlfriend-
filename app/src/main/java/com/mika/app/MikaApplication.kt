package com.mika.app

import android.app.Application
import android.util.Log
import com.mika.app.data.AppPreferences
import com.mika.app.data.db.AppDatabase

class MikaApplication : Application() {

    lateinit var preferences: AppPreferences
        private set

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferences = AppPreferences(this)
        database = AppDatabase.getInstance(this)
        Log.d("MikaApp", "MikaApplication initialized")
    }

    companion object {
        lateinit var instance: MikaApplication
            private set
    }
}
