package com.jetbrains.kmpapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Handler
import android.os.Looper

class NfcStateReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "NfcStateReceiver"

        var isNfcEnabled = false
            private set

        var shouldResumeScanning = false
            private set

        fun requestScanningResume(shouldResume: Boolean) {
            shouldResumeScanning = shouldResume
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
            val state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF)

            when (state) {
                NfcAdapter.STATE_ON -> {
                    isNfcEnabled = true

                    try {
                        context.sendBroadcast(Intent("com.jetbrains.kmpapp.NFC_ENABLED"))
                    } catch (e: Exception) {
                    }

                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            val mainActivity = MainActivity.INSTANCE
                            mainActivity?.refreshNfcState()

                            if (shouldResumeScanning) {
                                mainActivity?.restartTransferNfcScan()
                                shouldResumeScanning = false
                            }
                        } catch (e: Exception) {
                        }
                    }, 1000)
                }

                NfcAdapter.STATE_OFF -> {
                    isNfcEnabled = false

                    try {
                        context.sendBroadcast(Intent("com.jetbrains.kmpapp.NFC_DISABLED"))
                    } catch (e: Exception) {
                    }

                    val mainActivity = MainActivity.INSTANCE
                    if (mainActivity != null) {
                        try {
                            val transferNfcService = mainActivity.getTransferNfcServiceForExternal()
                            if (transferNfcService.isTransferScanActive.value) {
                                shouldResumeScanning = true
                            }
                        } catch (e: Exception) {
                        }

                        mainActivity.refreshNfcState()
                    }
                }

                NfcAdapter.STATE_TURNING_ON -> {
                }

                NfcAdapter.STATE_TURNING_OFF -> {
                }
            }
        }
    }
}