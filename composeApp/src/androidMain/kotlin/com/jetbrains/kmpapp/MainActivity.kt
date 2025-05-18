package com.jetbrains.kmpapp

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.jetbrains.kmpapp.data.AssetTransferNfcService
import com.jetbrains.kmpapp.data.NfcServiceImpl
import com.jetbrains.kmpapp.data.TransferNfcService
import com.jetbrains.kmpapp.data.setTransferActivity
import com.jetbrains.kmpapp.data.releaseActivity
import com.jetbrains.kmpapp.data.processTransferTag
import com.jetbrains.kmpapp.screens.qrcode.MainActivityHolder
import com.jetbrains.kmpapp.screens.qrcode.TransferActivityHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

class MainActivity : ComponentActivity() {
    companion object {
        var INSTANCE: MainActivity? = null
            private set
            
        var appContext: android.content.Context? = null
            private set
        private const val TAG = "MainActivity"
    }

    private val nfcService by inject<NfcServiceImpl>()
    private val assetTransferNfcService by inject<AssetTransferNfcService>(named("assetTransferNfc"))
    private val transferNfcService by inject<TransferNfcService>()
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var nfcIntentFilters: Array<IntentFilter>? = null
    private var techLists: Array<Array<String>>? = null

    fun getTransferNfcServiceForExternal(): TransferNfcService = transferNfcService
    fun restartTransferNfcScan() {
        try {
            transferNfcService.stopTransferScan()
            transferNfcService.startTransferScan()
        } catch (e: Exception) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        INSTANCE = this
        
        appContext = applicationContext

        MainActivityHolder.setActivity(this)

        TransferActivityHolder.setActivity(this, transferNfcService)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter != null) {
            checkNfcPermission()
        }

        nfcService.initialize(this)

        assetTransferNfcService.initialize(this)

        transferNfcService.setTransferActivity(this)



        setupNfcIntentHandling()

        handleIntent(intent)

        lifecycleScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                setContent {
                    App()
                }
            }
        }
    }
    
    private fun checkNfcPermission() {
        
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasNfcPermission = checkSelfPermission(android.Manifest.permission.NFC) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (!hasNfcPermission) {
                requestPermissions(arrayOf(android.Manifest.permission.NFC), 1001)
            }
        }
    }

    private fun setupNfcIntentHandling() {
        try {

            val intent = Intent(this, javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)


            val ndefDiscovered = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                try {
                    addDataType("*/*")
                } catch (e: IntentFilter.MalformedMimeTypeException) {
                }
            }

            val techDiscovered = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
            val tagDiscovered = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)

            tagDiscovered.priority = IntentFilter.SYSTEM_HIGH_PRIORITY

            nfcIntentFilters = arrayOf(ndefDiscovered, techDiscovered, tagDiscovered)

            techLists = arrayOf(
                arrayOf(Ndef::class.java.name),
                arrayOf(NdefFormatable::class.java.name),
                arrayOf(MifareClassic::class.java.name),
                arrayOf(MifareUltralight::class.java.name),
                arrayOf(NfcA::class.java.name),
                arrayOf(NfcB::class.java.name),
                arrayOf(NfcF::class.java.name),
                arrayOf(NfcV::class.java.name),
                arrayOf(IsoDep::class.java.name),
                arrayOf(NfcA::class.java.name, Ndef::class.java.name),
                arrayOf(NfcA::class.java.name, MifareClassic::class.java.name),
                arrayOf(NfcA::class.java.name, MifareUltralight::class.java.name)
            )

        } catch (e: Exception) {
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        MainActivityHolder.handlePermissionResult(requestCode, grantResults)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        handleIntent(intent)
    }

    fun refreshNfcState() {
        val nfcEnabled = nfcAdapter?.isEnabled == true
        if (nfcEnabled && transferNfcService.isTransferScanActive.value) {
            transferNfcService.stopTransferScan()
            transferNfcService.startTransferScan()
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == null) {
            return
        }

        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {


            val tag: Tag? = if (android.os.Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }

            if (tag == null) {
                return
            }

            try {
                val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(300, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(300)
                }
            } catch (e: Exception) {
            }

            val tagId = bytesToHex(tag.id)

            val isTransferActive = transferNfcService.isTransferScanActive.value
            if (isTransferActive) {

                try {
                    val serviceClass = transferNfcService.javaClass

                    try {
                        val scanActiveField = serviceClass.getDeclaredField("_isTransferScanActive")
                        scanActiveField.isAccessible = true
                        val activeFlow = scanActiveField.get(transferNfcService) as MutableStateFlow<Boolean>
                        if (!activeFlow.value) {
                            activeFlow.value = true
                        }
                    } catch (e: Exception) {
                    }

                    val processMethod = serviceClass.getDeclaredMethod("processTransferTag", Tag::class.java)
                    processMethod.isAccessible = true
                    val tagData = processMethod.invoke(transferNfcService, tag) as String

                    val tagDataField = serviceClass.getDeclaredField("_tagData")
                    tagDataField.isAccessible = true
                    val tagFlow = tagDataField.get(transferNfcService) as MutableStateFlow<String?>

                    tagFlow.value = null
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        tagFlow.value = tagData
                    }, 50)

                    val tagIntent = android.content.Intent("com.jetbrains.kmpapp.TAG_DETECTED")
                    tagIntent.putExtra("tag_id", tagId)
                    tagIntent.putExtra("tag_data", tagData)
                    sendBroadcast(tagIntent)

                    return
                } catch (e: Exception) {
                }

                try {
                    val tagData = transferNfcService.processTransferTag(tag)
                    return
                } catch (e: Exception) {
                }

                try {
                    val method = transferNfcService.javaClass.getMethod("processIntent", Intent::class.java)
                    val result = method.invoke(transferNfcService, intent) as Boolean
                    if (result) {
                        return
                    }
                } catch (e: Exception) {
                }

                try {
                    val result = TransferActivityHolder.processTransferIntent(intent)
                    if (result) {
                        return
                    }
                } catch (e: Exception) {
                }
            }

            if (!isTransferActive) {

                try {
                    nfcService.processIntent(intent)
                } catch (e: Exception) {
                }

                try {
                    assetTransferNfcService.handleIntent(intent)
                } catch (e: Exception) {
                }
            }
        }
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

    override fun onResume() {
        super.onResume()

        MainActivityHolder.setActivity(this)
        TransferActivityHolder.setActivity(this, transferNfcService)

        transferNfcService.setTransferActivity(this)

        try {
            if (nfcAdapter?.isEnabled == true) {
                
                if (transferNfcService.isTransferScanActive.value) {
                    TransferActivityHolder.enableTransferForegroundDispatch()
                } else {
                    nfcAdapter?.enableForegroundDispatch(
                        this,
                        pendingIntent,
                        nfcIntentFilters,
                        techLists
                    )
                }
            }
        } catch (e: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()

        try {
            if (nfcAdapter?.isEnabled == true) {
                nfcAdapter?.disableForegroundDispatch(this)
            }

            TransferActivityHolder.disableForegroundDispatch()

            try {
                nfcService.stopNfcScan()
            } catch (e: Exception) {
            }

            try {
                assetTransferNfcService.stopNfcScan()
            } catch (e: Exception) {
            }

            try {
                transferNfcService.stopTransferScan()
            } catch (e: Exception) {
            }
        } catch (e: Exception) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            nfcService.stopNfcScan()
            transferNfcService.stopTransferScan()

            TransferActivityHolder.clearActivity(this)

            if (assetTransferNfcService is AssetTransferNfcService) {
                assetTransferNfcService.cleanup()
            }

            transferNfcService.releaseActivity()

            if (INSTANCE === this) {
                INSTANCE = null
            }
        } catch (e: Exception) {
        }
    }
}