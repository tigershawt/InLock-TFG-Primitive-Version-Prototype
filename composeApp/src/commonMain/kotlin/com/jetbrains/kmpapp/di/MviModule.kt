package com.jetbrains.kmpapp.di

import com.jetbrains.kmpapp.data.repository.QRCodeRepository
import com.jetbrains.kmpapp.data.repository.TransferRepository
import com.jetbrains.kmpapp.screens.qrcode.QRCodeModel
import com.jetbrains.kmpapp.screens.qrcode.TransferScreenModelMvi
import org.koin.dsl.module

val mviModule = module {
    factory { QRCodeRepository(get(), get()) }
    factory { QRCodeModel(get()) }

    factory { TransferRepository(get(), get(), get()) }
    factory { TransferScreenModelMvi(get(), get()) }
}