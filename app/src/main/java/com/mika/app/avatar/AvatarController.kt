package com.mika.app.avatar

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

class AvatarController(
    private val webView: WebView,
    private val onModelLoaded: (triangleCount: Int) -> Unit = {},
    private val onModelError: (error: String) -> Unit = {},
    private val onOpenSettings: () -> Unit = {}
) {

    init {
        setupWebView()
    }

    private fun setupWebView() {
        val context = webView.context
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .addPathHandler("/files/", WebViewAssetLoader.InternalStoragePathHandler(context, context.filesDir))
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
        }

        webView.loadUrl("https://appassets.androidplatform.net/assets/avatar/index.html")
    }

    fun loadModel(vrmFileName: String) {
        val url = "https://appassets.androidplatform.net/files/$vrmFileName"
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
