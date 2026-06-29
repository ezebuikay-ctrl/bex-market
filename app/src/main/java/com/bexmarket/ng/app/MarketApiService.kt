package com.bexmarket.ng.app

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// 0. Data Model
data class MarketItem(
    val id: Int,
    val name: String,
    val price: String,
    val description: String,
    val imageUrl: String
)

// 1. Retrofit Service Interface
interface MarketApiService {
    @GET("api/products") 
    suspend fun getProducts(): List<MarketItem>
}

// 2. Retrofit Singleton Client
object RetrofitClient {
    private const val BASE_URL = "https://bexmarket-ng.vercel.app/"

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val apiService: MarketApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(MarketApiService::class.java)
    }
}
