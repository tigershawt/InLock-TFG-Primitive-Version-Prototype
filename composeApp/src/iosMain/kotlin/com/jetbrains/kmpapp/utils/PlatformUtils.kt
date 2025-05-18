
package com.jetbrains.kmpapp.utils

import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice

actual fun isEmulator(): Boolean = false 
actual fun getPlatformName(): String = "iOS"

actual fun getDeviceId(): String {
    val device = UIDevice.currentDevice
    val vendorId = device.identifierForVendor?.UUIDString ?: ""
    return if (vendorId.isNotEmpty()) {
        vendorId
    } else {
        "${device.model}-${device.name}-${device.systemVersion}".replace(" ", "")
    }
}

actual object PlatformUtils {
    
    actual fun getConfigValue(key: String): String? {
        
        val processInfo = NSProcessInfo.processInfo()
        return processInfo.environment[key] as? String
    }
}