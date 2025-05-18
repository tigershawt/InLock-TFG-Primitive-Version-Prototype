package com.jetbrains.kmpapp.data

import kotlin.random.Random
import com.jetbrains.kmpapp.utils.getCurrentTimeMillis

class TransferPermissionManager {
    private val verifiedAssets = mutableMapOf<String, Long>()
    private val physicalVerificationManager = PhysicalVerificationManager.getInstance()
    private val permissionDuration = 60 * 60 * 1000L
    private val codeCharacters = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    
    fun generateConfirmationCode(): String {
        val prefix = (0..1).map { codeCharacters.random() }.joinToString("")
        val suffix = (0..2).map { Random.nextInt(0, 10) }.joinToString("")
        return prefix + suffix
    }
    
    fun markAssetVerified(assetId: String) {
        verifiedAssets[assetId] = getCurrentTimeMillis()
    }
    
    fun hasValidTransferPermission(assetId: String): Boolean {
        val codeVerificationValid = hasValidCodeVerification(assetId)
        val physicalVerificationValid = physicalVerificationManager.hasRecentPhysicalVerification(assetId)
        return codeVerificationValid && physicalVerificationValid
    }
    
    fun hasValidCodeVerification(assetId: String): Boolean {
        val timestamp = verifiedAssets[assetId] ?: return false
        val currentTime = getCurrentTimeMillis()
        return (currentTime - timestamp) < permissionDuration
    }
    
    fun getTransferPermissionExpiry(assetId: String): Long {
        val codeExpiry = getCodeVerificationExpiry(assetId)
        val physicalExpiry = getCurrentTimeMillis() + physicalVerificationManager.getPhysicalVerificationRemainingMs(assetId)
        return minOf(codeExpiry, physicalExpiry)
    }
    
    private fun getCodeVerificationExpiry(assetId: String): Long {
        val timestamp = verifiedAssets[assetId] ?: return 0
        return timestamp + permissionDuration
    }
    
    fun getPhysicalVerificationTimeRemaining(assetId: String): String {
        val remainingMs = physicalVerificationManager.getPhysicalVerificationRemainingMs(assetId)
        if (remainingMs <= 0) return "Expired"
        
        val minutes = remainingMs / (60 * 1000)
        val seconds = (remainingMs % (60 * 1000)) / 1000
        return "$minutes min $seconds sec"
    }
    
    fun clearPermission(assetId: String) {
        verifiedAssets.remove(assetId)
        physicalVerificationManager.clearVerification(assetId)
    }
    
    companion object {
        private var instance: TransferPermissionManager? = null
        
        fun getInstance(): TransferPermissionManager {
            if (instance == null) {
                instance = TransferPermissionManager()
            }
            return instance!!
        }
    }
}