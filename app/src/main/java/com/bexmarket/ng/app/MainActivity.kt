package com.bexmarket.ng.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.appwrite.services.Realtime
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel: MarketViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private var realtimeSubscription: Any? = null
    private lateinit var connectivityObserver: ConnectivityObserver
    private var isOffline by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            setupNotificationListener()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupAds()
        setupBottomNavigation()
        observeGatekeeper()
        checkNotificationPermission()
        setupConnectivityObserver()
        setupOfflineBanner()
        
        // Listen to theme changes
        val themePreferences = ThemePreferences(this)
        lifecycleScope.launch {
            themePreferences.isDarkMode.collect { isDark ->
                updateThemeUi(isDark)
            }
        }
    }

    private fun setupOfflineBanner() {
        findViewById<androidx.compose.ui.platform.ComposeView>(R.id.offlineBannerView).setContent {
            AppTheme {
                if (isOffline) {
                    OfflineBanner()
                }
            }
        }
    }

    private fun setupConnectivityObserver() {
        connectivityObserver = NetworkConnectivityObserver(applicationContext)
        lifecycleScope.launch {
            connectivityObserver.observe().collect { status ->
                isOffline = status != ConnectivityObserver.Status.Available
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                setupNotificationListener()
            }
        } else {
            setupNotificationListener()
        }
    }

    private fun setupNotificationListener() {
        val themePreferences = ThemePreferences(this)
        lifecycleScope.launch {
            themePreferences.notificationsEnabled.collect { enabled ->
                if (enabled) {
                    startRealtimeListener()
                } else {
                    stopRealtimeListener()
                }
            }
        }
    }

    private fun startRealtimeListener() {
        if (realtimeSubscription != null) return
        
        Log.d("Notifications", "Starting Realtime Listener for marketplace...")
        try {
            realtimeSubscription = AppwriteProvider.realtime.subscribe(
                "databases.6a4d8f75003a9cd90f61.collections.products.documents"
            ) { event ->
                // Handle new products (Create)
                if (event.events.any { it.endsWith(".create") }) {
                    val data = event.payload as? Map<*, *>
                    val productName = data?.get("productName") as? String ?: "New Product"
                    val price = data?.get("price")?.toString() ?: ""
                    
                    if (data?.get("userId") as? String != authViewModel.currentUser.value?.uid) {
                        showLocalNotification("New Item Posted!", "$productName for ₦$price")
                    }
                }
                
                // Handle changes or cleanup (Create/Update/Delete)
                if (event.events.any { it.contains(".create") || it.contains(".update") || it.contains(".delete") }) {
                    Log.d("Notifications", "Marketplace changed (${event.events.firstOrNull()}), refreshing data...")
                    viewModel.refreshProducts()
                }
            }
            Log.d("Notifications", "Realtime subscription active")
        } catch (e: Exception) {
            Log.e("Notifications", "Realtime subscription failed: ${e.message}")
        }
    }

    private fun stopRealtimeListener() {
        Log.d("Notifications", "Stopping Realtime Listener")
        try {
            realtimeSubscription?.let { sub ->
                val method = sub.javaClass.getMethod("close")
                method.invoke(sub)
            }
        } catch (e: Exception) {
            Log.e("Notifications", "Error closing realtime subscription: ${e.message}")
        }
        realtimeSubscription = null
    }

    override fun onDestroy() {
        stopRealtimeListener()
        super.onDestroy()
    }

    private fun showLocalNotification(title: String, message: String) {
        val channelId = "bex_market_notifications"
        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(channelId, "Marketplace Alerts", android.app.NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE)

        val builder = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Use app icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun updateThemeUi(isDark: Boolean) {
        val root = findViewById<View>(android.R.id.content)
        val backgroundColor = if (isDark) 0xFF121212.toInt() else 0xFFF5F5F5.toInt()
        findViewById<View>(R.id.fragmentContainer)?.setBackgroundColor(backgroundColor)
        
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        if (isDark) {
            bottomNav.setBackgroundColor(0xFF1E1E1E.toInt())
            bottomNav.itemIconTintList = android.content.res.ColorStateList.valueOf(0xFF006400.toInt())
            bottomNav.itemTextColor = android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())
        } else {
            bottomNav.setBackgroundColor(android.graphics.Color.WHITE)
            bottomNav.itemIconTintList = android.content.res.ColorStateList.valueOf(0xFF006400.toInt())
            bottomNav.itemTextColor = android.content.res.ColorStateList.valueOf(0xFF006400.toInt())
        }
    }

    private fun setupAds() {
        MobileAds.initialize(this) { status ->
            val statusMap = status.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val configuration = statusMap[adapterClass]
                Log.d("AdMob", String.format("Adapter name: %s, Description: %s, Latency: %d",
                    adapterClass, configuration?.description, configuration?.latency))
            }
        }
        RewardedAdManager.loadAd(this)
    }

    private fun observeGatekeeper() {
        val splashLayout: View = findViewById(R.id.customSplashLayout)
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authStatus.collect { authStatus ->
                    // Only wait for Auth. Marketplace products will show Shimmer in the fragment.
                    val isAuthReady = authStatus !is AuthStatus.Loading && authStatus !is AuthStatus.Idle
                    
                    if (isAuthReady) {
                        splashLayout.visibility = View.GONE
                        handleDeepLink()
                    } else {
                        splashLayout.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun handleDeepLink() {
        val uri = intent.data ?: return
        if (uri.scheme == "bexmarketnative" && uri.host == "product") {
            val productId = uri.getQueryParameter("id") ?: return
            
            // Clear data so it doesn't trigger again on rotation/re-entry
            intent.data = null 
            
            val state = viewModel.uiState.value
            if (state is MarketUiState.Success) {
                val product = state.items.find { it.id == productId }
                if (product != null) {
                    val intent = Intent(this, ProductDetailActivity::class.java).apply {
                        putExtra("PRODUCT_JSON", com.google.gson.Gson().toJson(product))
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        loadFragment(HomeFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    viewModel.onCategorySelected("All")
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_favorites -> {
                    viewModel.onCategorySelected("Favorites")
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_marketplace -> {
                    viewModel.onCategorySelected("All")
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun performLogout() {
        authViewModel.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
