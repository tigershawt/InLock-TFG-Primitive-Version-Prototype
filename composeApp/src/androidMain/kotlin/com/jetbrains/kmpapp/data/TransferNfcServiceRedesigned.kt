package com.jetbrains.kmpapp.data

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

class TransferNfcServiceRedesigned(private val context: Context) : TransferNfcService {
    
    private var nfcAdapter: NfcAdapter? = null
    private var activity: Activity? = null
    private var pendingIntent: PendingIntent? = null
    private var intentFilters: Array<IntentFilter>? = null
    private var techLists: Array<Array<String>>? = null
    private val tagProcessingLock = AtomicBoolean(false)

    private val _tagData = MutableStateFlow<String?>(null)
    override val tagData: StateFlow<String?> = _tagData

    private val _isTransferScanActive = MutableStateFlow(false)
    override val isTransferScanActive: StateFlow<Boolean> = _isTransferScanActive

    private var readerModeCallback: NfcAdapter.ReaderCallback? = null
    
    init {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)

        setupIntentFilters()
    }
    
    private fun setupIntentFilters() {
        try {
            val ndefFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                try {
                    addDataType("*/*")
                } catch (e: IntentFilter.MalformedMimeTypeException) {
                }
            }
            
            val techFilter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
            val tagFilter = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
            
            intentFilters = arrayOf(ndefFilter, techFilter, tagFilter)

            techLists = arrayOf(
                arrayOf(Ndef::class.java.name),
                arrayOf(NdefFormatable::class.java.name),
                arrayOf(MifareClassic::class.java.name),
                arrayOf(MifareUltralight::class.java.name),
                arrayOf(NfcA::class.java.name),
                arrayOf(NfcB::class.java.name),
                arrayOf(NfcF::class.java.name),
                arrayOf(NfcV::class.java.name)
            )
        } catch (e: Exception) {
        }
    }

    fun setTransferActivity(newActivity: Activity) {
        if (activity != null && activity !== newActivity) {
            releaseActivity()
        }
        
        activity = newActivity

        nfcAdapter = NfcAdapter.getDefaultAdapter(newActivity)

        val intent = Intent(newActivity, newActivity.javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        pendingIntent = PendingIntent.getActivity(newActivity, 0, intent, flags)
    }

    fun releaseActivity() {
        stopTransferScan()

        activity = null
        pendingIntent = null
    }
    
    override fun isNfcSupported(): Boolean {
        if (nfcAdapter == null) {
            context.getSystemService(Context.NFC_SERVICE)?.let {
                nfcAdapter = it as? NfcAdapter
            }
        }
        return nfcAdapter != null
    }
    
    override fun isNfcEnabled(): Boolean {
        return nfcAdapter?.isEnabled == true
    }
    
    override fun startTransferScan(): Result<Unit> {
        return runCatching {
            if (!isNfcSupported()) {
                throw Exception("NFC is not supported on this device")
            }

            if (!isNfcEnabled()) {
                throw Exception("NFC is not enabled")
            }

            val currentActivity = activity ?: run {
                throw Exception("Activity not set for NFC scan")
            }

            stopTransferScan()
            tagProcessingLock.set(false)
            _tagData.value = null

            readerModeCallback = NfcAdapter.ReaderCallback { tag ->
                Thread {
                    try {
                        processTagInternal(tag)
                    } catch (e: Exception) {
                    }
                }.start()
            }

            setupReaderMode()
            setupForegroundDispatch()

            try {
                val adapter = nfcAdapter
                if (adapter != null) {
                    try {
                        val method = adapter.javaClass.getDeclaredMethod("resume")
                        method.isAccessible = true
                        method.invoke(adapter)
                    } catch (e: Exception) {
                    }
                }
            } catch (e: Exception) {
            }

            _isTransferScanActive.value = true
        }
    }
    
    private fun setupReaderMode() {
        activity?.let { currentActivity ->
            try {
                val flags = NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_NFC_F or
                        NfcAdapter.FLAG_READER_NFC_V or
                        NfcAdapter.FLAG_READER_NFC_BARCODE or
                        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

                val options = Bundle().apply {
                    putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)

                    try {
                        putBoolean("low_power", false)
                    } catch (e: Exception) {
                    }

                    try {
                        putBoolean("extended_reader_mode", true)
                    } catch (e: Exception) {
                    }
                }

                try {
                    nfcAdapter?.enableReaderMode(
                        currentActivity,
                        readerModeCallback,
                        flags,
                        options
                    )
                } catch (e: Exception) {
                    try {
                        nfcAdapter?.enableReaderMode(
                            currentActivity,
                            readerModeCallback,
                            flags,
                            null
                        )
                    } catch (e: Exception) {
                        try {
                            nfcAdapter?.enableReaderMode(
                                currentActivity,
                                readerModeCallback,
                                NfcAdapter.FLAG_READER_NFC_A,
                                null
                            )
                        } catch (e: Exception) {
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }
    
    private fun setupForegroundDispatch() {
        activity?.let { currentActivity ->
            try {
                nfcAdapter?.enableForegroundDispatch(
                    currentActivity,
                    pendingIntent,
                    intentFilters,
                    techLists
                )
            } catch (e: Exception) {
            }
        }
    }
    
    override fun stopTransferScan() {
        try {
            activity?.let { currentActivity ->
                try {
                    nfcAdapter?.disableReaderMode(currentActivity)
                } catch (e: Exception) {
                }
                
                try {
                    nfcAdapter?.disableForegroundDispatch(currentActivity)
                } catch (e: Exception) {
                }
            }
            
            readerModeCallback = null
            tagProcessingLock.set(false)
            
            _isTransferScanActive.value = false
        } catch (e: Exception) {
        }
    }
    
    fun processIntent(intent: Intent): Boolean {
        if (intent.action in listOf(
                NfcAdapter.ACTION_NDEF_DISCOVERED,
                NfcAdapter.ACTION_TECH_DISCOVERED,
                NfcAdapter.ACTION_TAG_DISCOVERED
            )) {
            
            if (!_isTransferScanActive.value) {
                return false
            }
            
            val tag = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            
            if (tag != null) {
                processTagInternal(tag)
                return true
            }
        }
        
        return false
    }
    
    private fun processTagInternal(tag: Tag) {
        try {
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(250, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(250)
                }
            } catch (e: Exception) {
            }

            val tagId = bytesToHexString(tag.id)

            _isTransferScanActive.value = true

            val tagInfo = processTransferTag(tag)

            _tagData.value = null

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                _tagData.value = tagInfo

                try {
                    val tagIntent = android.content.Intent("com.jetbrains.kmpapp.TAG_DETECTED")
                    tagIntent.putExtra("tag_id", tagId)
                    tagIntent.putExtra("tag_data", tagInfo)
                    context.sendBroadcast(tagIntent)
                } catch (e: Exception) {
                }
            }
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
                            if (!ndefTag.isConnected) {
                                ndefTag.connect()
                            }
                            
                            val maxSize = ndefTag.maxSize
                            val isWritable = ndefTag.isWritable
                            val ndefType = ndefTag.type
                            
                            stringBuilder.append("NDEF Support:\n")
                            stringBuilder.append("  Max Size: $maxSize bytes\n")
                            stringBuilder.append("  Is Writable: $isWritable\n")
                            stringBuilder.append("  Type: $ndefType\n\n")
                            
                            val ndefMessage = ndefTag.ndefMessage
                            if (ndefMessage != null) {
                                stringBuilder.append("NDEF Message:\n")
                                ndefMessage.records.forEachIndexed { index, record ->
                                    val payload = record.payload
                                    stringBuilder.append("  Record $index: ${bytesToHexString(payload)}\n")
                                }
                            } else {
                                stringBuilder.append("No NDEF message\n")
                            }
                            
                            ndefTag.close()
                        } catch (e: Exception) {
                            stringBuilder.append("Error reading NDEF: ${e.message}\n")
                        }
                    }
                    MifareClassic::class.java.name -> {
                        val mifareTag = MifareClassic.get(tag)
                        try {
                            if (!mifareTag.isConnected) {
                                mifareTag.connect()
                            }
                            
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
                            
                            mifareTag.close()
                        } catch (e: Exception) {
                            stringBuilder.append("Error reading Mifare Classic: ${e.message}\n")
                        }
                    }
                    MifareUltralight::class.java.name -> {
                        val mifareUlTag = MifareUltralight.get(tag)
                        try {
                            if (!mifareUlTag.isConnected) {
                                mifareUlTag.connect()
                            }
                            
                            val type = when (mifareUlTag.type) {
                                MifareUltralight.TYPE_ULTRALIGHT -> "Ultralight"
                                MifareUltralight.TYPE_ULTRALIGHT_C -> "Ultralight C"
                                else -> "Unknown"
                            }
                            
                            stringBuilder.append("Mifare Ultralight:\n")
                            stringBuilder.append("  Type: $type\n\n")
                            
                            mifareUlTag.close()
                        } catch (e: Exception) {
                            stringBuilder.append("Error reading Mifare Ultralight: ${e.message}\n")
                        }
                    }
                }
            }
            
            return stringBuilder.toString()
        } catch (e: Exception) {
            return "Error processing tag: ${e.message}"
        }
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
        val tagIdMatch = "Tag ID \\(hex\\): ([A-F0-9]+)".toRegex().find(tagData)
        val tagId = tagIdMatch?.groupValues?.get(1)

        if (!tagId.isNullOrEmpty()) {
            return tagId
        }

        val fallbackMatch = "[A-F0-9]{8,}".toRegex().find(tagData)
        val fallbackId = fallbackMatch?.value

        if (!fallbackId.isNullOrEmpty()) {
            return fallbackId
        }

        return ""
    }
}