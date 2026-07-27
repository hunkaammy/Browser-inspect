package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.NetworkRequest
import com.example.model.ResourceType
import com.example.model.WebTab
import com.example.network.DevToolsJsBridge
import java.util.UUID

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CustomWebView(
    tab: WebTab,
    onTabUpdate: (WebTab) -> Unit,
    onRequestCaptured: (NetworkRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webView = remember(context) {
        WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 DevInspect/1.0"
            }

            val bridge = DevToolsJsBridge(onRequestCaptured)
            addJavascriptInterface(bridge, DevToolsJsBridge.JS_INTERFACE_NAME)

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    onTabUpdate(
                        tab.copy(
                            loadingProgress = newProgress,
                            isLoading = newProgress < 100
                        )
                    )
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    if (!title.isNull_or_empty()) {
                        onTabUpdate(tab.copy(title = title ?: ""))
                    }
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    url?.let {
                        onTabUpdate(
                            tab.copy(
                                url = it,
                                isLoading = true,
                                canGoBack = view?.canGoBack() == true,
                                canGoForward = view?.canGoForward() == true
                            )
                        )
                    }
                    // Inject Network Interceptor script
                    view?.evaluateJavascript(DevToolsJsBridge.INTERCEPTOR_SCRIPT, null)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    url?.let {
                        onTabUpdate(
                            tab.copy(
                                url = it,
                                isLoading = false,
                                canGoBack = view?.canGoBack() == true,
                                canGoForward = view?.canGoForward() == true
                            )
                        )
                    }
                    // Re-inject script to ensure page scripts initialized after load are hooked
                    view?.evaluateJavascript(DevToolsJsBridge.INTERCEPTOR_SCRIPT, null)
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    if (request != null) {
                        val urlStr = request.url.toString()
                        val method = request.method ?: "GET"

                        // Capture non-XHR/Fetch resource requests like images, stylesheets, scripts, documents
                        val isMainFrame = request.isForMainFrame
                        val isImage = isImageResource(urlStr)
                        val isScriptOrCss = isScriptOrCssResource(urlStr)

                        if (isMainFrame || isImage || isScriptOrCss) {
                            val resourceType = when {
                                isMainFrame -> ResourceType.DOCUMENT
                                isImage -> ResourceType.IMAGE
                                isScriptOrCss -> ResourceType.JS_CSS
                                else -> ResourceType.OTHER
                            }

                            val headers = mutableMapOf<String, String>()
                            request.requestHeaders?.forEach { (k, v) -> headers[k] = v }

                            val reqLog = NetworkRequest(
                                id = UUID.randomUUID().toString(),
                                url = urlStr,
                                method = method,
                                resourceType = resourceType,
                                statusCode = 200,
                                statusText = "OK",
                                requestHeaders = headers,
                                responseHeaders = mapOf("Content-Type" to (guessMimeType(urlStr) ?: "text/html")),
                                isImage = isImage,
                                mimeType = guessMimeType(urlStr),
                                timestamp = System.currentTimeMillis()
                            )
                            onRequestCaptured(reqLog)
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }
            }
        }
    }

    // Load initial URL or update when tab URL changes externally
    LaunchedEffect(tab.url) {
        if (webView.url != tab.url && tab.url.isNotBlank()) {
            webView.loadUrl(tab.url)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )
}

private fun String?.isNull_or_empty(): Boolean = this.isNullOrBlank()

private fun isImageResource(url: String): Boolean {
    val lowercase = url.lowercase()
    return lowercase.endsWith(".png") || lowercase.endsWith(".jpg") ||
            lowercase.endsWith(".jpeg") || lowercase.endsWith(".webp") ||
            lowercase.endsWith(".svg") || lowercase.endsWith(".gif") ||
            lowercase.contains("image/")
}

private fun isScriptOrCssResource(url: String): Boolean {
    val lowercase = url.lowercase()
    return lowercase.endsWith(".js") || lowercase.endsWith(".css") ||
            lowercase.contains(".js?") || lowercase.contains(".css?")
}

private fun guessMimeType(url: String): String? {
    val lowercase = url.lowercase()
    return when {
        lowercase.endsWith(".png") -> "image/png"
        lowercase.endsWith(".jpg") || lowercase.endsWith(".jpeg") -> "image/jpeg"
        lowercase.endsWith(".webp") -> "image/webp"
        lowercase.endsWith(".svg") -> "image/svg+xml"
        lowercase.endsWith(".gif") -> "image/gif"
        lowercase.endsWith(".js") -> "application/javascript"
        lowercase.endsWith(".css") -> "text/css"
        lowercase.endsWith(".json") -> "application/json"
        else -> "text/html"
    }
}
