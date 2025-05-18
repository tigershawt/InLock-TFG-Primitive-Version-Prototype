package com.jetbrains.kmpapp.data

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual fun createNfcService(): NfcService = NfcServiceImpl()

class NfcServiceImpl : NfcService {
    private var nfcAdapter: NfcAdapter? = null
    private val _tagData = MutableStateFlow<String?>(null)
    override val tagData: StateFlow<String?> = _tagData.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var currentActivity: Activity? = null
    private var tagCallback: NfcAdapter.ReaderCallback? = null
    private val tagProcessingLock = AtomicBoolean(false)

    fun initialize(activity: Activity) {
        currentActivity = activity
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
    }

    override fun isNfcSupported(): Boolean {
        if (currentActivity == null) return false
        if (nfcAdapter == null) {
            nfcAdapter = NfcAdapter.getDefaultAdapter(currentActivity)
        }
        return nfcAdapter != null
    }

    override fun isNfcEnabled(): Boolean {
        return nfcAdapter?.isEnabled == true
    }

    override fun startNfcScan(): Result<Unit> = runCatching {
        if (currentActivity == null) {
            throw IllegalStateException("Activity not initialized. Call initialize(activity) first.")
        }

        if (!isNfcSupported()) {
            throw IllegalStateException("NFC is not supported on this device")
        }

        if (!isNfcEnabled()) {
            throw IllegalStateException("NFC is not enabled")
        }

        _tagData.value = null
        tagProcessingLock.set(false)

        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V

        tagCallback = NfcAdapter.ReaderCallback { tag ->
            if (tagProcessingLock.compareAndSet(false, true)) {
                try {
                    val tagInfo = readTag(tag)
                    _tagData.value = tagInfo
                    _isScanning.value = false
                } catch (e: Exception) {
                } finally {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        tagProcessingLock.set(false)
                    }, 1000)
                }
            }
        }

        nfcAdapter?.enableReaderMode(
            currentActivity,
            tagCallback,
            flags,
            null
        )

        _isScanning.value = true
    }

    override fun stopNfcScan() {
        try {
            currentActivity?.let { activity ->
                nfcAdapter?.disableReaderMode(activity)
            }
        } catch (e: Exception) {
        } finally {
            _isScanning.value = false
            _tagData.value = null
            tagCallback = null
            tagProcessingLock.set(false)
        }
    }

    fun processIntent(intent: Intent): String? {
        if (intent.action !in listOf(
                NfcAdapter.ACTION_TECH_DISCOVERED,
                NfcAdapter.ACTION_TAG_DISCOVERED,
                NfcAdapter.ACTION_NDEF_DISCOVERED)) {
            return null
        }
        
        try {
            val tag = if (android.os.Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            } ?: return null
            
            _isScanning.value = true
            val tagInfo = readTag(tag)
            _tagData.value = tagInfo
            
            return tagInfo
        } catch (e: Exception) {
            return null
        } finally {
            _isScanning.value = false
            tagProcessingLock.set(false)
        }
    }

    private fun readTag(tag: Tag): String {
        val sb = StringBuilder()

        val tagId = bytesToHex(tag.id)
        sb.append("Tag ID (hex): $tagId\n")
        
        val technologies = tag.techList
        sb.append("Tag Technologies:\n")
        technologies.forEach { tech ->
            sb.append("- $tech\n")
        }

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                val ndefMessage = ndef.ndefMessage
                
                if (ndefMessage != null) {
                    sb.append("NDEF Message:\n")
                    ndefMessage.records.forEachIndexed { index, record ->
                        val payload = record.payload
                        try {
                            if (payload.isNotEmpty()) {
                                if (record.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN && 
                                    record.type.contentEquals(android.nfc.NdefRecord.RTD_TEXT)) {
                                    val textEncoding = if (payload[0].toInt() and 128 == 0) "UTF-8" else "UTF-16"
                                    val languageCodeLength = payload[0].toInt() and 0x3F

                                    if (payload.size > languageCodeLength + 1) {
                                        val text = String(
                                            payload,
                                            languageCodeLength + 1,
                                            payload.size - languageCodeLength - 1,
                                            charset(textEncoding)
                                        )
                                        sb.append("Record $index: $text\n")
                                    } else {
                                        sb.append("Record $index: ${bytesToHex(payload)}\n")
                                    }
                                } else if (record.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN && 
                                        record.type.contentEquals(android.nfc.NdefRecord.RTD_URI)) {
                                    val prefixCode = payload[0].toInt()
                                    val prefix = when (prefixCode) {
                                        0x00 -> ""
                                        0x01 -> "http://www."
                                        0x02 -> "https://www."
                                        0x03 -> "http://"
                                        0x04 -> "https://"
                                        0x05 -> "tel:"
                                        0x06 -> "mailto:"
                                        0x07 -> "ftp://anonymous:anonymous@"
                                        0x08 -> "ftp://ftp."
                                        0x09 -> "ftps://"
                                        0x0A -> "sftp://"
                                        0x0B -> "smb://"
                                        0x0C -> "nfs://"
                                        0x0D -> "ftp://"
                                        0x0E -> "dav://"
                                        0x0F -> "news:"
                                        0x10 -> "telnet://"
                                        0x11 -> "imap:"
                                        0x12 -> "rtsp://"
                                        0x13 -> "urn:"
                                        0x14 -> "pop:"
                                        0x15 -> "sip:"
                                        0x16 -> "sips:"
                                        0x17 -> "tftp:"
                                        0x18 -> "btspp://"
                                        0x19 -> "btl2cap://"
                                        0x1A -> "btgoep://"
                                        0x1B -> "tcpobex://"
                                        0x1C -> "irdaobex://"
                                        0x1D -> "file://"
                                        0x1E -> "urn:epc:id:"
                                        0x1F -> "urn:epc:tag:"
                                        0x20 -> "urn:epc:pat:"
                                        0x21 -> "urn:epc:raw:"
                                        0x22 -> "urn:epc:"
                                        0x23 -> "urn:nfc:"
                                        else -> ""
                                    }
                                    val uri = prefix + String(payload, 1, payload.size - 1, Charsets.UTF_8)
                                    sb.append("Record $index: $uri\n")
                                } else {
                                    sb.append("Record $index: ${bytesToHex(payload)}\n")
                                }
                            } else {
                                sb.append("Record $index: Empty\n")
                            }
                        } catch (e: Exception) {
                            sb.append("Record $index: ${bytesToHex(payload)} (Error parsing: ${e.message})\n")
                        }
                    }
                } else {
                    sb.append("No NDEF message found\n")
                }
                ndef.close()
            } catch (e: Exception) {
                sb.append("Error reading NDEF: ${e.message}\n")
            }
        }

        val mifareClassic = MifareClassic.get(tag)
        if (mifareClassic != null) {
            try {
                mifareClassic.connect()
                val type = getMifareClassicType(mifareClassic.type)
                sb.append("Mifare Classic detected\n")
                sb.append("Size: ${mifareClassic.size} bytes\n")
                sb.append("Type: $type\n")
                
                try {
                    if (mifareClassic.authenticateSectorWithKeyA(0, MifareClassic.KEY_DEFAULT)) {
                        val block = mifareClassic.readBlock(0)
                        sb.append("Block 0: ${bytesToHex(block)}\n")
                    }
                } catch (e: Exception) {
                }
                
                mifareClassic.close()
            } catch (e: Exception) {
                sb.append("Error reading Mifare Classic: ${e.message}\n")
            }
        }

        val mifareUltralight = MifareUltralight.get(tag)
        if (mifareUltralight != null) {
            try {
                mifareUltralight.connect()
                val type = getMifareUltralightType(mifareUltralight.type)
                sb.append("Mifare Ultralight detected\n")
                sb.append("Type: $type\n")
                
                try {
                    val page = mifareUltralight.readPages(0)
                    sb.append("Page 0: ${bytesToHex(page)}\n")
                } catch (e: Exception) {
                }
                
                mifareUltralight.close()
            } catch (e: Exception) {
                sb.append("Error reading Mifare Ultralight: ${e.message}\n")
            }
        }

        return sb.toString()
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = "0123456789ABCDEF"[v shr 4]
            hexChars[i * 2 + 1] = "0123456789ABCDEF"[v and 0x0F]
        }
        return String(hexChars)
    }

    private fun getMifareClassicType(type: Int): String {
        return when (type) {
            MifareClassic.TYPE_CLASSIC -> "Classic"
            MifareClassic.TYPE_PLUS -> "Plus"
            MifareClassic.TYPE_PRO -> "Pro"
            else -> "Unknown"
        }
    }

    private fun getMifareUltralightType(type: Int): String {
        return when (type) {
            MifareUltralight.TYPE_ULTRALIGHT -> "Ultralight"
            MifareUltralight.TYPE_ULTRALIGHT_C -> "Ultralight C"
            else -> "Unknown"
        }
    }

    override suspend fun readNfcTag(): Result<String> = runCatching {
        _tagData.value?.let { return@runCatching it }

        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { stopNfcScan() }

            if (!_isScanning.value) {
                startNfcScan().onFailure { continuation.resumeWithException(it) }
            }

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!continuation.isCompleted) {
                    _tagData.value?.let { continuation.resume(it) }
                        ?: continuation.resumeWithException(IllegalStateException("Timeout waiting for NFC tag"))
                }
            }, 15000)
        }
    }
}