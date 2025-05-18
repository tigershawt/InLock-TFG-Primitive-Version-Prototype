package com.jetbrains.kmpapp.di

import android.app.Application
import android.content.Context
import com.jetbrains.kmpapp.data.AssetTransferNfcService
import com.jetbrains.kmpapp.data.NfcService
import com.jetbrains.kmpapp.data.NfcServiceImpl
import com.jetbrains.kmpapp.data.TransferNfcService
import com.jetbrains.kmpapp.screens.manufacturer.ManufacturerDashboardScreenModel
import com.jetbrains.kmpapp.screens.manufacturer.ManufacturerDashboardScreenModelBase
import com.jetbrains.kmpapp.screens.manufacturer.ProductRegistrationScreenModel
import com.jetbrains.kmpapp.screens.nfc.NfcScreenModel
import com.jetbrains.kmpapp.screens.qrcode.TransferScreenModel
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module

fun Application.initKoinAndroid() {
    try {
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@initKoinAndroid)
            modules(
                dataModule,
                screenModelsModule,
                createAndroidModule()
            )
        }
    } catch (e: Exception) {
        getKoin().loadModules(listOf(createAndroidModule()))
    }
}

private fun createAndroidModule(): Module = module {

    single { NfcServiceImpl() } binds arrayOf(NfcService::class)

    single(named("assetTransferNfc")) { AssetTransferNfcService() }

    single<TransferNfcService> {
        com.jetbrains.kmpapp.data.TransferNfcServiceRedesigned(androidContext())
    }

    factory {
        NfcScreenModel(
            nfcService = get(),
            blockchainService = get(),
            firebaseService = get(),
            productService = get()
        )
    }
    
    factory {
        TransferScreenModel(
            firebaseService = get(),
            productService = get(),
            nfcService = get<TransferNfcService>()
        )
    }
    
    factory { ProductRegistrationScreenModel(get(), get(), get()) }

    factory<ManufacturerDashboardScreenModelBase> { ManufacturerDashboardScreenModel(get(), get(), get()) }
}