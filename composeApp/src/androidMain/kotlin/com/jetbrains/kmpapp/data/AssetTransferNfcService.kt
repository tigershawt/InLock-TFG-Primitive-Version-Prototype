package com.jetbrains.kmpapp.data

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.jetbrains.kmpapp.MainActivity
import com.jetbrains.kmpapp.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class AssetTransferNfcService : NfcService {
    private var activity: Activity? = null
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    
    private val _tagData = MutableStateFlow<String?>(null)
    override val tagData: StateFlow<String?> = _tagData
    
    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning
    
    private var tagCallback: NfcAdapter.ReaderCallback? = null
    private val tagProcessingLock = AtomicBoolean(false)
    
    fun initialize(activity: Activity) {
        this.activity = activity
        
        try {
            nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
            
            val intent = Intent(activity, activity.javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            
            pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)
        } catch (e: Exception) {
            Logger.e("AssetTransferNfcService", "Error initializing: ${e.localizedMessage}", e)
        }
    }
    
    fun handleIntent(intent: Intent) {
        when (intent.action) {
            NfcAdapter.ACTION_TAG_DISCOVERED,
            NfcAdapter.ACTION_TECH_DISCOVERED,
            NfcAdapter.ACTION_NDEF_DISCOVERED -> {
                intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)?.let { tag ->
                    processTag(tag)
                }
            }
        }
    }
    
    override fun isNfcSupported(): Boolean {
        return nfcAdapter != null
    }
    
    override fun isNfcEnabled(): Boolean {
        return nfcAdapter?.isEnabled == true
    }
    
    override fun startNfcScan(): Result<Unit> {
        return try {
            when {
                !isNfcSupported() -> return Result.failure(Exception("NFC not supported on this device"))
                !isNfcEnabled() -> return Result.failure(Exception("NFC is not enabled"))
            }
            
            
            _tagData.value = null
            _isScanning.value = true
            
            (activity as? MainActivity)?.let { currentActivity ->
                
                setupForegroundDispatch(currentActivity)
                setupReaderMode()
                
                Result.success(Unit)
            } ?: run {
                _isScanning.value = false
                Result.failure(Exception("Activity reference is invalid"))
            }
        } catch (e: Exception) {
            _isScanning.value = false
            Logger.e("AssetTransferNfcService", "Error starting NFC scan: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }
    
    override fun stopNfcScan() {
        _isScanning.value = false
        
        activity?.let { currentActivity ->
            try {
                nfcAdapter?.disableForegroundDispatch(currentActivity)
            } catch (e: Exception) {
                Logger.e("AssetTransferNfcService", "Error disabling foreground dispatch: ${e.localizedMessage}", e)
            }
            
            try {
                nfcAdapter?.disableReaderMode(currentActivity)
            } catch (e: Exception) {
                Logger.e("AssetTransferNfcService", "Error disabling reader mode: ${e.localizedMessage}", e)
            }
        }
    }
    
    override suspend fun readNfcTag(): Result<String> {
        
        _tagData.value?.let { return Result.success(it) }
        
        
        startNfcScan()
        
        
        return Result.failure(Exception("Starting NFC scan, tag data will be delivered via StateFlow"))
    }
    
    private fun setupForegroundDispatch(activity: Activity) {
        try {
            nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, null, null)
        } catch (e: Exception) {
            Logger.e("AssetTransferNfcService", "Error enabling foreground dispatch: ${e.localizedMessage}", e)
        }
    }
    
    private fun setupReaderMode() {
        activity?.let { currentActivity ->
            try {
                
                tagCallback = NfcAdapter.ReaderCallback { tag ->
                    
                    if (tagProcessingLock.compareAndSet(false, true)) {
                        processTag(tag)
                    }
                }
                
                
                val readerModeFlags = NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_NFC_F or
                        NfcAdapter.FLAG_READER_NFC_V or
                        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
                
                nfcAdapter?.enableReaderMode(
                    currentActivity,
                    tagCallback,
                    readerModeFlags,
                    null
                )
            } catch (e: Exception) {
                Logger.e("AssetTransferNfcService", "Error enabling reader mode: ${e.localizedMessage}", e)
            }
        }
    }
    
    private fun processTag(tag: Tag) {
        try {
            
            readTag(tag).let { tagInfo ->
                
                _tagData.value = tagInfo
                
                
                _isScanning.value = false
            }
        } catch (e: Exception) {
            Logger.e("AssetTransferNfcService", "Error processing tag: ${e.localizedMessage}", e)
        } finally {
            
            Handler(Looper.getMainLooper()).postDelayed({
                tagProcessingLock.set(false)
            }, 1000)
        }
    }
    
    private fun readTag(tag: Tag): String {
        val sb = StringBuilder()
        val id = tag.id
        
        sb.apply {
            append("Tag ID (hex): ${bytesToHex(id)}\n")
            append("Tag ID (reversed hex): ${bytesToReversedHex(id)}\n")
            append("Tag ID (dec): ${bytesToDec(id)}\n")
            append("Tag ID (reversed dec): ${bytesToReversedDec(id)}\n")
            append("Technologies: ${tag.techList.joinToString(", ")}\n")
        }
        
        tag.techList.forEach { tech ->
            when (tech) {
                Ndef::class.java.name -> readNdefTechnology(tag, sb)
                MifareClassic::class.java.name -> readMifareClassicTechnology(tag, sb)
                MifareUltralight::class.java.name -> readMifareUltralightTechnology(tag, sb)
            }
        }
        
        return sb.toString()
    }
    
    private fun readNdefTechnology(tag: Tag, sb: StringBuilder) {
        Ndef.get(tag)?.let { ndef ->
            try {
                ndef.connect()
                parseNdefMessage(ndef.cachedNdefMessage, sb)
            } catch (e: Exception) {
                sb.append("Error reading NDEF: ${e.localizedMessage}\n")
            } finally {
                try { ndef.close() } catch (e: Exception) { }
            }
        }
    }
    
    private fun readMifareClassicTechnology(tag: Tag, sb: StringBuilder) {
        MifareClassic.get(tag)?.let { mifareClassic ->
            try {
                mifareClassic.connect()
                sb.apply {
                    append("Mifare Classic type: ${getMifareClassicType(mifareClassic)}\n")
                    append("Mifare size: ${mifareClassic.size} bytes\n")
                    append("Mifare sectors: ${mifareClassic.sectorCount}\n")
                    append("Mifare blocks: ${mifareClassic.blockCount}\n")
                }
                
                
                for (sectorIndex in 0 until minOf(mifareClassic.sectorCount, 3)) {
                    if (mifareClassic.authenticateSectorWithKeyA(sectorIndex, MifareClassic.KEY_DEFAULT)) {
                        sb.append("Sector $sectorIndex authenticated with KEY_DEFAULT\n")
                        val firstBlockIndex = mifareClassic.sectorToBlock(sectorIndex)
                        val lastBlockIndex = firstBlockIndex + mifareClassic.getBlockCountInSector(sectorIndex)
                        
                        for (blockIndex in firstBlockIndex until minOf(lastBlockIndex, firstBlockIndex + 2)) {
                            try {
                                val blockData = mifareClassic.readBlock(blockIndex)
                                sb.append("Block $blockIndex: ${bytesToHex(blockData)}\n")
                            } catch (e: IOException) {
                                sb.append("Error reading block $blockIndex: ${e.localizedMessage}\n")
                            }
                        }
                    } else {
                        sb.append("Sector $sectorIndex: Authorization failed\n")
                    }
                }
            } catch (e: Exception) {
                sb.append("Error reading Mifare Classic: ${e.localizedMessage}\n")
            } finally {
                try { mifareClassic.close() } catch (e: Exception) { }
            }
        }
    }
    
    private fun readMifareUltralightTechnology(tag: Tag, sb: StringBuilder) {
        MifareUltralight.get(tag)?.let { mifareUltralight ->
            try {
                mifareUltralight.connect()
                sb.append("Mifare Ultralight type: ${getMifareUltralightType(mifareUltralight)}\n")
                
                
                for (pageIndex in 0 until minOf(mifareUltralight.maxTransceiveLength / 4, 6)) {
                    try {
                        val pageData = mifareUltralight.readPages(pageIndex)
                        sb.append("Page $pageIndex: ${bytesToHex(pageData)}\n")
                    } catch (e: IOException) {
                        sb.append("Error reading page $pageIndex: ${e.localizedMessage}\n")
                        break
                    } catch (e: TagLostException) {
                        sb.append("Tag was lost while reading page $pageIndex\n")
                        break
                    }
                }
            } catch (e: Exception) {
                sb.append("Error reading Mifare Ultralight: ${e.localizedMessage}\n")
            } finally {
                try { mifareUltralight.close() } catch (e: Exception) { }
            }
        }
    }
    
    private fun parseNdefMessage(ndefMessage: NdefMessage?, sb: StringBuilder) {
        if (ndefMessage == null) {
            sb.append("No NDEF message found\n")
            return
        }
        
        val records = ndefMessage.records
        sb.append("NDEF Message contains ${records.size} records\n")
        
        records.forEachIndexed { i, record ->
            sb.append("NDEF Record ${i + 1}:\n")
            parseNdefRecord(record, sb)
        }
    }
    
    private fun parseNdefRecord(record: NdefRecord, sb: StringBuilder) {
        val tnf = record.tnf
        val type = record.type
        val id = record.id
        val payload = record.payload
        
        sb.apply {
            append("  TNF: $tnf\n")
            append("  Type: ${bytesToHex(type)} (${String(type)})\n")
            
            if (id.isNotEmpty()) {
                append("  ID: ${bytesToHex(id)} (${String(id)})\n")
            } else {
                append("  ID: (Empty)\n")
            }
            
            if (payload.isNotEmpty()) {
                append("  Payload (hex): ${bytesToHex(payload)}\n")
                
                when {
                    tnf == NdefRecord.TNF_WELL_KNOWN && type.contentEquals(NdefRecord.RTD_TEXT) -> {
                        
                        append("  Payload (Text): ${parseTextRecord(payload)}\n")
                    }
                    tnf == NdefRecord.TNF_WELL_KNOWN && type.contentEquals(NdefRecord.RTD_URI) -> {
                        
                        append("  Payload (URI): ${parseUriRecord(payload)}\n")
                    }
                    else -> {
                        
                        try {
                            val text = String(payload)
                            if (text.all { it.code in 32..126 }) {
                                append("  Payload (as Text): $text\n")
                            }
                        } catch (e: Exception) {
                            
                        }
                    }
                }
            } else {
                append("  Payload: (Empty)\n")
            }
        }
    }
    
    private fun parseTextRecord(payload: ByteArray): String {
        return try {
            val statusByte = payload[0].toInt()
            val languageCodeLength = statusByte and 0x3F
            val textEncoding = if (statusByte and 0x80 != 0) "UTF-16" else "UTF-8"
            
            
            
            
            
            String(
                payload,
                1 + languageCodeLength,
                payload.size - 1 - languageCodeLength,
                charset(textEncoding)
            )
        } catch (e: Exception) {
            Logger.e("AssetTransferNfcService", "Error parsing NDEF text record: ${e.localizedMessage}", e)
            "ERROR: Could not parse text record"
        }
    }
    
    private fun parseUriRecord(payload: ByteArray): String {
        return try {
            
            val uriPrefixes = arrayOf(
                "", "http://www.", "https://www.", "http://", "https://", "tel:", "mailto:",
                "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://", "sftp://", "smb://",
                "nfs://", "ftp://", "dav://", "news:", "telnet://", "imap:", "rtsp://", "urn:",
                "pop:", "sip:", "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://", "tcpobex://",
                "irdaobex://", "file://", "urn:epc:id:", "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:",
                "urn:epc:", "urn:nfc:"
            )
            
            val prefixCode = payload[0].toInt() and 0xFF
            val prefix = if (prefixCode < uriPrefixes.size) uriPrefixes[prefixCode] else ""
            val uri = String(payload, 1, payload.size - 1, Charsets.UTF_8)
            
            prefix + uri
        } catch (e: Exception) {
            Logger.e("AssetTransferNfcService", "Error parsing NDEF URI record: ${e.localizedMessage}", e)
            "ERROR: Could not parse URI record"
        }
    }
    
    private fun getMifareClassicType(mifareClassic: MifareClassic): String = when (mifareClassic.type) {
        MifareClassic.TYPE_CLASSIC -> "Classic"
        MifareClassic.TYPE_PLUS -> "Plus"
        MifareClassic.TYPE_PRO -> "Pro"
        else -> "Unknown"
    }
    
    private fun getMifareUltralightType(mifareUltralight: MifareUltralight): String = when (mifareUltralight.type) {
        MifareUltralight.TYPE_ULTRALIGHT -> "Ultralight"
        MifareUltralight.TYPE_ULTRALIGHT_C -> "Ultralight C"
        else -> "Unknown"
    }
    
    
    private fun bytesToHex(bytes: ByteArray): String = buildString {
        bytes.forEach { byte ->
            append(String.format("%02X", byte.toInt() and 0xFF))
        }
    }
    
    private fun bytesToReversedHex(bytes: ByteArray): String = buildString {
        bytes.reversed().forEach { byte ->
            append(String.format("%02X", byte.toInt() and 0xFF))
        }
    }
    
    private fun bytesToDec(bytes: ByteArray): Long {
        return bytes.fold(0L) { result, byte ->
            (result shl 8) or (byte.toInt() and 0xFF).toLong()
        }
    }
    
    private fun bytesToReversedDec(bytes: ByteArray): Long {
        return bytes.reversed().fold(0L) { result, byte ->
            (result shl 8) or (byte.toInt() and 0xFF).toLong()
        }
    }
    
    fun cleanup() {
        try {
            
            stopNfcScan()
            
            
            _isScanning.value = false
            _tagData.value = null
            tagProcessingLock.set(false)
            
            
            activity?.let { currentActivity ->
                try {
                    tagCallback?.let { _ -> nfcAdapter?.disableReaderMode(currentActivity) }
                } catch (e: Exception) {
                    Logger.e("AssetTransferNfcService", "Error disabling reader mode during cleanup: ${e.localizedMessage}", e)
                }
            }
            
            
            tagCallback = null
            activity = null
        } catch (e: Exception) {
            Logger.e("AssetTransferNfcService", "Error during NFC service cleanup: ${e.localizedMessage}", e)
            
            tagCallback = null
            activity = null
        }
    }
}