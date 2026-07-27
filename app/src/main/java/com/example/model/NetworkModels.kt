package com.example.model

enum class ResourceType(val displayName: String) {
    ALL("All"),
    XHR_FETCH("Fetch/XHR"),
    IMAGE("Images"),
    DOCUMENT("Doc"),
    JS_CSS("JS/CSS"),
    MEDIA("Media"),
    OTHER("Other")
}

data class NetworkRequest(
    val id: String,
    val url: String,
    val method: String,
    val resourceType: ResourceType,
    val statusCode: Int = 200,
    val statusText: String = "OK",
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String? = null,
    val responseHeaders: Map<String, String> = emptyMap(),
    val responseBody: String? = null,
    val isImage: Boolean = false,
    val mimeType: String? = null,
    val durationMs: Long = 0L,
    val contentLength: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
) {
    val host: String
        get() = try {
            java.net.URI(url).host ?: url
        } catch (e: Exception) {
            url
        }

    val pathWithQuery: String
        get() = try {
            val uri = java.net.URI(url)
            val path = uri.path.ifEmpty { "/" }
            if (uri.query != null) "$path?${uri.query}" else path
        } catch (e: Exception) {
            url
        }

    val isSuccess: Boolean
        get() = statusCode in 200..299

    val isError: Boolean
        get() = statusCode >= 400
}

data class WebTab(
    val id: String,
    val title: String = "New Tab",
    val url: String = "https://jsonplaceholder.typicode.com/posts/1",
    val faviconUrl: String? = null,
    val loadingProgress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val sslSecure: Boolean = url.startsWith("https://")
)

data class Bookmark(
    val title: String,
    val url: String,
    val category: String,
    val description: String
)
