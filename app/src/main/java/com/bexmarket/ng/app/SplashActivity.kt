package com.bexmarket.ng.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Anti-tamper check
        if (!SecurityUtils.verifySignature(this)) {
            Log.e("Security", "Tamper detected! Signature mismatch.")
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        findViewById<android.view.View>(R.id.customSplashLayout).visibility = android.view.View.VISIBLE

        // Safety backup: If app is stuck on splash for more than 10 seconds, force start
        lifecycleScope.launch {
            kotlinx.coroutines.delay(10000)
            if (!isFinishing) {
                Log.w("SplashActivity", "Splash taking too long, force starting MainActivity...")
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }
        }

        checkVersionAndObserveAuth()
    }

    private fun checkVersionAndObserveAuth() {
        lifecycleScope.launch {
            try {
                // Add a timeout of 3 seconds so the app doesn't hang if network is slow
                val versionCheckJob = kotlinx.coroutines.withTimeoutOrNull(3000) {
                    AppwriteProvider.repository.getMinimumRequiredVersion()
                }
                
                val minVersion = versionCheckJob ?: 0
                val currentVersion = packageManager.getPackageInfo(packageName, 0).versionCode
                
                Log.d("SplashActivity", "Version Check - Current: $currentVersion, Min Required: $minVersion")
                
                if (minVersion > 0 && currentVersion < minVersion) {
                    showUpdateDialog()
                } else {
                    observeAuth()
                }
            } catch (e: Exception) {
                Log.e("SplashActivity", "Version check failed/timed out, proceeding: ${e.message}")
                observeAuth()
            }
        }
    }

    private fun showUpdateDialog() {
        AlertDialog.Builder(this)
            .setTitle("Update Required")
            .setMessage("A newer version of BEXMARKETPLACE-NG is available. Please update to continue using the app.")
            .setPositiveButton("Update Now") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun observeAuth() {
        val themePreferences = ThemePreferences(this)
        lifecycleScope.launch {
            // Check Onboarding status first
            val isComplete = themePreferences.onboardingComplete.first()
            
            // Wait until the initial session check is finished (Loading -> Authenticated/Unauthenticated)
            authViewModel.authStatus
                .filter { it !is AuthStatus.Loading && it !is AuthStatus.Idle }
                .collect { status ->
                    if (!isComplete) {
                        startActivity(Intent(this@SplashActivity, OnboardingActivity::class.java))
                    } else {
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    }
                    finish()
                }
        }
    }
}
