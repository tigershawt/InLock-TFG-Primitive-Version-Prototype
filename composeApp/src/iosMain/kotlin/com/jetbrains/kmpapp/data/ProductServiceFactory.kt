package com.jetbrains.kmpapp.data

class ProductServiceStub(
    private val firebaseService: FirebaseService,
    private val blockchainService: BlockchainService
) : ProductService {
    override suspend fun createProduct(product: Product): Result<String> =
        Result.failure(UnsupportedOperationException("Product service not implemented on iOS yet"))

    override suspend fun getProduct(productId: String): Result<Product?> =
        Result.success(null)

    override suspend fun updateProduct(product: Product): Result<Unit> =
        Result.failure(UnsupportedOperationException("Product service not implemented on iOS yet"))

    override suspend fun uploadProductImage(productId: String, imageBytes: ByteArray): Result<String> =
        Result.failure(UnsupportedOperationException("Product service not implemented on iOS yet"))

    override suspend fun getManufacturerProducts(manufacturerId: String): Result<List<Product>> =
        Result.success(emptyList())

    override suspend fun getUserProducts(userId: String): Result<List<Product>> =
        Result.success(emptyList())

    override suspend fun registerProductOnBlockchain(product: Product): Result<String> =
        Result.failure(UnsupportedOperationException("Product service not implemented on iOS yet"))

    override suspend fun transferProductOwnership(productId: String, newOwnerId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Product service not implemented on iOS yet"))

    override suspend fun createProductTemplate(template: Product): Result<String> =
        Result.failure(UnsupportedOperationException("Product service not implemented on iOS yet"))

    override suspend fun getManufacturerTemplates(manufacturerId: String): Result<List<Product>> =
        Result.success(emptyList())

    override suspend fun instantiateProduct(templateId: String, rfidTagId: String): Result<String> =
        Result.failure(UnsupportedOperationException("Product service not implemented on iOS yet"))

    override suspend fun getManufacturerInstantiatedProducts(manufacturerId: String): Result<List<Product>> =
        Result.success(emptyList())
    
    override suspend fun reportAssetStolen(assetId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Product service not implemented on iOS yet"))
        
    override suspend fun getBlockchainProducts(userId: String): Result<List<String>> =
        Result.success(emptyList())
        
    override suspend fun syncAllBlockchainProducts(userId: String): Result<Int> =
        Result.success(0)
        
    override suspend fun verifyProductOwnership(productId: String, userId: String): Result<Boolean> =
        Result.success(false)
        
    override suspend fun forceProductSync(assetId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Product service not implemented on iOS yet"))
}

actual fun createProductService(
    firebaseService: FirebaseService,
    blockchainService: BlockchainService
): ProductService = ProductServiceStub(firebaseService, blockchainService)