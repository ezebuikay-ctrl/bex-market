package com.bexmarket.ng.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

/**
 * Helper class that manages App Open Ads.
 */
class AppOpenAdManager(private val myApplication: Application) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false

    /** Keep track of the time an ad was loaded to ensure it's not expired. */
    private var loadTime: Long = 0

    private var currentActivity: Activity? = null

    private val AD_UNIT_ID = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/9257395912" else "ca-app-pub-9972987433387087/2984102941"

    init {
        myApplication.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /** Request an ad. */
    fun fetchAd() {
        if (isAdAvailable()) return

        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            myApplication,
            AD_UNIT_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    Log.d("AppOpenAdManager", "onAdLoaded.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    Log.e("AppOpenAdManager", "onAdFailedToLoad: ${loadAdError.message}")
                }
            }
        )
    }

    /** Shows the ad if one isn't already showing. */
    fun showAdIfAvailable(activity: Activity) {
        showAdIfAvailable(activity, object : OnShowAdCompleteListener {
            override fun onShowAdComplete() {
                // Empty
            }
        })
    }

    /** Shows the ad if one isn't already showing. */
    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
        // If the app open ad is already showing, do not show the ad again.
        if (isShowingAd) {
            Log.d("AppOpenAdManager", "The app open ad is already showing.")
            return
        }

        // If the app open ad is not available yet, invoke the callback then load the ad.
        if (!isAdAvailable()) {
            Log.d("AppOpenAdManager", "The app open ad is not ready yet.")
            onShowAdCompleteListener.onShowAdComplete()
            fetchAd()
            return
        }

        Log.d("AppOpenAdManager", "Will show ad.")

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                // Set the ad reference to null so isAdAvailable() returns false.
                appOpenAd = null
                isShowingAd = false
                Log.d("AppOpenAdManager", "onAdDismissedFullScreenContent.")
                onShowAdCompleteListener.onShowAdComplete()
                fetchAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                Log.e("AppOpenAdManager", "onAdFailedToShowFullScreenContent: ${adError.message}")
                onShowAdCompleteListener.onShowAdComplete()
                fetchAd()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                Log.d("AppOpenAdManager", "onAdShowedFullScreenContent.")
            }
        }
        appOpenAd?.show(activity)
    }

    /** Check if ad exists and can be shown. */
    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        currentActivity?.let { showAdIfAvailable(it) }
    }

    /** ActivityLifecycleCallbacks methods. */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) { currentActivity = activity }
    override fun onActivityResumed(activity: Activity) { currentActivity = activity }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) { currentActivity = null }

    /** Interface definition for a callback to be invoked when an app open ad is complete. */
    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }
}
