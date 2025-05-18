package com.jetbrains.kmpapp.screens.qrcode

import com.jetbrains.kmpapp.data.TransferNfcService

interface TransferActivityHolderInterface {
    fun processTransferIntent(intent: Any): Boolean
}

expect object TransferActivityHolder : TransferActivityHolderInterface {
    override fun processTransferIntent(intent: Any): Boolean
}