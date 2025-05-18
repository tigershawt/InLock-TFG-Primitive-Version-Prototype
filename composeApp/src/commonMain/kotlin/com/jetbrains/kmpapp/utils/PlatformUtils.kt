
@file:JvmName("PlatformUtilsCommon")
package com.jetbrains.kmpapp.utils

import kotlin.jvm.JvmName
import kotlinx.datetime.Clock

expect fun isEmulator(): Boolean
expect fun getPlatformName(): String
expect fun getDeviceId(): String


expect object PlatformUtils {
    fun getConfigValue(key: String): String?
}

fun getCurrentTimeMillis(): Long {
    return Clock.System.now().toEpochMilliseconds()
}