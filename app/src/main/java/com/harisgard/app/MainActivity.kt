package com.harisgard.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private var pendingEnable = false
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scheduleDaily() else notifyPage(false, "اجازه اعلان داده نشد. برای فعال‌سازی باید از تنظیمات گوشی اجازه بدهی.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.settings.allowContentAccess = true
        web.settings.javaScriptCanOpenWindowsAutomatically = true
        web.webChromeClient = WebChromeClient()
        web.addJavascriptInterface(HarisBridge(this), "HarisNativeNotifications")
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val host = uri.host ?: ""
                if (host == "herisrasa.ir" || host.endsWith(".herisrasa.ir")) return false
                if (uri.scheme == "https" || uri.scheme == "http" || uri.scheme == "tel" || uri.scheme == "mailto") {
                    try { startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) {}
                    return true
                }
                return false
            }
        }
        web.loadUrl("file:///android_asset/index.html")
    }

    inner class HarisBridge(private val ctx: Context) {
        @JavascriptInterface fun enableDaily() { runOnUiThread { requestEnable() } }
        @JavascriptInterface fun disableDaily() { WorkManager.getInstance(ctx).cancelUniqueWork("haris_daily_notification"); prefs().edit().putBoolean("daily_enabled", false).apply(); notifyPage(true, "اعلان روزانه خاموش شد.") }
        @JavascriptInterface fun isEnabled(): Boolean = prefs().getBoolean("daily_enabled", false)
        @JavascriptInterface fun openNewsExternal() { runOnUiThread { try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://herisrasa.ir/"))) } catch (_: Exception) { notifyPage(false, "مرورگر مناسبی برای بازکردن سایت پیدا نشد.") } } }
    }

    private fun requestEnable() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingEnable = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else scheduleDaily()
    }

    private fun scheduleDaily() {
        val request = PeriodicWorkRequestBuilder<DailyHarisWorker>(24, TimeUnit.HOURS, 2, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("haris_daily_notification", ExistingPeriodicWorkPolicy.UPDATE, request)
        prefs().edit().putBoolean("daily_enabled", true).apply()
        notifyPage(true, "اعلان روزانه هریس‌گرد فعال شد. هر روز یک پیام دریافت می‌کنی.")
    }

    private fun prefs() = getSharedPreferences("haris_settings", Context.MODE_PRIVATE)
    private fun notifyPage(ok: Boolean, message: String) {
        val safe = message.replace("\\", "\\\\").replace("'", "\\'")
        web.evaluateJavascript("window.onNativeNotificationResult && window.onNativeNotificationResult($ok, '$safe');", null)
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
