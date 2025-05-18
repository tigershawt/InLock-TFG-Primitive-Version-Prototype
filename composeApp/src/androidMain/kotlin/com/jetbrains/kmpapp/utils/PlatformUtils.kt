@file:JvmName("PlatformUtilsAndroid")
package com.jetbrains.kmpapp.utils

import android.annotation.SuppressLint
import android.os.Build
import android.provider.Settings
import com.jetbrains.kmpapp.InLockApp

actual fun isEmulator(): Boolean {
    return Build.MODEL.contains("sdk") ||
            Build.PRODUCT.contains("sdk") ||
            Build.HARDWARE.contains("goldfish")
}

actual fun getPlatformName(): String = "Android"

@SuppressLint("HardwareIds")
actual fun getDeviceId(): String {
    return try {
        val context = com.jetbrains.kmpapp.MainActivity.appContext ?: return getDeviceInfo()
        
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        
        androidId ?: getDeviceInfo()
    } catch (e: Exception) {
        getDeviceInfo()
    }
}

private fun getDeviceInfo(): String {
    return "${Build.MANUFACTURER}-${Build.MODEL}-${Build.SERIAL}".replace(" ", "")
}

actual object PlatformUtils {
    
    actual fun getConfigValue(key: String): String? {
        
        return try {
            System.getProperty(key)
        } catch (e: Exception) {
            null
        }
    }
}