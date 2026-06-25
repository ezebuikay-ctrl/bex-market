package com.bexmarket.ng.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.MobileAds

class MainActivity : AppCompatActivity() {

    private var uploadMessage: ValueCallback<Array<Uri>>? = null
    private val FILECHOOSER_RESULTCODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Google Mobile Ads SDK
        MobileAds.initialize(this) {}

        val myWebView: WebView = findViewById(R.id.webview)
        val splashLayout: RelativeLayout = findViewById(R.id.customSplashLayout)

        val settings = myWebView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true

        // Append custom string to User Agent to identify the app
        val defaultUserAgent = settings.userAgentString
        settings.userAgentString = "$defaultUserAgent BexMarketApp"

        myWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Ensure splash screen is hidden when the page finishes loading
                splashLayout.visibility = View.GONE
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()

                // 1. Handle WhatsApp links
                if (url.startsWith("whatsapp://")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        view?.context?.startActivity(intent)
                        return true
                    } catch (_: Exception) {
                        // WhatsApp not installed or error handling
                        return false
                    }
                }

                // 2. Traffic Controller: Detect ad-redirects and special schemes
                if (url.startsWith("intent://") || url.startsWith("market://")) {
                    try {
                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        view?.context?.startActivity(intent)
                        return true // Successfully handled, do NOT load in WebView
                    } catch (_: Exception) {
                        return false // If the intent fails, fall back to default
                    }
                }

                // 3. Whitelist: Only load our domain inside the WebView
                if (url.contains("bexmarket-ng.vercel.app")) {
                    return false // Load in WebView
                }

                // 4. External Links/Ads: Open in system browser
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    view?.context?.startActivity(intent)
                    return true
                } catch (_: Exception) {
                    return false
                }
            }
        }

        myWebView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                if (uploadMessage != null) {
                    uploadMessage?.onReceiveValue(null)
                    uploadMessage = null
                }

                uploadMessage = filePathCallback

                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*"

                try {
                    startActivityForResult(
                        Intent.createChooser(intent, "File Chooser"),
                        FILECHOOSER_RESULTCODE
                    )
                } catch (e: Exception) {
                    uploadMessage = null
                    return false
                }

                return true
            }
        }

        // Load the website at the end of onCreate
        if (savedInstanceState == null) {
            myWebView.loadUrl("https://bexmarket-ng.vercel.app")
        } else {
            myWebView.restoreState(savedInstanceState)
        }

        // Handle back button behavior for WebView
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (myWebView.canGoBack()) {
                    myWebView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (uploadMessage == null) return
            uploadMessage?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
            uploadMessage = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val myWebView: WebView? = findViewById(R.id.webview)
        myWebView?.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val myWebView: WebView? = findViewById(R.id.webview)
        myWebView?.restoreState(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        val myWebView: WebView? = findViewById(R.id.webview)
        
        if (myWebView == null) {
            recreate()
            return
        }

        myWebView.let {
            it.onResume()

            val currentUrl = it.url
            if (currentUrl == null || currentUrl.isEmpty() || currentUrl == "about:blank") {
                if (it.canGoBack()) {
                    it.reload()
                } else {
                    it.loadUrl("https://bexmarket-ng.vercel.app")
                }
            }
        }
    }
}
