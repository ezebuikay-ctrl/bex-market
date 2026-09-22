package com.bexmarket.ng.app

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.appwrite.ID
import io.appwrite.models.InputFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

sealed class MarketUiState {
    object Loading : MarketUiState()
    data class Success(val items: List<MarketItem>) : MarketUiState()
    data class Error(val message: String) : MarketUiState()
}

class MarketViewModel : ViewModel() {
    private val repository get() = AppwriteProvider.repository
    private val databases get() = AppwriteProvider.databases
    private val storage get() = AppwriteProvider.storage

    private val DATABASE_ID = "6a4d8f75003a9cd90f61" 
    private val PRODUCTS_COLLECTION_ID = "products"
    private val BUCKET_ID = "6a4dce2200029a992eb4"

    private val _uiState = MutableStateFlow<MarketUiState>(MarketUiState.Loading)
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    private var allItems: List<MarketItem> = emptyList()
    
    private val _categories = MutableStateFlow<List<String>>(listOf("All"))
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedLocation = MutableStateFlow("All Locations")
    val selectedLocation: StateFlow<String> = _selectedLocation.asStateFlow()

    private val _selectedSubCategory = MutableStateFlow("All")
    val selectedSubCategory: StateFlow<String> = _selectedSubCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val searchHistoryManager = SearchHistoryManager(AppwriteProvider.context)

    private val _priceRange = MutableStateFlow<Pair<Double?, Double?>?>(null)
    val priceRange: StateFlow<Pair<Double?, Double?>?> = _priceRange.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _promotedItems = MutableStateFlow<List<MarketItem>>(emptyList())
    val promotedItems: StateFlow<List<MarketItem>> = _promotedItems.asStateFlow()

    private val _similarProducts = MutableStateFlow<List<MarketItem>>(emptyList())
    val similarProducts: StateFlow<List<MarketItem>> = _similarProducts.asStateFlow()

    private var currentUserId: String? = null

    private var lastFetchTime: Long = 0
    private val CACHE_EXPIRY = 5 * 60 * 1000 // 5 minutes

    private val gson = com.google.gson.Gson()
    private val CACHE_FILE_NAME = "products_cache.json"

    init {
        loadFromDisk()
        fetchProducts()
        loadSearchHistory()
    }

    private fun loadSearchHistory() {
        _searchHistory.value = searchHistoryManager.getHistory()
    }

    private fun saveToDisk() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = AppwriteProvider.context
                val file = java.io.File(context.filesDir, CACHE_FILE_NAME)
                file.writeText(gson.toJson(allItems))
                Log.d("MarketViewModel", "Cache saved to disk: ${allItems.size} items")
            } catch (e: Exception) {
                Log.e("MarketViewModel", "Failed to save cache: ${e.message}")
            }
        }
    }

    private fun loadFromDisk() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = AppwriteProvider.context
                val file = java.io.File(context.filesDir, CACHE_FILE_NAME)
                if (file.exists()) {
                    val json = file.readText()
                    val type = object : com.google.gson.reflect.TypeToken<List<MarketItem>>() {}.type
                    val cachedItems: List<MarketItem> = gson.fromJson(json, type)
                    if (cachedItems.isNotEmpty()) {
                        allItems = cachedItems
                        Log.d("MarketViewModel", "Cache loaded from disk: ${allItems.size} items")
                        // Immediately update UI with cached data if we haven't fetched yet
                        if (_uiState.value is MarketUiState.Loading) {
                            applyFilters()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MarketViewModel", "Failed to load cache: ${e.message}")
            }
        }
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
        _selectedSubCategory.value = "All" // Reset sub-category when main changes
        applyFilters()
    }

    fun onSubCategorySelected(subCategory: String) {
        _selectedSubCategory.value = subCategory
        applyFilters()
    }

    fun onLocationSelected(location: String) {
        _selectedLocation.value = location
        applyFilters()
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        applyFilters()
    }

    fun saveSearchQuery(query: String) {
        if (query.isNotBlank()) {
            searchHistoryManager.saveSearch(query)
            loadSearchHistory()
        }
    }

    fun clearSearchHistory() {
        searchHistoryManager.clearHistory()
        loadSearchHistory()
    }

    fun onPriceRangeSelected(min: Double?, max: Double?) {
        _priceRange.value = min to max
        fetchProducts(currentUserId)
    }

    fun clearPriceFilter() {
        _priceRange.value = null
        fetchProducts(currentUserId)
    }

    fun toggleFavorite(userId: String, productId: String) {
        viewModelScope.launch {
            try {
                val item = allItems.find { it.id == productId } ?: return@launch
                val isNowFavorite = !item.isFavorite
                
                // Optimistic UI update
                allItems = allItems.map { 
                    if (it.id == productId) it.copy(isFavorite = isNowFavorite) else it 
                }
                applyFilters()

                // Sync with backend on IO thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    if (isNowFavorite) {
                        repository.addToFavorites(userId, productId)
                    } else {
                        repository.removeFromFavorites(userId, productId)
                    }
                }
                Log.d("MarketViewModel", "Favorite toggled for $productId: $isNowFavorite")
            } catch (e: Exception) {
                Log.e("MarketViewModel", "Error toggling favorite: ${e.message}", e)
                // Rollback on error
                fetchProducts(userId)
            }
        }
    }

    fun addOrUpdateProduct(context: android.content.Context, product: MarketItem, imageUris: List<Uri>, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("MarketViewModel", "Starting to add product: ${product.name} with ${imageUris.size} images")
                
                val uploadedImageIds = mutableListOf<String>()
                for (uri in imageUris) {
                    try {
                        val fileId = uploadImage(context, uri)
                        uploadedImageIds.add(fileId)
                    } catch (e: Exception) {
                        Log.e("MarketViewModel", "Failed to upload one of the images: ${e.message}")
                        // We might want to continue or fail here. Let's fail for now to ensure consistency.
                        throw Exception("Image upload failed: ${e.message}")
                    }
                }

                val primaryImageUrl = if (uploadedImageIds.isNotEmpty()) {
                    repository.getFullImageUrl(uploadedImageIds.first())
                } else product.imageUrl

                // Sync with Appwrite Database on IO thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val data = mutableMapOf(
                        "productName" to product.name,
                        "price" to product.price,
                        "description" to product.description,
                        "category" to product.category,
                        "stockQuantity" to (product.stockQuantity ?: 0),
                        "sku" to (product.sku ?: ""),
                        "dimensions" to (product.dimensions ?: ""),
                        "imageUrl" to (primaryImageUrl ?: ""),
                        "imageUrls" to uploadedImageIds, 
                        "sellerName" to (product.sellerName ?: ""),
                        "sellerPhone" to (product.sellerPhone ?: ""),
                        "sellerWhatsapp" to (product.sellerWhatsapp ?: ""),
                        "sellerProfilePic" to (product.sellerProfilePic ?: ""),
                        "locationState" to (product.locationState ?: "Lagos"),
                        "subCategory" to (product.subCategory ?: ""),
                        "brand" to (product.brand ?: ""),
                        "model" to (product.model ?: ""),
                        "storage" to (product.storage ?: ""),
                        "batteryHealth" to (product.batteryHealth ?: ""),
                        "processor" to (product.processor ?: ""),
                        "ram" to (product.ram ?: ""),
                        "ssd" to (product.ssd ?: ""),
                        "userId" to product.userId,
                        "boostedAt" to "" // Initialize with empty string to ensure it exists for sorting
                    )
                    
                    Log.d("MarketViewModel", "Creating document in Appwrite with data: $data")
                    val response = databases.createDocument(
                        databaseId = DATABASE_ID,
                        collectionId = PRODUCTS_COLLECTION_ID,
                        documentId = ID.unique(),
                        data = data
                    )
                    Log.d("MarketViewModel", "Document created successfully: ${response.id}")
                }
                
                lastFetchTime = 0 
                refreshProducts()
                onResult(true, null)
                
            } catch (e: Exception) {
                Log.e("MarketViewModel", "CRITICAL ERROR adding product: ${e.message}", e)
                onResult(false, e.localizedMessage ?: "Unknown error occurred during posting")
            }
        }
    }

    fun updateProduct(
        context: android.content.Context,
        productId: String,
        title: String,
        price: Double,
        description: String,
        category: String,
        subCategory: String,
        brand: String,
        model: String,
        storage: String,
        batteryHealth: String,
        processor: String,
        ram: String,
        ssd: String,
        locationState: String,
        locationAxis: String,
        sellerPhone: String,
        sellerWhatsapp: String,
        existingImageIds: List<String>,
        newImageUris: List<Uri>,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("MarketViewModel", "Updating product $productId: $title")
                val newlyUploadedIds = mutableListOf<String>()
                for (uri in newImageUris) {
                    newlyUploadedIds.add(uploadImage(context, uri))
                }

                val finalImageIds = existingImageIds + newlyUploadedIds
                val primaryImageId = if (finalImageIds.isNotEmpty()) finalImageIds.first() else ""
                val primaryImageUrl = if (primaryImageId.isNotEmpty()) repository.getFullImageUrl(primaryImageId) else ""

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val data = mapOf(
                        "productName" to title,
                        "price" to price,
                        "description" to description,
                        "category" to category,
                        "subCategory" to subCategory,
                        "brand" to brand,
                        "model" to model,
                        "storage" to storage,
                        "batteryHealth" to batteryHealth,
                        "processor" to processor,
                        "ram" to ram,
                        "ssd" to ssd,
                        "locationState" to locationState,
                        "locationAxis" to locationAxis,
                        "sellerPhone" to sellerPhone,
                        "sellerWhatsapp" to sellerWhatsapp,
                        "imageUrls" to finalImageIds,
                        "imageUrl" to primaryImageUrl
                    )
                    Log.d("MarketViewModel", "Updating document in Appwrite: $productId with $data")
                    databases.updateDocument(
                        databaseId = DATABASE_ID,
                        collectionId = PRODUCTS_COLLECTION_ID,
                        documentId = productId,
                        data = data
                    )
                }
                
                lastFetchTime = 0
                refreshProducts()
                onResult(true, null)
            } catch (e: Exception) {
                Log.e("MarketViewModel", "CRITICAL ERROR updating product: ${e.message}", e)
                onResult(false, e.localizedMessage ?: "Failed to update product details")
            }
        }
    }

    private suspend fun fetchProductsSequentially() {
        try {
            Log.d("MarketViewModel", "fetchProductsSequentially() START. User: $currentUserId")
            
            val fetchedItems = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                repository.getAllProducts(currentUserId)
            }
            
            if (fetchedItems != null) {
                allItems = fetchedItems
                Log.d("MarketViewModel", "Cache updated with ${allItems.size} items")
                saveToDisk()
                
                if (allItems.isEmpty()) {
                    _uiState.value = MarketUiState.Success(emptyList())
                }
            } else {
                Log.w("MarketViewModel", "Fetch failed, keeping existing cache if any")
                if (allItems.isEmpty()) {
                    _uiState.value = MarketUiState.Error("Failed to load products. Please check your connection.")
                }
            }
            
            updateCategories(allItems)
            
            // Apply all filters locally
            applyFilters()
            
        } catch (e: Exception) {
            Log.e("MarketViewModel", "fetchProductsSequentially ERROR: ${e.message}", e)
            _uiState.value = MarketUiState.Error("Sync Error: ${e.message}")
        }
    }

    private suspend fun uploadImage(context: android.content.Context, uri: Uri): String {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            Log.d("MarketViewModel", "Starting image upload for URI: $uri")
            val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            
            // Image Compression & Resizing
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val originalBitmap = android.graphics.BitmapFactory.decodeStream(input)
                    if (originalBitmap != null) {
                        // Resize if too large (e.g., max 1200px)
                        val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                        val maxWidth = 1200f
                        val maxHeight = 1200f
                        
                        var newWidth = originalBitmap.width
                        var newHeight = originalBitmap.height
                        
                        if (originalBitmap.width > maxWidth || originalBitmap.height > maxHeight) {
                            if (ratio > 1) {
                                newWidth = maxWidth.toInt()
                                newHeight = (maxWidth / ratio).toInt()
                            } else {
                                newHeight = maxHeight.toInt()
                                newWidth = (maxHeight * ratio).toInt()
                            }
                        }
                        
                        val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                        
                        FileOutputStream(file).use { output ->
                            resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, output)
                        }
                        Log.d("MarketViewModel", "Image compressed and resized: ${newWidth}x${newHeight}, size: ${file.length() / 1024} KB")
                    } else {
                        // Fallback to direct copy if decode fails
                        FileOutputStream(file).use { output -> input.copyTo(output) }
                    }
                }
            } catch (e: Exception) {
                Log.e("MarketViewModel", "Compression failed, falling back to direct copy: ${e.message}")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
            }

            Log.d("MarketViewModel", "Final file for upload: ${file.absolutePath}, size: ${file.length()}")

            try {
                val appwriteFile = storage.createFile(
                    bucketId = BUCKET_ID,
                    fileId = ID.unique(),
                    file = InputFile.fromPath(file.absolutePath)
                )
                Log.d("MarketViewModel", "Image uploaded successfully. File ID: ${appwriteFile.id}")
                appwriteFile.id // Return the ID, repository will handle full URL
            } catch (e: Exception) {
                Log.e("MarketViewModel", "Appwrite storage upload failed: ${e.message}")
                throw e
            } finally {
                if (file.exists()) {
                    file.delete()
                    Log.d("MarketViewModel", "Temporary file deleted")
                }
            }
        }
    }

    fun deleteProduct(productId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                Log.d("MarketViewModel", "Initiating complete deletion for product ID: $productId")
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    // 1. Fetch document data to identify associated images before deletion
                    val document = databases.getDocument(
                        databaseId = DATABASE_ID,
                        collectionId = PRODUCTS_COLLECTION_ID,
                        documentId = productId
                    )
                    
                    val imageIds = (document.data["imageUrls"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    Log.d("MarketViewModel", "Found ${imageIds.size} images to clean up from storage")

                    // 2. Delete the Document from Database FIRST
                    databases.deleteDocument(DATABASE_ID, PRODUCTS_COLLECTION_ID, productId)
                    Log.d("MarketViewModel", "Product document deleted from database successfully")
                    
                    // 3. Background cleanup: Delete files from Storage
                    for (fileId in imageIds) {
                        try {
                            storage.deleteFile(BUCKET_ID, fileId)
                            Log.d("MarketViewModel", "Deleted image: $fileId")
                        } catch (e: Exception) {
                            Log.w("MarketViewModel", "Note: Storage file $fileId was not found or already deleted: ${e.message}")
                        }
                    }
                }
                
                // 4. Immediately refresh local cache and signal UI completion
                lastFetchTime = 0 
                refreshProducts()
                onComplete()

            } catch (e: Exception) {
                Log.e("MarketViewModel", "Complete deletion process encountered an error: ${e.message}", e)
                // Fallback attempt: Still try to delete the document if it wasn't removed yet
                try {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        databases.deleteDocument(DATABASE_ID, PRODUCTS_COLLECTION_ID, productId)
                    }
                    refreshProducts()
                    onComplete()
                } catch (innerE: Exception) {
                    Log.e("MarketViewModel", "Fallback database deletion failed: ${innerE.message}")
                }
            }
        }
    }

    fun refreshProducts() {
        viewModelScope.launch {
            Log.d("MarketViewModel", "refreshProducts() called")
            _isRefreshing.value = true
            try {
                // Small delay to allow Appwrite indexing to catch up if this was called immediately after a post
                kotlinx.coroutines.delay(1000) 
                fetchProductsSequentially()
                lastFetchTime = System.currentTimeMillis()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun applyFilters() {
        var filtered = allItems
        
        val category = _selectedCategory.value
        val subCategory = _selectedSubCategory.value
        val location = _selectedLocation.value
        val search = _searchQuery.value
        val price = _priceRange.value

        Log.d("MarketViewModel", "Applying filters - Total: ${allItems.size}, Category: $category, Sub: $subCategory, Location: $location, Price: $price")
        
        // 1. Category Filter
        if (category != "All" && category != "Favorites") {
            filtered = filtered.filter { it.category == category }
        }

        // 2. Sub-category Filter
        if (subCategory != "All") {
            filtered = filtered.filter { it.subCategory == subCategory }
        }

        // 3. Location Filter
        if (location != "All Locations") {
            filtered = filtered.filter { it.locationState == location }
        }
        
        // 4. Search Filter
        if (search.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.contains(search, ignoreCase = true) ||
                (it.description?.contains(search, ignoreCase = true) ?: false) ||
                (it.brand?.contains(search, ignoreCase = true) ?: false) ||
                (it.model?.contains(search, ignoreCase = true) ?: false)
            }
        }

        // 4. Price Filter (Local filtering for consistency and cache reliability)
        if (price != null) {
            filtered = filtered.filter { item ->
                val min = price.first ?: 0.0
                val max = price.second ?: Double.MAX_VALUE
                item.price >= min && item.price <= max
            }
        }

        // 5. Favorites Filter
        if (category == "Favorites") {
            filtered = filtered.filter { it.isFavorite }
        }
        
        Log.d("MarketViewModel", "Result count: ${filtered.size}")
        _uiState.value = MarketUiState.Success(filtered)

        // Update promoted items (Limit to 5 for the carousel)
        _promotedItems.value = allItems.filter { it.isPromoted }.take(5)
    }

    fun fetchProducts(userId: String? = null) {
        currentUserId = userId
        
        val currentTime = System.currentTimeMillis()
        val isCacheExpired = (currentTime - lastFetchTime) > CACHE_EXPIRY
        
        Log.d("MarketViewModel", "fetchProducts() called. User: $userId, Cache size: ${allItems.size}, Expired: $isCacheExpired")
        
        viewModelScope.launch {
            // Instant UI: If we already have items in memory, show them immediately
            if (allItems.isNotEmpty()) {
                applyFilters()
            } else {
                _uiState.value = MarketUiState.Loading
            }
            
            // Only fetch from network if cache is empty or expired
            if (allItems.isEmpty() || isCacheExpired) {
                fetchProductsSequentially()
                lastFetchTime = System.currentTimeMillis()
            }
        }
    }

    fun clearAllProducts(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                Log.d("MarketViewModel", "ADMIN: Initiating full marketplace wipe...")
                val response = databases.listDocuments(
                    databaseId = DATABASE_ID,
                    collectionId = PRODUCTS_COLLECTION_ID
                )
                
                for (doc in response.documents) {
                    try {
                        // Use existing logic to clean storage and database
                        deleteProduct(doc.id)
                    } catch (e: Exception) {
                        Log.e("MarketViewModel", "Failed to delete doc ${doc.id}: ${e.message}")
                    }
                }
                
                Log.d("MarketViewModel", "Full wipe complete.")
                lastFetchTime = 0
                refreshProducts()
                onComplete()
            } catch (e: Exception) {
                Log.e("MarketViewModel", "Full wipe failed: ${e.message}")
            }
        }
    }

    fun fetchSimilarProducts(category: String, currentProductId: String) {
        viewModelScope.launch {
            _similarProducts.value = repository.getSimilarProducts(category, currentProductId)
        }
    }

    fun getUserProductCount(userId: String): Int {
        return allItems.count { it.userId == userId }
    }

    fun promoteProduct(productId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Get current timestamp in ISO 8601 format
                val currentTimestamp = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    java.time.OffsetDateTime.now().toString()
                } else {
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ", java.util.Locale.US).format(java.util.Date())
                }

                databases.updateDocument(
                    databaseId = DATABASE_ID,
                    collectionId = PRODUCTS_COLLECTION_ID,
                    documentId = productId,
                    data = mapOf(
                        "isPromoted" to true,
                        "boostedAt" to currentTimestamp
                    )
                )
                refreshProducts()
                onComplete(true)
            } catch (e: Exception) {
                Log.e("MarketViewModel", "Failed to promote product: ${e.message}")
                onComplete(false)
            }
        }
    }

    private fun updateCategories(items: List<MarketItem>) {
        val distinctCategories = items.mapNotNull { it.category }.distinct().sorted()
        _categories.value = listOf("All") + distinctCategories
        Log.d("MarketViewModel", "Categories updated: ${_categories.value}")
    }
}
