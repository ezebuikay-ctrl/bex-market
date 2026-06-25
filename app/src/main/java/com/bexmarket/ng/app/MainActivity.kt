package com.bexmarket.ng.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var uploadMessage: ValueCallback<Array<Uri>>? = null
    private val FILECHOOSER_RESULTCODE = 1
    private val PERMISSION_REQUEST_CODE = 100
    private var pendingFileChooserParams: WebChromeClient.FileChooserParams? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myWebView: WebView = findViewById(R.id.webview)
        val splashLayout: RelativeLayout = findViewById(R.id.customSplashLayout)

        val settings = myWebView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false

        // Append custom string to User Agent to identify the app
        val defaultUserAgent = settings.userAgentString
        settings.userAgentString = "$defaultUserAgent BexMarketApp"

        myWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Ensure splash screen is hidden when the page finishes loading
                splashLayout.visibility = View.GONE

                // Inject CSS to fix layout shifting and ensure stationary grid
                val css = "html, body { width: 100% !important; max-width: 100vw !important; margin: 0 !important; padding: 0 !important; overflow-x: hidden !important; overflow-y: auto !important; -webkit-overflow-scrolling: touch !important; } " +
                        "#page-wrapper, .page-wrapper { overflow-x: hidden !important; width: 100% !important; } " +
                        ".main-grid-container { width: 100vw !important; min-width: 100vw !important; pointer-events: auto !important; } " +
                        ".side-menu, .navbar { position: fixed !important; pointer-events: none !important; z-index: 1000 !important; } " +
                        ".side-menu *, .navbar *, .post-product-button { pointer-events: auto !important; } " +
                        ".post-product-button { position: fixed !important; z-index: 1002 !important; }"
                
                val js = "var style = document.createElement('style'); style.innerHTML = '$css'; document.head.appendChild(style);"
                view?.evaluateJavascript(js, null)
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
                pendingFileChooserParams = fileChooserParams

                if (checkStoragePermission()) {
                    openFileChooser()
                } else {
                    requestStoragePermission()
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
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
    }

    private fun openFileChooser() {
        val intent = pendingFileChooserParams?.createIntent()
        try {
            startActivityForResult(intent!!, FILECHOOSER_RESULTCODE)
        } catch (e: Exception) {
            uploadMessage?.onReceiveValue(null)
            uploadMessage = null
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFileChooser()
            } else {
                uploadMessage?.onReceiveValue(null)
                uploadMessage = null
            }
        }
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
