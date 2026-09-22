package com.bexmarket.ng.app

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.delay

@Composable
fun rememberNativeAd(adUnitId: String, refreshTrigger: Int = 0): Pair<NativeAd?, String?> {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var errorInfo by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(adUnitId, retryTrigger, refreshTrigger) {
        if (retryTrigger > 0) delay(5000)
        errorInfo = null // Reset error info to show shimmer during retry
        
        android.util.Log.d("AdMob", "Loading Native Ad (Attempt ${retryTrigger + 1}, Refresh $refreshTrigger): $adUnitId")
        
        // Explicitly configure NativeAdOptions
        val adOptions = NativeAdOptions.Builder()
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
            .setRequestMultipleImages(false)
            .setReturnUrlsForImageAssets(false)
            .build()

        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                android.util.Log.d("AdMob", "Ad loaded successfully")
                nativeAd = ad
                errorInfo = null
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    // Log detailed error for debugging
                    val msg = "Ad Error: ${error.code} - ${error.message}. Domain: ${error.domain}"
                    android.util.Log.e("AdMob", msg)
                    errorInfo = msg
                    
                    // If it's a NO_FILL (3), we retry. 
                    // If it's a format mismatch, retrying won't help but won't hurt much.
                    if (retryTrigger < 2) retryTrigger++
                }
            })
            .withNativeAdOptions(adOptions)
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    DisposableEffect(adUnitId) {
        onDispose { nativeAd?.destroy() }
    }

    return nativeAd to errorInfo
}

@Composable
fun NativeAdGridItem(
    nativeAd: NativeAd,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = if (isDark) Color.White else Color(0xFF1A1A1A)
    val bodyColor = if (isDark) Color.LightGray else Color(0xFF666666)

    Card(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
            .height(260.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val adView = LayoutInflater.from(context).inflate(R.layout.ad_unified_grid, null) as NativeAdView
                populateNativeAdView(nativeAd, adView, textColor, bodyColor)
                adView
            },
            update = { adView ->
                populateNativeAdView(nativeAd, adView, textColor, bodyColor)
            }
        )
    }
}

private fun populateNativeAdView(
    nativeAd: NativeAd, 
    adView: NativeAdView, 
    textColor: Color, 
    bodyColor: Color
) {
    adView.mediaView = adView.findViewById(R.id.ad_media)
    adView.headlineView = adView.findViewById(R.id.ad_headline)
    adView.bodyView = adView.findViewById(R.id.ad_body)
    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
    adView.iconView = adView.findViewById(R.id.ad_app_icon)

    (adView.headlineView as? TextView)?.apply {
        text = nativeAd.headline
        setTextColor(android.graphics.Color.argb(
            (textColor.alpha * 255).toInt(),
            (textColor.red * 255).toInt(),
            (textColor.green * 255).toInt(),
            (textColor.blue * 255).toInt()
        ))
    }

    nativeAd.mediaContent?.let {
        adView.mediaView?.setMediaContent(it)
    }

    if (nativeAd.body == null) {
        adView.bodyView?.visibility = View.INVISIBLE
    } else {
        adView.bodyView?.visibility = View.VISIBLE
        (adView.bodyView as? TextView)?.apply {
            text = nativeAd.body
            setTextColor(android.graphics.Color.argb(
                (bodyColor.alpha * 255).toInt(),
                (bodyColor.red * 255).toInt(),
                (bodyColor.green * 255).toInt(),
                (bodyColor.blue * 255).toInt()
            ))
        }
    }

    if (nativeAd.callToAction == null) {
        adView.callToActionView?.visibility = View.INVISIBLE
    } else {
        adView.callToActionView?.visibility = View.VISIBLE
        (adView.callToActionView as? Button)?.text = nativeAd.callToAction
    }

    if (nativeAd.icon == null) {
        adView.iconView?.visibility = View.GONE
    } else {
        (adView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
        adView.iconView?.visibility = View.VISIBLE
    }

    adView.setNativeAd(nativeAd)
}
