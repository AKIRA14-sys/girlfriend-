package com.mika.app.avatar

import android.content.Context
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import java.io.File

class AvatarController(
    private val webView: WebView,
    private val onModelLoaded: (triangleCount: Int) -> Unit = {},
    private val onModelError: (error: String) -> Unit = {},
    private val onOpenSettings: () -> Unit = {}
) {

    private var isPageLoaded = false
    private var pendingVrmFileName: String? = null

    init {
        setupWebView()
    }

    private fun setupWebView() {
        val context = webView.context
        val avatarDir = File(context.filesDir, "avatar").apply { if (!exists()) mkdirs() }

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .addPathHandler("/avatar-files/", WebViewAssetLoader.InternalStoragePathHandler(context, avatarDir))
            .build()

        webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            domStorageEnabled = true
        }

        webView.addJavascriptInterface(JSInterface(), "AndroidBridge")

        webView.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isPageLoaded = true
                pendingVrmFileName?.let {
                    loadModel(it)
                    pendingVrmFileName = null
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                onModelError("WebView error: $description")
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    if (it.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                        Log.e("AvatarWebView", "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
                        onModelError(it.message())
                    }
                }
                return true
            }
        }

        webView.loadUrl("https://appassets.androidplatform.net/assets/avatar/index.html")
    }

    fun loadModel(vrmFileName: String) {
        if (!isPageLoaded) {
            pendingVrmFileName = vrmFileName
            return
        }
        val timestamp = System.currentTimeMillis()
        val url = "https://appassets.androidplatform.net/avatar-files/$vrmFileName?v=$timestamp"
        runJs("loadModel('$url')")
    }

    fun setExpression(name: String) {
        runJs("setExpression('$name')")
    }

    fun playGesture(name: String) {
        runJs("playGesture('$name')")
    }

    fun setCamera(preset: String) {
        runJs("setCamera('$preset')")
    }

    fun startSimulatedSpeech() {
        runJs("startSimulatedSpeech()")
    }

    fun stopSpeech() {
        runJs("stopSpeech()")
    }

    fun speakAudio(base64Audio: String) {
        runJs("speakAudio('$base64Audio')")
    }

    private fun runJs(js: String) {
        webView.post {
            webView.evaluateJavascript(js, null)
        }
    }

    inner class JSInterface {
        @JavascriptInterface
        fun modelLoaded(triangleCount: Int) {
            Log.d("AvatarController", "Model loaded with $triangleCount triangles")
            onModelLoaded(triangleCount)
        }

        @JavascriptInterface
        fun modelError(error: String) {
            Log.e("AvatarController", "Model error: $error")
            onModelError(error)
        }

        @JavascriptInterface
        fun openSettings() {
            onOpenSettings()
        }
    }
}
