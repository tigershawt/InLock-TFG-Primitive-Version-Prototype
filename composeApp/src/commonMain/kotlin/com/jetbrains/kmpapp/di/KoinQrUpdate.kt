package com.jetbrains.kmpapp.di

import com.jetbrains.kmpapp.screens.assets.EnhancedAssetDetailScreenModel
import com.jetbrains.kmpapp.screens.qrcode.QRCodeScreenModel
import com.jetbrains.kmpapp.screens.qrcode.TransferScreenModel
import com.jetbrains.kmpapp.screens.qrcode.createQRCodeService
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

val qrCodeModule = module {
    single { createQRCodeService() }

    factory { QRCodeScreenModel(get(), get()) }
    factory { TransferScreenModel(get(), get(), get()) }

    factory { EnhancedAssetDetailScreenModel(get(), get(), get()) }
}

fun initQrCodeModule() {
    loadKoinModules(qrCodeModule)
}