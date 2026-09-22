package com.bexmarket.ng.app

import android.util.Log
import io.appwrite.Query
import io.appwrite.services.Databases
import io.appwrite.services.Storage

class ProductRepository(
    private val databases: Databases,
    private val storage: Storage
) {
    private val DATABASE_ID = "6a4d8f75003a9cd90f61"
    private val PRODUCTS_COLLECTION_ID = "products"
    private val BUCKET_ID = "6a4dce2200029a992eb4"
    private val PROJECT_ID = "6a4d87e7001a45656a73"
    private val FAVORITES_COLLECTION_ID = "favourites"
    private val USERS_COLLECTION_ID = "users"
    private val CONFIG_COLLECTION_ID = "config"

    /**
     * Checks if a new app version is available.
     */
    suspend fun getMinimumRequiredVersion(): Int {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val response = databases.getDocument(
                    databaseId = DATABASE_ID,
                    collectionId = CONFIG_COLLECTION_ID,
                    documentId = "app_settings"
                )
                (response.data["minVersion"] as? Number)?.toInt() ?: 0
            } catch (e: Exception) {
                Log.e("ProductRepository", "Error fetching config: ${e.message}")
                0
            }
        }
    }

    /**
     * Fetches a seller's public profile.
     */
    suspend fun getSellerProfile(userId: String): UserProfile? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                Log.d("SellerDebug", "Fetching profile for userId: $userId from collection: $USERS_COLLECTION_ID")
                val response = databases.getDocument(
                    databaseId = DATABASE_ID,
                    collectionId = USERS_COLLECTION_ID,
                    documentId = userId
                )
                val data = response.data
                val profilePicUrl = data["profilePicUrl"] as? String
                Log.d("SellerDebug", "Successfully fetched profile. Name: ${data["name"]}, Pic URL: $profilePicUrl")
                
                UserProfile(
                    uid = response.id,
                    name = data["name"] as? String ?: "",
                    username = data["username"] as? String ?: "",
                    businessName = data["businessName"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    phone = data["phone"] as? String ?: "",
                    whatsapp = data["whatsapp"] as? String ?: "",
                    profilePic = profilePicUrl,
                    isVerified = data["isVerified"] as? Boolean ?: false
                )
            } catch (e: Exception) {
                Log.e("SellerDebug", "Error fetching seller profile for $userId: ${e.message}")
                null
            }
        }
    }

    /**
     * Fetches all products from Appwrite.
     */
    suspend fun getAllProducts(currentUserId: String? = null): List<MarketItem>? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                Log.d("AppwriteFetch", "Executing listDocuments on $PRODUCTS_COLLECTION_ID with Boosted/Created sort")
                val response = databases.listDocuments(
                    databaseId = DATABASE_ID,
                    collectionId = PRODUCTS_COLLECTION_ID,
                    queries = listOf(
                        Query.orderDesc("boostedAt"),
                        Query.orderDesc("\$createdAt"),
                        Query.limit(100) 
                    )
                )
                
                val favorites = if (currentUserId != null) {
                    getFavoritesForUser(currentUserId)
                } else emptySet()

                Log.d("ProductRepository", "Found ${response.documents.size} documents. User favorites: ${favorites.size}")
                
                response.documents.mapNotNull { doc ->
                    try {
                        val item = mapDocumentToMarketItem(doc.id, doc.data)
                        item.copy(isFavorite = favorites.contains(doc.id))
                    } catch (e: Exception) {
                        Log.e("ProductRepository", "Mapping failed for doc ${doc.id}: ${e.message}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("ProductRepository", "Error fetching products with sort: ${e.message}")
                
                // Fallback to simpler sort if complex index is missing
                try {
                    Log.d("AppwriteFetch", "Attempting fallback fetch with \$createdAt sort")
                    val fallbackResponse = databases.listDocuments(
                        databaseId = DATABASE_ID,
                        collectionId = PRODUCTS_COLLECTION_ID,
                        queries = listOf(
                            Query.orderDesc("\$createdAt"),
                            Query.limit(100)
                        )
                    )
                    
                    val favorites = if (currentUserId != null) {
                        getFavoritesForUser(currentUserId)
                    } else emptySet()
                    
                    fallbackResponse.documents.mapNotNull { doc ->
                        try {
                            val item = mapDocumentToMarketItem(doc.id, doc.data)
                            item.copy(isFavorite = favorites.contains(doc.id))
                        } catch (e2: Exception) { null }
                    }
                } catch (e3: Exception) {
                    Log.e("ProductRepository", "Fallback fetch failed: ${e3.message}")
                    null // Return null to indicate error, not empty
                }
            }
        }
    }

    suspend fun getFavoritesForUser(userId: String): Set<String> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                Log.d("ProductRepository", "Requesting favorites for userId: $userId from collection $FAVORITES_COLLECTION_ID")
                val response = databases.listDocuments(
                    databaseId = DATABASE_ID,
                    collectionId = FAVORITES_COLLECTION_ID,
                    queries = listOf(Query.equal("userId", userId))
                )
                Log.d("ProductRepository", "Success: Fetched ${response.documents.size} favorite records for user $userId")
                response.documents.mapNotNull { 
                    val productId = it.data["productId"] as? String
                    Log.d("ProductRepository", "Mapping favorite: ${it.id} -> productId: $productId")
                    productId 
                }.toSet()
            } catch (e: Exception) {
                Log.e("ProductRepository", "CRITICAL: Error fetching favorites for user $userId: ${e.message}")
                e.printStackTrace()
                emptySet()
            }
        }
    }

    suspend fun addToFavorites(userId: String, productId: String) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                Log.d("ProductRepository", "Adding to favorites: userId=$userId, productId=$productId")
                val result = databases.createDocument(
                    databaseId = DATABASE_ID,
                    collectionId = FAVORITES_COLLECTION_ID,
                    documentId = io.appwrite.ID.unique(),
                    data = mapOf("userId" to userId, "productId" to productId)
                )
                Log.d("ProductRepository", "Success: Favorite created with ID: ${result.id}")
            } catch (e: Exception) {
                Log.e("ProductRepository", "Error in addToFavorites: ${e.message}")
                throw e
            }
        }
    }

    suspend fun removeFromFavorites(userId: String, productId: String) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val response = databases.listDocuments(
                    databaseId = DATABASE_ID,
                    collectionId = FAVORITES_COLLECTION_ID,
                    queries = listOf(
                        Query.equal("userId", userId),
                        Query.equal("productId", productId)
                    )
                )
                for (doc in response.documents) {
                    databases.deleteDocument(DATABASE_ID, FAVORITES_COLLECTION_ID, doc.id)
                }
            } catch (e: Exception) {
                Log.e("ProductRepository", "Error removing favorite: ${e.message}")
            }
        }
    }

    /**
     * Fetches products with price less than a specific amount.
     */
    suspend fun getProductsUnderPrice(maxPrice: Double, currentUserId: String? = null): List<MarketItem> {
        return try {
            Log.d("AppwriteFetch", "Fetching products under price: $maxPrice from collection $PRODUCTS_COLLECTION_ID")
            
            // Try server-side query first with sort
            val response = databases.listDocuments(
                databaseId = DATABASE_ID,
                collectionId = PRODUCTS_COLLECTION_ID,
                queries = listOf(
                    Query.lessThan("price", maxPrice),
                    Query.orderDesc("\$createdAt"),
                    Query.limit(100)
                )
            )
            
            var docs = response.documents
            Log.d("ProductRepository", "Server query returned ${docs.size} docs")
            
            // Fallback: If server query returned nothing, try fetching all and filtering locally
            // This handles cases where indexes are missing or types are mismatched on server
            if (docs.isEmpty()) {
                Log.d("ProductRepository", "Falling back to local filtering for price...")
                val allResponse = databases.listDocuments(
                    databaseId = DATABASE_ID,
                    collectionId = PRODUCTS_COLLECTION_ID,
                    queries = emptyList()
                )
                docs = allResponse.documents.filter { doc ->
                    val p = doc.data["price"] ?: doc.data["Price"] ?: doc.data["price (₦)"]
                    val price = when (p) {
                        is Number -> p.toDouble()
                        is String -> p.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                    price < maxPrice
                }
                Log.d("ProductRepository", "Local filter found ${docs.size} matches")
            }
            
            val favorites = if (currentUserId != null) {
                getFavoritesForUser(currentUserId)
            } else emptySet()
            
            docs.mapNotNull { doc ->
                try {
                    val item = mapDocumentToMarketItem(doc.id, doc.data)
                    item.copy(isFavorite = favorites.contains(doc.id))
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error fetching affordable products: ${e.message}")
            emptyList()
        }
    }

    /**
     * Fetches products within a specific price range.
     */
    suspend fun getProductsByPriceRange(minPrice: Double?, maxPrice: Double?, currentUserId: String? = null): List<MarketItem> {
        return try {
            val queries = mutableListOf<String>()
            if (minPrice != null) queries.add(Query.greaterThanEqual("price", minPrice))
            if (maxPrice != null) queries.add(Query.lessThanEqual("price", maxPrice))
            
            Log.d("AppwriteFetch", "Fetching products with range: min=$minPrice, max=$maxPrice")
            val response = databases.listDocuments(
                databaseId = DATABASE_ID,
                collectionId = PRODUCTS_COLLECTION_ID,
                queries = queries
            )
            
            val favorites = if (currentUserId != null) {
                getFavoritesForUser(currentUserId)
            } else emptySet()
            
            response.documents.mapNotNull { doc ->
                try {
                    val item = mapDocumentToMarketItem(doc.id, doc.data)
                    item.copy(isFavorite = favorites.contains(doc.id))
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error fetching products by price: ${e.message}")
            emptyList()
        }
    }

    /**
     * Fetches products belonging to a specific user.
     */
    suspend fun getProductsByUser(userId: String): List<MarketItem> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                Log.d("AppwriteFetch", "Fetching products for user: $userId")
                val response = databases.listDocuments(
                    databaseId = DATABASE_ID,
                    collectionId = PRODUCTS_COLLECTION_ID,
                    queries = listOf(
                        Query.equal("userId", userId),
                        Query.orderDesc("\$createdAt"),
                        Query.limit(100)
                    )
                )
                response.documents.mapNotNull { doc ->
                    try {
                        mapDocumentToMarketItem(doc.id, doc.data)
                    } catch (e: Exception) {
                        Log.e("ProductRepository", "Mapping failed for user doc ${doc.id}: ${e.message}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("ProductRepository", "Error fetching user products: ${e.message}")
                
                // Fallback without sort if index is missing
                try {
                    val fallbackResponse = databases.listDocuments(
                        databaseId = DATABASE_ID,
                        collectionId = PRODUCTS_COLLECTION_ID,
                        queries = listOf(
                            Query.equal("userId", userId),
                            Query.orderDesc("\$createdAt"),
                            Query.limit(100)
                        )
                    )
                    fallbackResponse.documents.mapNotNull { doc ->
                        try { mapDocumentToMarketItem(doc.id, doc.data) } catch (e2: Exception) { null }
                    }
                } catch (e3: Exception) {
                    emptyList()
                }
            }
        }
    }

    /**
     * Maps Appwrite Document data to MarketItem data class.
     * Ensure keys match Appwrite Console Attributes exactly.
     */
    private fun mapDocumentToMarketItem(docId: String, data: Map<String, Any?>): MarketItem {
        val rawImageUrl = data["imageUrl"] as? String ?: data["image"] as? String ?: ""
        
        val rawImageUrls = (data["imageUrls"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        
        // Appwrite attributes might be returned as Long, Double, or String depending on version and setup
        val priceValue = data["price"] ?: data["Price"] ?: data["price (₦)"]
        val categoryValue = data["category"] ?: "General"
        val userIdValue = data["userId"] ?: data["ownerId"] ?: data["creatorId"] ?: data["user"] ?: data["owner"] ?: ""
        
        if (userIdValue.toString().isEmpty()) {
            Log.e("SellerCheck", "Product Mapping ERROR: No valid userId found for doc $docId. Raw Data: $data")
        } else {
            Log.d("SellerCheck", "Mapped Product $docId to User: $userIdValue")
        }

        val price = when (priceValue) {
            is Number -> priceValue.toDouble()
            is String -> {
                // Remove currency symbols, commas and spaces before parsing
                priceValue.replace("₦", "").replace(",", "").replace(" ", "").toDoubleOrNull() ?: 0.0
            }
            else -> 0.0
        }

        val stock = when (val s = data["stockQuantity"]) {
            is Number -> s.toInt()
            is String -> s.toIntOrNull() ?: 0
            else -> 0
        }

        return MarketItem(
            id = docId,
            name = data["productName"] as? String ?: "Unknown Product",
            price = price,
            description = data["description"] as? String,
            category = categoryValue.toString(),
            stockQuantity = stock,
            sku = data["sku"] as? String ?: "",
            dimensions = data["dimensions"] as? String ?: "",
            imageUrl = getFullImageUrl(rawImageUrl),
            imageUrls = rawImageUrls,
            sellerName = data["sellerName"] as? String ?: "Bex Seller",
            sellerPhone = data["sellerPhone"] as? String ?: "",
            sellerWhatsapp = data["sellerWhatsapp"] as? String ?: "",
            sellerEmail = data["sellerEmail"] as? String ?: "support@bexmarket.ng",
            sellerProfilePic = data["sellerProfilePic"] as? String,
            locationState = data["locationState"] as? String ?: "Lagos",
            locationAxis = data["locationAxis"] as? String ?: "Mainland",
            subCategory = data["subCategory"] as? String,
            brand = data["brand"] as? String,
            model = data["model"] as? String,
            storage = data["storage"] as? String,
            batteryHealth = data["batteryHealth"] as? String,
            processor = data["processor"] as? String,
            ram = data["ram"] as? String,
            ssd = data["ssd"] as? String,
            isVerified = data["isVerified"] as? Boolean ?: false,
            isOnline = data["isOnline"] as? Boolean ?: false,
            isPromoted = data["isPromoted"] as? Boolean ?: false,
            boostedAt = data["boostedAt"] as? String,
            userId = userIdValue.toString()
        )
    }

    /**
     * Fetches products from the same category, excluding the current one.
     */
    suspend fun getSimilarProducts(category: String, currentProductId: String, limit: Int = 10): List<MarketItem> {
        return try {
            val response = databases.listDocuments(
                databaseId = DATABASE_ID,
                collectionId = PRODUCTS_COLLECTION_ID,
                queries = listOf(
                    Query.equal("category", category),
                    Query.notEqual("\$id", currentProductId),
                    Query.limit(limit)
                )
            )
            response.documents.mapNotNull { mapDocumentToMarketItem(it.id, it.data) }
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error fetching similar products: ${e.message}")
            emptyList()
        }
    }
    fun getFullImageUrl(fileId: String): String {
        return if (fileId.startsWith("http")) {
            fileId
        } else if (fileId.isNotEmpty()) {
            "https://cloud.appwrite.io/v1/storage/buckets/$BUCKET_ID/files/$fileId/view?project=$PROJECT_ID"
        } else {
            ""
        }
    }
}
