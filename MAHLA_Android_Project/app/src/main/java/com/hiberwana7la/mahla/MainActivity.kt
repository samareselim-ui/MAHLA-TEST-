package com.hiberwana7la.mahla

import android.annotation.SuppressLint
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private val appUrl = "https://appassets.androidplatform.net/assets/index.html"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
        }
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: android.webkit.WebResourceRequest
            ): android.webkit.WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)
        }
        webView.webChromeClient = WebChromeClient()
        loadAppWithAuthCallback(intent)
        checkForAppUpdate()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadAppWithAuthCallback(intent)
    }

    private fun loadAppWithAuthCallback(source: android.content.Intent?) {
        val uri = source?.data
        val isMahlaCallback = uri?.scheme == "mahla" && uri.host == "auth-callback"
        val fragment = if (isMahlaCallback) uri?.fragment else null
        val target = if (!fragment.isNullOrBlank()) "$appUrl#$fragment" else appUrl
        webView.loadUrl(target)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }


    /**
     * Checks the edition-specific update manifest hosted in the MAHLA GitHub repository.
     * This only notifies and opens the APK download page; Android still asks the user
     * to approve installation. No Supabase/auth/license code is changed here.
     */
    private fun checkForAppUpdate() {
        val manifestUrl = if (packageName.contains(".owner")) {
            "https://raw.githubusercontent.com/samareselim-ui/MAHLA-TEST-/main/updates/owner.json"
        } else {
            "https://raw.githubusercontent.com/samareselim-ui/MAHLA-TEST-/main/updates/client.json"
        }

        Executors.newSingleThreadExecutor().execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(manifestUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("Cache-Control", "no-cache")
                }
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@execute

                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val manifest = JSONObject(body)
                val latestCode = manifest.optInt("versionCode", 0)
                val latestName = manifest.optString("versionName", "")
                val apkUrl = manifest.optString("apkUrl", "")
                val notes = manifest.optString("releaseNotes", "تحسينات وإصلاحات.")

                val currentCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, 0).versionCode
                }

                if (latestCode > currentCode && apkUrl.startsWith("https://")) {
                    runOnUiThread {
                        val prefs = getSharedPreferences("mahla_updates", MODE_PRIVATE)
                        if (prefs.getInt("dismissed_version", 0) >= latestCode) return@runOnUiThread

                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("تحديث جديد لتطبيق مهلة")
                            .setMessage("الإصدار الجديد: $latestName\n\n$notes\n\nيمكنك تنزيل التحديث الآن. سيطلب أندرويد تأكيد التثبيت.")
                            .setPositiveButton("تنزيل التحديث") { _, _ ->
                                try {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)))
                                } catch (e: Exception) {
                                    Log.e("MAHLA_UPDATE", "Cannot open update URL", e)
                                }
                            }
                            .setNegativeButton("لاحقًا") { _, _ ->
                                prefs.edit().putInt("dismissed_version", latestCode).apply()
                            }
                            .show()
                    }
                }
            } catch (e: Exception) {
                Log.w("MAHLA_UPDATE", "Update check skipped: ${e.message}")
            } finally {
                connection?.disconnect()
            }
        }
    }

}
