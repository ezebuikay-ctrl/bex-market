package com.bexmarket.ng.app

import com.google.gson.annotations.SerializedName

data class MarketItem(
    val id: String,
    @SerializedName("productName") val name: String,
    @SerializedName("price") val price: Double,
    @SerializedName("description") val description: String?,
    @SerializedName("category") val category: String? = "General",
    @SerializedName("stockQuantity") val stockQuantity: Int? = 0,
    @SerializedName("sku") val sku: String? = "",
    @SerializedName("dimensions") val dimensions: String? = "",
    @SerializedName("imageUrl") val imageUrl: String?, // Primary image
    @SerializedName("imageUrls") val imageUrls: List<String> = emptyList(), // List of up to 7 image IDs/URLs
    @SerializedName("sellerName") val sellerName: String? = "Bex Seller",
    @SerializedName("sellerPhone") val sellerPhone: String? = "+2340000000000",
    @SerializedName("sellerWhatsapp") val sellerWhatsapp: String? = "+2340000000000",
    @SerializedName("sellerEmail") val sellerEmail: String? = "support@bexmarket.ng",
    @SerializedName("sellerProfilePic") val sellerProfilePic: String? = null,
    @SerializedName("locationState") val locationState: String? = "Lagos",
    @SerializedName("locationAxis") val locationAxis: String? = "Mainland",
    @SerializedName("subCategory") val subCategory: String? = null,
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("storage") val storage: String? = null,
    @SerializedName("batteryHealth") val batteryHealth: String? = null,
    @SerializedName("processor") val processor: String? = null,
    @SerializedName("ram") val ram: String? = null,
    @SerializedName("ssd") val ssd: String? = null,
    @SerializedName("isVerified") val isVerified: Boolean = false,
    @SerializedName("isOnline") val isOnline: Boolean = false,
    @SerializedName("isPromoted") val isPromoted: Boolean = false,
    @SerializedName("boostedAt") val boostedAt: String? = null,
    @SerializedName("userId") val userId: String = "", // Added for security and management
    var isFavorite: Boolean = false
)
