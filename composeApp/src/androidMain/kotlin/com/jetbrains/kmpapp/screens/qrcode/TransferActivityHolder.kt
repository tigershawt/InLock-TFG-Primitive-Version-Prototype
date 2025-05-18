package com.jetbrains.kmpapp.screens.qrcode

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.os.Build
import com.jetbrains.kmpapp.data.TransferNfcService
import com.jetbrains.kmpapp.data.TransferNfcServiceAndroid
import com.jetbrains.kmpapp.data.TransferNfcServiceRedesigned
import com.jetbrains.kmpapp.data.processTransferTag
import kotlinx.coroutines.flow.MutableStateFlow

actual object TransferActivityHolder : TransferActivityHolderInterface {
    private var currentActivity: Activity? = null
    private var nfcAdapter: NfcAdapter? = null
    private var transferNfcService: TransferNfcService? = null
    private var isForegroundDispatchEnabled = false
    private var pendingIntent: PendingIntent? = null
    private var nfcIntentFilters: Array<IntentFilter>? = null
    private var techLists: Array<Array<String>>? = null

    fun setActivity(activity: Activity, transferNfcService: TransferNfcService) {
        if (currentActivity != null && currentActivity !== activity) {
            disableForegroundDispatch()
        }

        currentActivity = activity
        this.transferNfcService = transferNfcService
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)

        initializeNfcForegroundDispatch()
    }

    fun clearActivity(activity: Activity) {
        if (currentActivity === activity) {
            disableForegroundDispatch()
            currentActivity = null
        }
    }

    private fun initializeNfcForegroundDispatch() {
        val activity = currentActivity ?: return
        val adapter = nfcAdapter ?: return

        try {
            val intent = Intent(activity, activity.javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)

            val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
            try {
                ndef.addDataType("*/*")
            } catch (e: IntentFilter.MalformedMimeTypeException) {
            }

            val tech = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
            val tag = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
            nfcIntentFilters = arrayOf(ndef, tech, tag)

            techLists = arrayOf(
                arrayOf(Ndef::class.java.name),
                arrayOf(MifareClassic::class.java.name),
                arrayOf(MifareUltralight::class.java.name),
                arrayOf(android.nfc.tech.NfcA::class.java.name),
                arrayOf(android.nfc.tech.NfcB::class.java.name),
                arrayOf(android.nfc.tech.NfcF::class.java.name),
                arrayOf(android.nfc.tech.NfcV::class.java.name),
                arrayOf(android.nfc.tech.IsoDep::class.java.name)
            )
        } catch (e: Exception) {
        }
    }

    fun enableTransferForegroundDispatch() {
        val activity = currentActivity ?: return
        val adapter = nfcAdapter ?: return

        try {
            if (pendingIntent == null) {
                initializeNfcForegroundDispatch()
            }

            if (!isForegroundDispatchEnabled) {
                adapter.enableForegroundDispatch(activity, pendingIntent, nfcIntentFilters, techLists)
                isForegroundDispatchEnabled = true
            }
        } catch (e: Exception) {
        }
    }

    fun disableForegroundDispatch() {
        if (isForegroundDispatchEnabled) {
            try {
                val activity = currentActivity
                val adapter = nfcAdapter
                
                if (activity != null && adapter != null) {
                    adapter.disableForegroundDispatch(activity)
                }
            } catch (e: Exception) {
            } finally {
                isForegroundDispatchEnabled = false
            }
        }
    }

    actual override fun processTransferIntent(intent: Any): Boolean {
        if (intent !is Intent) return false
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {

            val tag = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }

            if (tag != null) {
                val service = transferNfcService
                if (service != null) {
                    try {
                        if (service is TransferNfcServiceRedesigned) {
                            val field = service.javaClass.getDeclaredField("_isTransferScanActive")
                            field.isAccessible = true
                            val stateFlow = field.get(service) as MutableStateFlow<Boolean>
                            stateFlow.tryEmit(true)
                        }
                    } catch (e: Exception) {
                    }

                    val tagData = service.processTransferTag(tag)
                    return true
                }
            }
        }
        return false
    }

    fun isTransferNfcAvailable(): Boolean {
        val adapter = nfcAdapter ?: return false
        return adapter.isEnabled
    }
}