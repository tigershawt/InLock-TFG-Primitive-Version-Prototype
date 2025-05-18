package com.jetbrains.kmpapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import java.util.UUID

class FirebaseProductService(
    private val firebaseService: FirebaseService,
    private val blockchainService: BlockchainService
) : ProductService {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    private val productsCollection by lazy { firestore.collection("products") }
    private val templatesCollection by lazy { firestore.collection("product_templates") }
    private val storageRef by lazy { storage.reference.child("product_images") }

    override suspend fun createProduct(product: Product): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            if (!firebaseService.isManufacturer()) {
                throw SecurityException("Only manufacturers can create products")
            }

            val currentUserId = firebaseService.getCurrentUserId()
                ?: throw Exception("User not signed in")

            val userData = firebaseService.getUserData(currentUserId).getOrNull()
                ?: throw Exception("User data not found")

            val timestamp = Clock.System.now().toEpochMilliseconds()

            val newProduct = product.copy(
                manufacturer = currentUserId,
                manufacturerName = userData.displayName,
                createdAt = timestamp,
                currentOwner = currentUserId,
                isTemplate = false
            )

            withTimeout(10000) {
                productsCollection.document(newProduct.id).set(newProduct).await()
            }

            return@withContext newProduct.id
        }
    }

    override suspend fun getProduct(productId: String): Result<Product?> = runCatching {
        withContext(Dispatchers.IO) {
            withTimeout(5000) {
                val documentSnapshot = productsCollection.document(productId).get().await()
                if (documentSnapshot.exists()) {
                    documentSnapshot.toObject(Product::class.java)
                } else {
                    null
                }
            }
        }
    }

    override suspend fun updateProduct(product: Product): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val currentUserId = firebaseService.getCurrentUserId()
                ?: throw Exception("User not signed in")

            withTimeout(5000) {
                val existingProductSnapshot =
                    if (product.isTemplate) templatesCollection.document(product.id).get().await()
                    else productsCollection.document(product.id).get().await()

                if (!existingProductSnapshot.exists()) {
                    throw Exception("Product not found")
                }

                val existingProduct = existingProductSnapshot.toObject(Product::class.java)
                    ?: throw Exception("Failed to parse product data")

                if (existingProduct.manufacturer != currentUserId) {
                    throw SecurityException("Only the manufacturer can update this product")
                }

                if (product.isTemplate) {
                    templatesCollection.document(product.id).set(product).await()
                } else {
                    productsCollection.document(product.id).set(product).await()
                }
            }
        }
    }

    override suspend fun uploadProductImage(productId: String, imageBytes: ByteArray): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val currentUserId = firebaseService.getCurrentUserId()
                ?: throw Exception("User not signed in")

            val isTemplate = (getProductTemplate(productId).getOrNull() != null)

            val existingProduct = if (isTemplate) {
                getProductTemplate(productId).getOrNull()
            } else {
                getProduct(productId).getOrNull()
            } ?: throw Exception("Product not found")

            if (existingProduct.manufacturer != currentUserId) {
                throw SecurityException("Only the manufacturer can upload images for this product")
            }

            val filename = "${UUID.randomUUID()}.jpg"
            val imageRef = storageRef.child(productId).child(filename)

            withTimeout(30000) {
                imageRef.putBytes(imageBytes).await()
                val downloadUrl = imageRef.downloadUrl.await().toString()

                val updatedProduct = existingProduct.copy(imageUrl = downloadUrl)
                updateProduct(updatedProduct).getOrThrow()

                return@withTimeout downloadUrl
            }
        }
    }

    override suspend fun getManufacturerProducts(manufacturerId: String): Result<List<Product>> = runCatching {
        withContext(Dispatchers.IO) {
            withTimeout(10000) {
                val snapshot = productsCollection
                    .whereEqualTo("manufacturer", manufacturerId)
                    .whereEqualTo("isTemplate", false)
                    .get()
                    .await()

                return@withTimeout snapshot.documents.mapNotNull {
                    it.toObject(Product::class.java)
                }
            }
        }
    }

    override suspend fun getUserProducts(userId: String): Result<List<Product>> = runCatching {
        withContext(Dispatchers.IO) {
            try {
                val snapshot = productsCollection
                    .whereEqualTo("currentOwner", userId)
                    .whereEqualTo("isTemplate", false)
                    .get()
                    .await()

                val products = snapshot.documents.mapNotNull {
                    it.toObject(Product::class.java)
                }

                return@withContext products
            } catch (e: Exception) {
                try {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        currentUser.getIdToken(true).await()
                    }

                    val allProductsSnapshot = productsCollection.get().await()

                    val filteredProducts = allProductsSnapshot.documents.mapNotNull { doc ->
                        val product = doc.toObject(Product::class.java)
                        if (product != null && product.currentOwner == userId && !product.isTemplate) {
                            product
                        } else {
                            null
                        }
                    }

                    return@withContext filteredProducts
                } catch (fallbackError: Exception) {
                    try {
                        val blockchainAssets = blockchainService.getUserAssets(userId).getOrNull() ?: emptyList()

                        val products = mutableListOf<Product>()
                        for (assetId in blockchainAssets) {
                            try {
                                val docSnapshot = productsCollection.document(assetId).get().await()
                                if (docSnapshot.exists()) {
                                    val product = docSnapshot.toObject(Product::class.java)
                                    if (product != null) {
                                        products.add(product)
                                    }
                                }
                            } catch (_: Exception) { }
                        }

                        if (products.isNotEmpty()) {
                            return@withContext products
                        }
                    } catch (_: Exception) { }

                    throw fallbackError
                }
            }
        }
    }

    override suspend fun registerProductOnBlockchain(product: Product): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val currentUserId = firebaseService.getCurrentUserId()
                ?: throw Exception("User not signed in")

            if (product.manufacturer != currentUserId) {
                throw SecurityException("Only the manufacturer can register this product on the blockchain")
            }

            if (product.isRegisteredOnBlockchain) {
                throw Exception("Product is already registered on the blockchain")
            }

            val assetData = product.properties.toMutableMap()
            assetData["name"] = product.name
            assetData["description"] = product.description
            assetData["category"] = product.category
            assetData["manufacturer"] = product.manufacturerName
            assetData["createdAt"] = product.createdAt.toString()
            if (product.templateId.isNotEmpty()) {
                assetData["templateId"] = product.templateId
            }

            val result = withTimeout(15000) {
                blockchainService.registerAsset(
                    assetId = product.id,
                    userId = currentUserId,
                    assetData = assetData
                ).getOrThrow()
            }

            val updatedProduct = product.copy(isRegisteredOnBlockchain = true)

            withTimeout(5000) {
                productsCollection.document(product.id).set(updatedProduct).await()
            }

            return@withContext result
        }
    }

    override suspend fun forceProductSync(assetId: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val currentUserId = firebaseService.getCurrentUserId()
                ?: throw Exception("User not signed in")


            val history = withTimeout(10000) {
                blockchainService.getAssetHistory(assetId).getOrThrow()
            }

            if (history.isEmpty()) {
                throw Exception("Asset not found on blockchain")
            }


            val ownershipResult = withTimeout(5000) {
                blockchainService.verifyOwnership(assetId, currentUserId).getOrNull() ?: false
            }

            val registrationRecord = history.find { it.action == "register" }
            val latestRecord = history.last()

            if (registrationRecord == null) {
                throw Exception("No registration record found for asset")
            }

            val nodeData = try {
                withTimeout(8000) {
                    blockchainService.getAssetNodeData(assetId).getOrNull() ?: emptyMap()
                }
            } catch (e: Exception) {
                emptyMap<String, String>()
            }


            val manufacturerId = registrationRecord.user_id

            val currentOwnerId = if (ownershipResult) currentUserId else latestRecord.user_id

            var manufacturerName = nodeData["manufacturer"] ?: ""
            if (manufacturerName.isEmpty()) {
                try {
                    val userData = firebaseService.getUserData(manufacturerId).getOrNull()
                    manufacturerName = userData?.displayName ?: "Unknown Manufacturer"
                } catch (e: Exception) {
                    manufacturerName = "Unknown Manufacturer"
                }
            }

            val reservedKeys = setOf(
                "name", "description", "category", "manufacturer",
                "createdAt", "templateId", "status"
            )

            val properties = HashMap<String, String>()
            nodeData.forEach { (key, value) ->
                if (key !in reservedKeys) {
                    properties[key] = value
                }
            }

            val isStolen = nodeData["status"] == "stolen"
            if (isStolen) {
                properties["status"] = "stolen"
            }

            val product = Product(
                id = assetId,
                name = nodeData["name"] ?: "Asset $assetId",
                description = nodeData["description"] ?: "Asset recovered from blockchain",
                category = nodeData["category"] ?: "Recovered Asset",
                manufacturer = manufacturerId,
                manufacturerName = manufacturerName,
                properties = properties,
                createdAt = nodeData["createdAt"]?.toLongOrNull() ?: registrationRecord.timestamp.toLong(),
                currentOwner = currentOwnerId,
                isRegisteredOnBlockchain = true,
                isTemplate = false,
                templateId = nodeData["templateId"] ?: ""
            )

            val existingProduct = getProduct(assetId).getOrNull()

            if (existingProduct != null) {
                val mergedProduct = existingProduct.copy(
                    name = if (existingProduct.name.isNotEmpty()) existingProduct.name else product.name,
                    description = if (existingProduct.description.isNotEmpty()) existingProduct.description else product.description,
                    currentOwner = product.currentOwner,
                    isRegisteredOnBlockchain = true,
                    properties = existingProduct.properties + product.properties
                )

                productsCollection.document(assetId).set(mergedProduct).await()
            } else {
                productsCollection.document(assetId).set(product).await()
            }

            val syncedProduct = withTimeout(5000) {
                getProduct(assetId).getOrNull()
            }

            if (syncedProduct == null) {
                throw Exception("Failed to verify product after sync")
            }

            try {
                blockchainService.verifyOwnership(assetId, currentOwnerId)
            } catch (_: Exception) { }

            return@withContext
        }
    }

    override suspend fun transferProductOwnership(productId: String, newOwnerId: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
                        
            val currentUserId = firebaseService.getCurrentUserId()
                ?: throw Exception("User not signed in")

            val isOwnerOnBlockchain = withTimeoutOrNull(10000) {
                blockchainService.verifyOwnership(productId, currentUserId).getOrNull() ?: false
            } ?: false
            
            if (!isOwnerOnBlockchain) {
                throw SecurityException("Blockchain verification failed: You are not the owner of this asset")
            }

            val blockchainResult = withTimeout(15000) {
                blockchainService.transferAsset(
                    assetId = productId,
                    fromUserId = currentUserId,
                    toUserId = newOwnerId
                )
            }
            
            if (blockchainResult.isFailure) {
                val error = blockchainResult.exceptionOrNull()
                throw error ?: Exception("Blockchain transfer failed")
            }

            try {
                val product = getProduct(productId).getOrNull()
                
                if (product != null) {
                    val updatedProduct = product.copy(
                        currentOwner = newOwnerId,
                        isRegisteredOnBlockchain = true
                    )
                    
                    withTimeout(5000) {
                        productsCollection.document(productId).set(updatedProduct).await()
                    }
                } else {
                    

                    val assetData = withTimeoutOrNull(5000) {
                        blockchainService.getAssetNodeData(productId).getOrNull() ?: emptyMap<String, String>()
                    } ?: emptyMap<String, String>()
                    
                    val minimalProduct = Product(
                        id = productId,
                        name = assetData["name"] ?: "Asset $productId",
                        description = assetData["description"] ?: "Asset from blockchain",
                        category = assetData["category"] ?: "Blockchain Asset",
                        manufacturer = currentUserId,
                        manufacturerName = assetData["manufacturer"] ?: "Unknown",
                        currentOwner = newOwnerId,
                        isRegisteredOnBlockchain = true,
                        createdAt = assetData["createdAt"]?.toLongOrNull() ?: System.currentTimeMillis(),
                        properties = assetData.filterKeys<String, String> { 
                            it !in listOf("name", "description", "category", "manufacturer", "createdAt") 
                        }.mapValues<String, String, String> { it.value }
                    )
                    
                    withTimeout(5000) {
                        productsCollection.document(productId).set(minimalProduct).await()
                    }
                }
            } catch (e: Exception) {
            }
            
        }
    }

    override suspend fun createProductTemplate(template: Product): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            if (!firebaseService.isManufacturer()) {
                throw SecurityException("Only manufacturers can create product templates")
            }

            val currentUserId = firebaseService.getCurrentUserId()
                ?: throw Exception("User not signed in")

            val userData = firebaseService.getUserData(currentUserId).getOrNull()
                ?: throw Exception("User data not found")

            val timestamp = Clock.System.now().toEpochMilliseconds()
            val templateId = UUID.randomUUID().toString()

            val newTemplate = template.copy(
                id = templateId,
                manufacturer = currentUserId,
                manufacturerName = userData.displayName,
                createdAt = timestamp,
                currentOwner = currentUserId,
                isTemplate = true
            )

            val templateMap = mapOf(
                "id" to newTemplate.id,
                "name" to newTemplate.name,
                "description" to newTemplate.description,
                "manufacturer" to newTemplate.manufacturer,
                "manufacturerName" to newTemplate.manufacturerName,
                "category" to newTemplate.category,
                "imageUrl" to newTemplate.imageUrl,
                "createdAt" to newTemplate.createdAt,
                "properties" to newTemplate.properties,
                "currentOwner" to newTemplate.currentOwner,
                "isRegisteredOnBlockchain" to newTemplate.isRegisteredOnBlockchain,
                "isTemplate" to true,
                "templateId" to newTemplate.templateId
            )

            withTimeout(10000) {
                templatesCollection.document(templateId).set(templateMap).await()
            }

            return@withContext templateId
        }
    }

    override suspend fun getManufacturerTemplates(manufacturerId: String): Result<List<Product>> = runCatching {
        withContext(Dispatchers.IO) {
            withTimeout(10000) {
                val snapshot = templatesCollection
                    .whereEqualTo("manufacturer", manufacturerId)
                    .get()
                    .await()

                val templates = snapshot.documents
                    .filter { doc ->
                        val isTemplate = doc.getBoolean("isTemplate")
                        isTemplate == true || isTemplate == null
                    }
                    .mapNotNull { doc ->
                        try {
                            val template = doc.toObject(Product::class.java)
                            template?.copy(isTemplate = true)
                        } catch (_: Exception) {
                            null
                        }
                    }

                return@withTimeout templates
            }
        }
    }

    private suspend fun getProductTemplate(templateId: String): Result<Product?> = runCatching {
        withContext(Dispatchers.IO) {
            withTimeout(5000) {
                val documentSnapshot = templatesCollection.document(templateId).get().await()
                if (documentSnapshot.exists()) {
                    val template = documentSnapshot.toObject(Product::class.java)
                    template?.copy(isTemplate = true)
                } else {
                    null
                }
            }
        }
    }

    override suspend fun instantiateProduct(templateId: String, rfidTagId: String): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            if (!firebaseService.isManufacturer()) {
                throw SecurityException("Only manufacturers can instantiate products")
            }

            val currentUserId = firebaseService.getCurrentUserId()
                ?: throw Exception("User not signed in")

            val template = getProductTemplate(templateId).getOrNull()
                ?: throw Exception("Template not found")

            if (template.manufacturer != currentUserId) {
                throw SecurityException("Only the template owner can instantiate products from it")
            }

            val timestamp = Clock.System.now().toEpochMilliseconds()

            val newProduct = template.copy(
                id = rfidTagId,
                isTemplate = false,
                createdAt = timestamp,
                templateId = templateId,
                currentOwner = currentUserId
            )

            withTimeout(10000) {
                productsCollection.document(rfidTagId).set(newProduct).await()
            }

            return@withContext rfidTagId
        }
    }

    override suspend fun getManufacturerInstantiatedProducts(manufacturerId: String): Result<List<Product>> = runCatching {
        withContext(Dispatchers.IO) {
            withTimeout(10000) {
                val snapshot = productsCollection
                    .whereEqualTo("manufacturer", manufacturerId)
                    .whereEqualTo("isTemplate", false)
                    .get()
                    .await()

                return@withTimeout snapshot.documents.mapNotNull {
                    it.toObject(Product::class.java)
                }
            }
        }
    }

    override suspend fun reportAssetStolen(assetId: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val currentUserId = firebaseService.getCurrentUserId()
                ?: throw Exception("User not signed in")

            val product = getProduct(assetId).getOrThrow()
                ?: throw Exception("Product not found: $assetId")

            if (product.currentOwner != currentUserId) {
                throw SecurityException("Only the owner can report this asset as stolen")
            }

            val updatedProperties = HashMap(product.properties)
            updatedProperties["status"] = "stolen"

            val updatedProduct = product.copy(
                properties = updatedProperties
            )

            withTimeout(10000) {
                productsCollection.document(assetId).set(updatedProduct).await()
            }

            if (product.isRegisteredOnBlockchain) {
                try {
                    withTimeout(10000) {
                        val blockchainData = mapOf(
                            "status" to "stolen",
                            "reported_at" to Clock.System.now().toEpochMilliseconds().toString(),
                            "reported_by" to currentUserId
                        )

                        blockchainService.registerAsset(
                            assetId = assetId,
                            userId = currentUserId,
                            assetData = blockchainData
                        ).getOrThrow()
                    }
                } catch (_: Exception) { }
            }

            return@withContext
        }
    }

    override suspend fun getBlockchainProducts(userId: String): Result<List<String>> = runCatching {
        withContext(Dispatchers.IO) {
            val assets = blockchainService.getUserAssets(userId).getOrThrow()
            return@withContext assets
        }
    }

    override suspend fun syncAllBlockchainProducts(userId: String): Result<Int> = runCatching {
        withContext(Dispatchers.IO) {
            val assetIds = blockchainService.getUserAssets(userId).getOrThrow()
            if (assetIds.isEmpty()) {
                return@withContext 0
            }

            var syncedCount = 0

            for (assetId in assetIds) {
                try {
                    forceProductSync(assetId).getOrThrow()
                    syncedCount++
                } catch (_: Exception) { }
            }

            return@withContext syncedCount
        }
    }

    override suspend fun verifyProductOwnership(productId: String, userId: String): Result<Boolean> = runCatching {
        withContext(Dispatchers.IO) {
            
            val firestoreProduct = getProduct(productId).getOrNull()
            
            
            
            if (firestoreProduct != null && !firestoreProduct.isRegisteredOnBlockchain) {
                
                return@withContext firestoreProduct.currentOwner == userId
            }
            
            
            try {
                val isOwner = blockchainService.verifyOwnership(productId, userId).getOrThrow()
                
                
                if (isOwner && firestoreProduct != null && firestoreProduct.currentOwner != userId) {
                    try {
                        val updatedProduct = firestoreProduct.copy(
                            currentOwner = userId,
                            isRegisteredOnBlockchain = true 
                        )
                        updateProduct(updatedProduct).getOrThrow()
                    } catch (e: Exception) {
                            
                    }
                }
                
                return@withContext isOwner
            } catch (e: Exception) {
                
                if (firestoreProduct != null) {
                    
                    return@withContext firestoreProduct.currentOwner == userId
                }
                
                
                throw Exception("Failed to verify product ownership: ${e.message}")
            }
        }
    }
}