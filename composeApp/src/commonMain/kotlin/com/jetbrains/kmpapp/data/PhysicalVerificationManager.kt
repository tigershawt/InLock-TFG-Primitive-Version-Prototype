package com.jetbrains.kmpapp.data

import kotlin.math.abs
import com.jetbrains.kmpapp.utils.getCurrentTimeMillis

class PhysicalVerificationManager {
    private val physicallyVerifiedAssets = mutableMapOf<String, Long>()
    private val physicalVerificationDuration = 60 * 60 * 1000L
    
    fun recordPhysicalVerification(assetId: String) {
        physicallyVerifiedAssets[assetId] = getCurrentTimeMillis()
    }
    
    fun hasRecentPhysicalVerification(assetId: String): Boolean {
        val verifiedTimestamp = physicallyVerifiedAssets[assetId] ?: return false
        val currentTime = getCurrentTimeMillis()
        val timeSinceVerification = abs(currentTime - verifiedTimestamp)
        return timeSinceVerification < physicalVerificationDuration
    }
    
    fun getPhysicalVerificationRemainingMs(assetId: String): Long {
        val verifiedTimestamp = physicallyVerifiedAssets[assetId] ?: return 0
        val currentTime = getCurrentTimeMillis()
        val timeSinceVerification = abs(currentTime - verifiedTimestamp)
        return (physicalVerificationDuration - timeSinceVerification).coerceAtLeast(0)
    }
    
    fun clearVerification(assetId: String) {
        physicallyVerifiedAssets.remove(assetId)
    }
    
    fun getAllPhysicallyVerifiedAssets(): Map<String, Long> {
        return physicallyVerifiedAssets.toMap()
    }
    
    
    companion object {
        private var instance: PhysicalVerificationManager? = null
        
        fun getInstance(): PhysicalVerificationManager {
            if (instance == null) {
                instance = PhysicalVerificationManager()
            }
            return instance!!
        }
    }
}