package com.jetbrains.kmpapp.data

import com.jetbrains.kmpapp.utils.isEmulator
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock

class BlockchainService(private val httpClient: HttpClient) {
    

    private val baseUrl = when {
        isEmulator() ->
            "http://10.0.2.2:6000"
        else -> {
            "http://192.168.50.97:6000"
        }
    }

    suspend fun getHealthStatus(): Result<String> = runCatching {
        try {
            withTimeout(5000) {
                val response = httpClient.get("$baseUrl/health")
                val healthResponse: Map<String, Any> = response.body()
                val activeBlockchains = healthResponse["active_blockchains"] as? Int ?: 0
                val minConsensus = healthResponse["min_consensus"] as? Int ?: 0

                return@withTimeout "Blockchain orchestrator: ${healthResponse["status"] ?: "unknown"}, " +
                        "Active nodes: $activeBlockchains, Min consensus: $minConsensus"
            }
        } catch (e: TimeoutCancellationException) {
            throw Exception("Blockchain service health check timed out")
        } catch (e: Exception) {
            throw Exception("Failed to connect to blockchain service: ${e.message}")
        }
    }

    suspend fun processNfcTag(
        tagId: String,
        userId: String,
        tagTechnologies: List<String>,
        ndefMessage: String,
        timestamp: Long = Clock.System.now().toEpochMilliseconds()
    ): Result<NfcTagProcessResponse> = runCatching {
        withTimeout(10000) {
            val response = httpClient.post("$baseUrl/process_nfc_tag") {
                contentType(ContentType.Application.Json)
                setBody(
                    NfcTagRequest(
                        tag_id = tagId,
                        user_id = userId,
                        tag_technologies = tagTechnologies,
                        ndef_message = ndefMessage,
                        timestamp = timestamp
                    )
                )
            }

            return@withTimeout response.body<NfcTagProcessResponse>()
        }
    }

    suspend fun registerAsset(
        assetId: String,
        userId: String,
        assetData: Map<String, String>? = null
    ): Result<String> = runCatching {
        withTimeout(15000) {
            val response = httpClient.post("$baseUrl/register_asset") {
                contentType(ContentType.Application.Json)
                setBody(RegisterAssetRequest(assetId, userId, assetData ?: emptyMap()))
            }

            val result: OrchestrationResponse = response.body()

            if (result.success) {
                return@withTimeout result.message
            } else {
                throw Exception(result.message)
            }
        }
    }

    suspend fun transferAsset(
        assetId: String,
        fromUserId: String,
        toUserId: String
    ): Result<String> = runCatching {
        withTimeout(15000) {
            val ownershipResult = verifyOwnership(assetId, fromUserId)
            val ownershipVerified = ownershipResult.getOrDefault(false)
            
            if (!ownershipVerified) {
                throw Exception("Cannot transfer asset: Current user is not the owner")
            }

            val transferRequest = TransferAssetRequest(assetId, fromUserId, toUserId)
            
            try {
                val response = httpClient.post("$baseUrl/transfer_asset") {
                    contentType(ContentType.Application.Json)
                    setBody(transferRequest)
                }
                
                val result: OrchestrationResponse = response.body()

                if (result.success) {
                    return@withTimeout result.message
                } else {
                    throw Exception("Transfer failed on blockchain: ${result.message}")
                }
            } catch (e: Exception) {
                throw Exception("Exception during blockchain transfer: ${e.message}")
            }
        }
    }


    suspend fun getUserBalance(userId: String): Result<Int> = runCatching {
        withTimeout(10000) {
            val response = httpClient.get("$baseUrl/user_balance/$userId")
            val result: UserBalanceResponse = response.body()
            return@withTimeout result.balance
        }
    }

    suspend fun getUserAssets(userId: String): Result<List<String>> = runCatching {
        withTimeout(10000) {
            val response = httpClient.get("$baseUrl/user_assets/$userId")
            val result: UserAssetsResponse = response.body()
            return@withTimeout result.assets
        }
    }

    suspend fun verifyOwnership(assetId: String, userId: String): Result<Boolean> = runCatching {
        withTimeout(10000) {
            val response = httpClient.get("$baseUrl/verify_ownership") {
                url {
                    parameters.append("asset_id", assetId)
                    parameters.append("user_id", userId)
                }
            }
            val result: OwnershipResponse = response.body()
            return@withTimeout result.is_owner
        }
    }

    suspend fun getAssetHistory(assetId: String): Result<List<OwnershipRecord>> = runCatching {
        retryWithBackoff(maxAttempts = 3) {
            withTimeout(10000) {
                val response = httpClient.get("$baseUrl/asset_history/$assetId")
                val result: AssetHistoryResponse = response.body()
                return@withTimeout result.history
            }
        }
    }
    
    private suspend fun <T> retryWithBackoff(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 500,
        maxDelayMs: Long = 5000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        var attempts = 0
        
        while (true) {
            try {
                attempts++
                return block()
            } catch (e: Exception) {
                if (attempts >= maxAttempts) {
                    throw e
                }
                
                kotlinx.coroutines.delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
            }
        }
    }

    suspend fun getAssetNodeData(assetId: String): Result<Map<String, String>?> = runCatching {
        retryWithBackoff(maxAttempts = 3) {
            withTimeout(10000) {
                val response = httpClient.get("$baseUrl/asset_data/$assetId")
                val result: AssetDataResponse = response.body()
                return@withTimeout result.data
            }
        }
    }

    suspend fun getBlockchainStats(): Result<Map<String, Any>> = runCatching {
        withTimeout(10000) {
            val response = httpClient.get("$baseUrl/blockchain_stats")
            val statsResponse: Map<String, Any> = response.body()
            return@withTimeout statsResponse
        }
    }

    suspend fun verifyIntegrity(): Result<Boolean> = runCatching {
        withTimeout(15000) {
            val response = httpClient.get("$baseUrl/verify_integrity")
            val integrityResponse: Map<String, Any> = response.body()
            val isIntegrityOk = integrityResponse["integrity_ok"] as? Boolean ?: false
            val message = integrityResponse["message"] as? String ?: "Unknown integrity status"

            if (!isIntegrityOk) {
                throw Exception("Blockchain integrity check failed: $message")
            }

            return@withTimeout isIntegrityOk
        }
    }
}

@Serializable
data class NfcTagRequest(
    val tag_id: String,
    val user_id: String,
    val tag_technologies: List<String>,
    val ndef_message: String,
    val timestamp: Long
)

@Serializable
data class NfcTagProcessResponse(
    val success: Boolean,
    val result: String,
    val action: String,
    val asset_id: String
)

@Serializable
data class RegisterAssetRequest(
    val asset_id: String,
    val user_id: String,
    val asset_data: Map<String, String>
)

@Serializable
data class TransferAssetRequest(
    val asset_id: String,
    val from_user_id: String,
    val to_user_id: String
)


@Serializable
data class BlockchainResponse(
    val success: Boolean,
    val result: String
)

@Serializable
data class OrchestrationResponse(
    val success: Boolean,
    val message: String,
    val node_ids: List<String> = emptyList()
)

@Serializable
data class UserBalanceResponse(
    val user_id: String,
    val balance: Int
)

@Serializable
data class UserAssetsResponse(
    val user_id: String,
    val assets: List<String>
)

@Serializable
data class OwnershipResponse(
    val asset_id: String,
    val user_id: String,
    val is_owner: Boolean
)

@Serializable
data class OwnershipRecord(
    val user_id: String,
    val timestamp: Double,
    val node_id: String,
    val action: String
)

@Serializable
data class AssetHistoryResponse(
    val asset_id: String,
    val history: List<OwnershipRecord>
)

@Serializable
data class AssetDataResponse(
    val asset_id: String,
    val data: Map<String, String>? = null
)

