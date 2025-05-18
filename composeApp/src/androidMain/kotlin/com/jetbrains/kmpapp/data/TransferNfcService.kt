package com.jetbrains.kmpapp.data

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TransferNfcServiceAndroid(private val context: Context) : TransferNfcService {
    private var nfcAdapter: NfcAdapter? = null
    private var activity: Activity? = null
    private var tagCallback: NfcAdapter.ReaderCallback? = null
    private val tagProcessingLock = java.util.concurrent.atomic.AtomicBoolean(false)
    
    private val _tagData = MutableStateFlow<String?>(null)
    override val tagData: StateFlow<String?> = _tagData
    
    private val _isTransferScanActive = MutableStateFlow(false)
    override val isTransferScanActive: StateFlow<Boolean> = _isTransferScanActive
    
    init {
        val nfcManager = context.getSystemService(Context.NFC_SERVICE) as NfcManager
        nfcAdapter = nfcManager.defaultAdapter
    }
    
    override fun isNfcSupported(): Boolean {
        return nfcAdapter != null
    }
    
    override fun isNfcEnabled(): Boolean {
        return nfcAdapter?.isEnabled == true
    }
    
    fun setTransferActivity(newActivity: Activity) {
        activity?.let { releaseActivity() }
        activity = newActivity
        nfcAdapter = NfcAdapter.getDefaultAdapter(newActivity)
    }
    
    fun releaseActivity() {
        stopTransferScan()
        activity = null
    }
    
    override fun startTransferScan(): Result<Unit> {
        return runCatching {
            if (!isNfcSupported()) {
                throw Exception("NFC is not supported on this device")
            }

            if (!isNfcEnabled()) {
                throw Exception("NFC is not enabled")
            }

            val currentActivity = activity ?: throw Exception("Activity not set for NFC scan")

            stopTransferScan()
            _tagData.value = null
            tagProcessingLock.set(false)

            tagCallback = NfcAdapter.ReaderCallback { tag ->
                if (tagProcessingLock.compareAndSet(false, true)) {
                    try {
                        val tagInfo = processTransferTag(tag)
                        _tagData.value = tagInfo
                    } catch (e: Exception) {
                    } finally {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            tagProcessingLock.set(false)
                        }, 1000)
                    }
                }
            }

            val flags = NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

            nfcAdapter?.enableReaderMode(
                currentActivity,
                tagCallback,
                flags,
                null
            )

            _isTransferScanActive.value = true
        }
    }
    
    override fun stopTransferScan() {
        try {
            activity?.let { currentActivity ->
                try {
                    nfcAdapter?.disableReaderMode(currentActivity)
                } catch (e: Exception) {
                }
            }

            tagCallback = null
            tagProcessingLock.set(false)
            _isTransferScanActive.value = false
            _tagData.value = null

        } catch (e: Exception) {
        }
    }
    
    fun processTransferTag(tag: Tag): String {
        val stringBuilder = StringBuilder()

        try {
            val tagId = bytesToHexString(tag.id)
            stringBuilder.append("Tag ID (hex): $tagId\n\n")

            val techList = tag.techList
            stringBuilder.append("Technologies: ${techList.joinToString(", ")}\n\n")

            for (tech in techList) {
                when (tech) {
                    Ndef::class.java.name -> {
                        val ndefTag = Ndef.get(tag)
                        try {
                            val maxSize = ndefTag.maxSize
                            val isWritable = ndefTag.isWritable
                            val ndefType = ndefTag.type
                            stringBuilder.append("NDEF Support:\n")
                            stringBuilder.append("  Max Size: $maxSize bytes\n")
                            stringBuilder.append("  Is Writable: $isWritable\n")
                            stringBuilder.append("  Type: $ndefType\n\n")
                        } catch (e: Exception) {
                            stringBuilder.append("Error reading NDEF: ${e.message}\n")
                        } finally {
                            try { ndefTag.close() } catch (e: Exception) {}
                        }
                    }
                    MifareClassic::class.java.name -> {
                        val mifareTag = MifareClassic.get(tag)
                        try {
                            val type = when (mifareTag.type) {
                                MifareClassic.TYPE_CLASSIC -> "Classic"
                                MifareClassic.TYPE_PLUS -> "Plus"
                                MifareClassic.TYPE_PRO -> "Pro"
                                else -> "Unknown"
                            }
                            val size = mifareTag.size
                            stringBuilder.append("Mifare Classic:\n")
                            stringBuilder.append("  Type: $type\n")
                            stringBuilder.append("  Size: $size bytes\n\n")
                        } catch (e: Exception) {
                            stringBuilder.append("Error reading Mifare Classic: ${e.message}\n")
                        } finally {
                            try { mifareTag.close() } catch (e: Exception) {}
                        }
                    }
                    MifareUltralight::class.java.name -> {
                        val mifareUlTag = MifareUltralight.get(tag)
                        try {
                            val type = when (mifareUlTag.type) {
                                MifareUltralight.TYPE_ULTRALIGHT -> "Ultralight"
                                MifareUltralight.TYPE_ULTRALIGHT_C -> "Ultralight C"
                                else -> "Unknown"
                            }
                            stringBuilder.append("Mifare Ultralight:\n")
                            stringBuilder.append("  Type: $type\n\n")
                        } catch (e: Exception) {
                            stringBuilder.append("Error reading Mifare Ultralight: ${e.message}\n")
                        } finally {
                            try { mifareUlTag.close() } catch (e: Exception) {}
                        }
                    }
                }
            }

            if (_isTransferScanActive.value) {
                _tagData.value = stringBuilder.toString()
            }

            return stringBuilder.toString()
        } catch (e: Exception) {
            return "Error processing tag: ${e.message}"
        }
    }
    
    fun clearTagData() {
        _tagData.value = null
    }
    
    private fun bytesToHexString(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            val hex = Integer.toHexString(0xFF and b.toInt())
            if (hex.length == 1) {
                sb.append('0')
            }
            sb.append(hex.uppercase())
        }
        return sb.toString()
    }
    
    override fun extractRfidIdFromTagData(tagData: String): String {
        return "Tag ID \\(hex\\): ([A-F0-9]+)".toRegex()
            .find(tagData)?.groupValues?.get(1) ?: ""
    }
}