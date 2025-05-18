package com.jetbrains.kmpapp.di

import com.jetbrains.kmpapp.data.BlockchainService
import com.jetbrains.kmpapp.data.ProductService
import com.jetbrains.kmpapp.data.createNfcService
import com.jetbrains.kmpapp.data.createProductService
import com.jetbrains.kmpapp.data.firebaseService
import com.jetbrains.kmpapp.screens.admin.AdminScreenModel
import com.jetbrains.kmpapp.screens.assets.AssetDetailScreenModel
import com.jetbrains.kmpapp.screens.assets.AssetsScreenModel
import com.jetbrains.kmpapp.screens.assets.EnhancedAssetDetailScreenModel
import com.jetbrains.kmpapp.screens.home.HomeScreenModel
import com.jetbrains.kmpapp.screens.login.LoginScreenModel
import com.jetbrains.kmpapp.screens.nfc.NfcScreenModel
import com.jetbrains.kmpapp.screens.profile.ProfileScreenModel
import com.jetbrains.kmpapp.screens.qrcode.QRCodeScreenModel
import com.jetbrains.kmpapp.screens.qrcode.TransferScreenModel
import com.jetbrains.kmpapp.screens.qrcode.createQRCodeService
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule = module {
    single {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = true
            encodeDefaults = true
        }
        HttpClient {
            install(ContentNegotiation) {
                json(json, contentType = ContentType.Any)
            }

            install(HttpTimeout) {
                connectTimeoutMillis = 15000
                requestTimeoutMillis = 30000
                socketTimeoutMillis = 15000
            }
        }
    }

    single { firebaseService() }
    single { BlockchainService(get()) }
    single { createNfcService() }
    single { createQRCodeService() }
    single { createProductService(get(), get()) } bind ProductService::class
}

val screenModelsModule = module {
    factoryOf(::LoginScreenModel)
    factoryOf(::AdminScreenModel)
    factoryOf(::HomeScreenModel)
    factoryOf(::ProfileScreenModel)
    factoryOf(::AssetsScreenModel)
    factoryOf(::AssetDetailScreenModel)
    factoryOf(::EnhancedAssetDetailScreenModel)
    factoryOf(::NfcScreenModel)
    factoryOf(::QRCodeScreenModel)
    factoryOf(::TransferScreenModel)
}

fun initKoin(additionalModules: List<Module> = emptyList()) {
    try {
        startKoin {
            modules(
                dataModule,
                screenModelsModule,
                mviModule,
                *additionalModules.toTypedArray()
            )
        }
    } catch (e: Exception) {
        
    }
}

fun doInitKoin() {
    initKoin()
}