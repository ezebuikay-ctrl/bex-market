package com.bexmarket.ng.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory

import coil.imageLoader

class BexMarketApp : Application(), ImageLoaderFactory {
    private lateinit var appOpenAdManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()
        AppwriteProvider.init(this)
        
        appOpenAdManager = AppOpenAdManager(this)
        
        // If you want to clear the cache once on every app launch for testing:
        // clearCoilCache()
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearCoilCache() {
        val loader = this.imageLoader
        loader.diskCache?.clear()
        loader.memoryCache?.clear()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient {
                RetrofitClient.okHttpClient
            }
            .build()
    }
}
