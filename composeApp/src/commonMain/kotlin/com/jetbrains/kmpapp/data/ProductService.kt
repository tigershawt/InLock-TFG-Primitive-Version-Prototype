package com.jetbrains.kmpapp.data

interface ProductService {
    suspend fun reportAssetStolen(assetId: String): Result<Unit>

    suspend fun createProduct(product: Product): Result<String>

    suspend fun getProduct(productId: String): Result<Product?>

    suspend fun updateProduct(product: Product): Result<Unit>

    suspend fun uploadProductImage(productId: String, imageBytes: ByteArray): Result<String>

    suspend fun getManufacturerProducts(manufacturerId: String): Result<List<Product>>

    suspend fun getUserProducts(userId: String): Result<List<Product>>

    suspend fun registerProductOnBlockchain(product: Product): Result<String>

    suspend fun transferProductOwnership(productId: String, newOwnerId: String): Result<Unit>

    suspend fun createProductTemplate(template: Product): Result<String>

    suspend fun getManufacturerTemplates(manufacturerId: String): Result<List<Product>>

    suspend fun instantiateProduct(templateId: String, rfidTagId: String): Result<String>

    suspend fun getManufacturerInstantiatedProducts(manufacturerId: String): Result<List<Product>>

    suspend fun forceProductSync(assetId: String): Result<Unit>

    suspend fun getBlockchainProducts(userId: String): Result<List<String>>

    suspend fun syncAllBlockchainProducts(userId: String): Result<Int>

    suspend fun verifyProductOwnership(productId: String, userId: String): Result<Boolean>
}



