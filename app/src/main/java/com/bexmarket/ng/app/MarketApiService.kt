package com.bexmarket.ng.app

import com.google.gson.GsonBuilder
import okhttp3.ConnectionSpec
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*

// 1. Retrofit Service Interface
interface MarketApiService {
    @Headers(
        "Accept: application/json",
        "Cache-Control: no-cache",
        "Pragma: no-cache",
        "X-Requested-With: XMLHttpRequest"
    )
    @GET("api/products")
    suspend fun getProducts(): Response<ResponseBody>

    @POST("api/register")
    suspend fun registerUser(@Body registrationData: Map<String, String>): Response<ResponseBody>

    @POST("api/login")
    suspend fun loginUser(@Body loginData: Map<String, String>): Response<ResponseBody>

    @Multipart
    @POST("api/products")
    suspend fun postProduct(
        @Part("title") title: RequestBody,
        @Part("price") price: RequestBody,
        @Part("description") description: RequestBody,
        @Part("category") category: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<ResponseBody>
}

// 2. Retrofit Singleton Client
object RetrofitClient {
    private const val BASE_URL = "https://bexmarketplace-ng.vercel.app/"

    val gson = GsonBuilder()
        .setLenient()
        .create()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
        .build()

    val apiService: MarketApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(MarketApiService::class.java)
    }
}
