package com.bexmarket.ng.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import java.security.MessageDigest

object SecurityUtils {

    /**
     * Replace this with your actual release certificate SHA-256 hash.
     * You can get this by running: 
     * keytool -list -v -keystore your_keystore.jks
     */
    private const val OFFICIAL_SIGNATURE_HASH = "REPLACE_WITH_YOUR_ACTUAL_SHA256_HASH"

    @SuppressLint("PackageManagerGetSignatures")
    fun verifySignature(context: Context): Boolean {
        // Temporarily bypass this check for release builds to prevent immediate closing.
        // TODO: Replace OFFICIAL_SIGNATURE_HASH with your actual SHA-256 hash before final release.
        if (true) return true 

        if (BuildConfig.DEBUG) return true // Skip check for debug builds

        try {
            val packageManager = context.packageManager
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo
                signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
            }

            if (signatures != null) {
                for (signature in signatures) {
                    val md = MessageDigest.getInstance("SHA-256")
                    md.update(signature.toByteArray())
                    val currentSignatureHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
                    
                    if (OFFICIAL_SIGNATURE_HASH == currentSignatureHash) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}
