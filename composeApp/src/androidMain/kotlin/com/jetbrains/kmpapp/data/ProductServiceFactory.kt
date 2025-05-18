package com.jetbrains.kmpapp.data

actual fun createProductService(
    firebaseService: FirebaseService,
    blockchainService: BlockchainService
): ProductService = FirebaseProductService(firebaseService, blockchainService)