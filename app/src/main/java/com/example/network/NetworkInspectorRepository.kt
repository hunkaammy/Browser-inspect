package com.example.network

import com.example.model.NetworkRequest
import com.example.model.ResourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class NetworkInspectorRepository {

    private val _requests = MutableStateFlow<List<NetworkRequest>>(emptyList())
    val requests: StateFlow<List<NetworkRequest>> = _requests.asStateFlow()

    private val _isRecording = MutableStateFlow(true)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ResourceType.ALL)
    val selectedFilter: StateFlow<ResourceType> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun addRequest(request: NetworkRequest) {
        if (!_isRecording.value) return
        _requests.update { current ->
            val existing = current.find {
                it.url == request.url &&
                        it.method == request.method &&
                        Math.abs(it.timestamp - request.timestamp) < 150
            }
            if (existing != null) current else listOf(request) + current
        }
    }

    fun toggleRecording() {
        _isRecording.update { !it }
    }

    fun clearLogs() {
        _requests.value = emptyList()
    }

    fun setFilter(filter: ResourceType) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun generateCurl(request: NetworkRequest): String {
        val sb = StringBuilder()
        sb.append("curl -X ${request.method} '${request.url}'")
        request.requestHeaders.forEach { (key, value) ->
            if (key.lowercase() != "content-length") {
                sb.append(" \\\n  -H '${key}: ${value.replace("'", "\\'")}'")
            }
        }
        if (!request.requestBody.isNull_or_empty()) {
            val safeBody = request.requestBody?.replace("'", "\\'") ?: ""
            sb.append(" \\\n  --data-raw '$safeBody'")
        }
        return sb.toString()
    }

    private fun String?.isNull_or_empty(): Boolean = this.isNullOrBlank()

    suspend fun replayRequest(request: NetworkRequest): NetworkRequest {
        val startTime = System.currentTimeMillis()
        try {
            val builder = Request.Builder().url(request.url)
            
            // Set Method & Body
            if (request.method in listOf("POST", "PUT", "PATCH")) {
                val mediaType = (request.requestHeaders["Content-Type"] ?: "application/json").toMediaTypeOrNull()
                val body = (request.requestBody ?: "").toRequestBody(mediaType)
                builder.method(request.method, body)
            } else if (request.method == "HEAD") {
                builder.head()
            } else if (request.method == "DELETE") {
                if (request.requestBody != null) {
                    val mediaType = (request.requestHeaders["Content-Type"] ?: "application/json").toMediaTypeOrNull()
                    builder.delete(request.requestBody.toRequestBody(mediaType))
                } else {
                    builder.delete()
                }
            } else {
                builder.get()
            }

            // Headers
            request.requestHeaders.forEach { (k, v) ->
                if (k.lowercase() != "host" && k.lowercase() != "content-length") {
                    builder.header(k, v)
                }
            }

            val response = httpClient.newCall(builder.build()).execute()
            val duration = System.currentTimeMillis() - startTime
            val resBodyStr = response.body?.string() ?: ""

            val resHeadersMap = mutableMapOf<String, String>()
            response.headers.names().forEach { name ->
                resHeadersMap[name] = response.header(name) ?: ""
            }

            val replayed = request.copy(
                id = java.util.UUID.randomUUID().toString(),
                statusCode = response.code,
                statusText = response.message.ifBlank { if (response.isSuccessful) "OK" else "Error" },
                responseHeaders = resHeadersMap,
                responseBody = resBodyStr,
                durationMs = duration,
                contentLength = resBodyStr.length.toLong(),
                timestamp = System.currentTimeMillis()
            )

            addRequest(replayed)
            return replayed
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val failed = request.copy(
                id = java.util.UUID.randomUUID().toString(),
                statusCode = 0,
                statusText = "Replay Exception",
                responseBody = "Failed to replay request: ${e.localizedMessage}",
                durationMs = duration,
                timestamp = System.currentTimeMillis()
            )
            addRequest(failed)
            return failed
        }
    }
}
