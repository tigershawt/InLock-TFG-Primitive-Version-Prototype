
package com.jetbrains.kmpapp.di

import com.jetbrains.kmpapp.screens.manufacturer.ManufacturerDashboardScreenModelBase
import com.jetbrains.kmpapp.screens.manufacturer.ManufacturerDashboardScreenModelStub
import com.jetbrains.kmpapp.screens.manufacturer.ProductRegistrationScreenModel
import com.jetbrains.kmpapp.screens.nfc.NfcScreenModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

val iosModule = module {
    factory { NfcScreenModel(get(), get(), get(), get()) }
    factory { ProductRegistrationScreenModel(get(), get(), get()) }
    factory<ManufacturerDashboardScreenModelBase> { ManufacturerDashboardScreenModelStub() }
}

fun initKoinIOS() {
    try {
        initKoin()
        
        org.koin.core.context.loadKoinModules(iosModule)
    } catch (e: Exception) {
        
        startKoin {
            modules(
                dataModule,
                screenModelsModule,
                iosModule
            )
        }
    }
}