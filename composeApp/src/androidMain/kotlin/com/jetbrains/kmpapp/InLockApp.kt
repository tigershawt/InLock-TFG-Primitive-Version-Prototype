package com.jetbrains.kmpapp

import android.app.Application
import android.os.StrictMode
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import com.jetbrains.kmpapp.di.initKoinAndroid
import com.jetbrains.kmpapp.ui.icons.AndroidIcons

class InLockApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .build()
            )
        }

        FirebaseApp.initializeApp(this)
        initKoinAndroid()
        AndroidIcons.init()
    }
}
