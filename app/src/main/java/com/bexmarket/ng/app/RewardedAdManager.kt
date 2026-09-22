package com.bexmarket.ng.app

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object RewardedAdManager {
    private var rewardedAd: RewardedAd? = null
    var isAdLoading = false
        private set
    
    var lastLoadError: LoadAdError? = null
        private set

    private const val TAG = "RewardedAdManager"

    // Ad Unit IDs
    private const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    private const val PROD_AD_UNIT_ID = "ca-app-pub-9972987433387087/4673605078"

    private val AD_UNIT_ID = if (BuildConfig.DEBUG) TEST_AD_UNIT_ID else PROD_AD_UNIT_ID

    fun loadAd(activity: Activity) {
        if (rewardedAd != null || isAdLoading) return

        isAdLoading = true
        lastLoadError = null
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(activity, AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Ad failed to load: ${adError.code} - ${adError.message}. Domain: ${adError.domain}")
                rewardedAd = null
                isAdLoading = false
                lastLoadError = adError
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Ad was loaded successfully. Response Info: ${ad.responseInfo}")
                rewardedAd = ad
                isAdLoading = false
                lastLoadError = null
            }
        })
    }

    fun showAd(activity: Activity, onRewardEarned: () -> Unit, onAdClosed: (Boolean) -> Unit) {
        if (rewardedAd != null) {
            var rewardEarned = false
            rewardedAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed.")
                    rewardedAd = null
                    onAdClosed(rewardEarned)
                    loadAd(activity) // Preload next one
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    Log.d(TAG, "Ad failed to show: ${adError.message}")
                    rewardedAd = null
                    onAdClosed(false)
                    loadAd(activity)
                }
            }
            
            rewardedAd?.show(activity, OnUserEarnedRewardListener { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                rewardEarned = true
                onRewardEarned()
            })
        } else {
            Log.d(TAG, "Ad not ready yet.")
            onAdClosed(false)
            loadAd(activity)
        }
    }

    /**
     * Shows a sequence of rewarded ads back-to-back.
     */
    fun showAdSequence(
        activity: Activity,
        totalAds: Int,
        onComplete: () -> Unit,
        onCancel: (Int) -> Unit,
        onProgress: (Int) -> Unit = {}
    ) {
        var adsShown = 0

        fun showNext() {
            if (adsShown >= totalAds) {
                onComplete()
                return
            }

            if (!isAdReady()) {
                Log.d(TAG, "Ad not ready for sequence index $adsShown, waiting and loading...")
                loadAd(activity)
                // In a real app, you might want a small delay or a retry mechanism here
                // For now, we tell the user it's loading via onCancel or a toast elsewhere
                onCancel(adsShown)
                return
            }

            onProgress(adsShown + 1)
            showAd(activity, 
                onRewardEarned = {
                    adsShown++
                },
                onAdClosed = { rewardEarned ->
                    if (rewardEarned) {
                        // Small delay before next ad for better UX
                        activity.window.decorView.postDelayed({
                            showNext()
                        }, 500)
                    } else {
                        onCancel(adsShown)
                    }
                }
            )
        }

        showNext()
    }

    fun isAdReady(): Boolean = rewardedAd != null
}
