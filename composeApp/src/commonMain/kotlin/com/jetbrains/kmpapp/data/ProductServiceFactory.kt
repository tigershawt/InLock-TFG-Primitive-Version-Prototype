package com.jetbrains.kmpapp.data

expect fun createProductService(
    firebaseService: FirebaseService,
    blockchainService: BlockchainService
): ProductService