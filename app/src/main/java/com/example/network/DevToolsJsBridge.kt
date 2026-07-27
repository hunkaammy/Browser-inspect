package com.example.network

import android.webkit.JavascriptInterface
import com.example.model.NetworkRequest
import com.example.model.ResourceType
import org.json.JSONObject
import java.util.UUID

class DevToolsJsBridge(
    private val onRequestCaptured: (NetworkRequest) -> Unit
) {

    @JavascriptInterface
    fun onNetworkIntercepted(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val url = json.optString("url", "")
            if (url.isBlank() || url.startsWith("data:") || url.startsWith("blob:")) return

            val method = json.optString("method", "GET").uppercase()
            val status = json.optInt("status", 200)
            val statusText = json.optString("statusText", "OK")
            val requestBody = json.optString("requestBody", null)?.takeIf { it.isNotBlank() && it != "null" }
            val responseBody = json.optString("responseBody", null)?.takeIf { it.isNotBlank() && it != "null" }
            val durationMs = json.optLong("durationMs", 0L)
            val mimeType = json.optString("mimeType", "")

            val reqHeaders = parseHeadersJson(json.optJSONObject("requestHeaders"))
            val resHeaders = parseHeadersJson(json.optJSONObject("responseHeaders"))

            val isImage = mimeType.contains("image", ignoreCase = true) ||
                    url.endsWith(".png", true) || url.endsWith(".jpg", true) ||
                    url.endsWith(".jpeg", true) || url.endsWith(".webp", true) ||
                    url.endsWith(".svg", true) || url.endsWith(".gif", true)

            val resourceType = when {
                isImage -> ResourceType.IMAGE
                mimeType.contains("json", true) -> ResourceType.XHR_FETCH
                mimeType.contains("javascript", true) || url.endsWith(".js", true) -> ResourceType.JS_CSS
                mimeType.contains("css", true) || url.endsWith(".css", true) -> ResourceType.JS_CSS
                mimeType.contains("html", true) -> ResourceType.DOCUMENT
                else -> ResourceType.XHR_FETCH
            }

            val request = NetworkRequest(
                id = UUID.randomUUID().toString(),
                url = url,
                method = method,
                resourceType = resourceType,
                statusCode = status,
                statusText = statusText,
                requestHeaders = reqHeaders,
                requestBody = requestBody,
                responseHeaders = resHeaders,
                responseBody = responseBody,
                isImage = isImage,
                mimeType = mimeType,
                durationMs = durationMs,
                contentLength = (responseBody?.length ?: 0).toLong(),
                timestamp = System.currentTimeMillis()
            )

            onRequestCaptured(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseHeadersJson(jsonObj: JSONObject?): Map<String, String> {
        if (jsonObj == null) return emptyMap()
        val map = mutableMapOf<String, String>()
        val keys = jsonObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = jsonObj.optString(key, "")
        }
        return map
    }

    companion object {
        const val JS_INTERFACE_NAME = "AndroidDevTools"

        val INTERCEPTOR_SCRIPT = """
            (function() {
                if (window.__devToolsInjected) return;
                window.__devToolsInjected = true;

                function sendToAndroid(data) {
                    if (window.AndroidDevTools && window.AndroidDevTools.onNetworkIntercepted) {
                        try {
                            window.AndroidDevTools.onNetworkIntercepted(JSON.stringify(data));
                        } catch(e) {}
                    }
                }

                // Hook window.fetch
                const originalFetch = window.fetch;
                if (originalFetch) {
                    window.fetch = async function(...args) {
                        const startTime = performance.now();
                        let url = "";
                        let method = "GET";
                        let reqHeaders = {};
                        let reqBody = null;

                        try {
                            if (typeof args[0] === "string") {
                                url = args[0];
                            } else if (args[0] && args[0].url) {
                                url = args[0].url;
                                method = args[0].method || "GET";
                            }
                            if (args[1]) {
                                method = args[1].method || method;
                                if (args[1].headers) {
                                    if (args[1].headers instanceof Headers) {
                                        args[1].headers.forEach((v, k) => { reqHeaders[k] = v; });
                                    } else if (typeof args[1].headers === 'object') {
                                        reqHeaders = args[1].headers;
                                    }
                                }
                                if (args[1].body) {
                                    reqBody = typeof args[1].body === 'string' ? args[1].body : '[Binary/Object Body]';
                                }
                            }
                        } catch(e) {}

                        try {
                            const response = await originalFetch.apply(this, args);
                            const durationMs = Math.round(performance.now() - startTime);
                            const clone = response.clone();
                            
                            const resHeaders = {};
                            try {
                                clone.headers.forEach((v, k) => { resHeaders[k] = v; });
                            } catch(e) {}

                            let resBody = null;
                            const contentType = clone.headers.get("content-type") || "";
                            try {
                                if (contentType.includes("json") || contentType.includes("text") || contentType.includes("javascript")) {
                                    resBody = await clone.text();
                                } else {
                                    resBody = "[Binary or Media Data]";
                                }
                            } catch(e) {
                                resBody = "[Unable to read response text]";
                            }

                            sendToAndroid({
                                url: url,
                                method: method,
                                status: response.status,
                                statusText: response.statusText,
                                requestHeaders: reqHeaders,
                                requestBody: reqBody,
                                responseHeaders: resHeaders,
                                responseBody: resBody,
                                durationMs: durationMs,
                                mimeType: contentType
                            });

                            return response;
                        } catch(error) {
                            const durationMs = Math.round(performance.now() - startTime);
                            sendToAndroid({
                                url: url,
                                method: method,
                                status: 0,
                                statusText: error.message || "Failed / Blocked",
                                requestHeaders: reqHeaders,
                                requestBody: reqBody,
                                responseHeaders: {},
                                responseBody: error.message || "Network request failed",
                                durationMs: durationMs,
                                mimeType: ""
                            });
                            throw error;
                        }
                    };
                }

                // Hook XMLHttpRequest
                const XHR = XMLHttpRequest.prototype;
                const open = XHR.open;
                const send = XHR.send;
                const setRequestHeader = XHR.setRequestHeader;

                XHR.open = function(method, url) {
                    this._method = method;
                    this._url = url;
                    this._reqHeaders = {};
                    this._startTime = performance.now();
                    return open.apply(this, arguments);
                };

                XHR.setRequestHeader = function(header, value) {
                    if (!this._reqHeaders) this._reqHeaders = {};
                    this._reqHeaders[header] = value;
                    return setRequestHeader.apply(this, arguments);
                };

                XHR.send = function(postData) {
                    this._reqBody = typeof postData === 'string' ? postData : (postData ? '[Object Body]' : null);
                    
                    this.addEventListener("load", function() {
                        const durationMs = Math.round(performance.now() - (this._startTime || performance.now()));
                        let resHeadersStr = this.getAllResponseHeaders() || "";
                        let resHeaders = {};
                        resHeadersStr.trim().split(/[\r\n]+/).forEach(line => {
                            let parts = line.split(': ');
                            let header = parts.shift();
                            let value = parts.join(': ');
                            if (header) resHeaders[header] = value;
                        });

                        sendToAndroid({
                            url: this._url,
                            method: this._method || "GET",
                            status: this.status,
                            statusText: this.statusText,
                            requestHeaders: this._reqHeaders || {},
                            requestBody: this._reqBody,
                            responseHeaders: resHeaders,
                            responseBody: this.responseText || null,
                            durationMs: durationMs,
                            mimeType: this.getResponseHeader("content-type") || ""
                        });
                    });

                    this.addEventListener("error", function() {
                        const durationMs = Math.round(performance.now() - (this._startTime || performance.now()));
                        sendToAndroid({
                            url: this._url,
                            method: this._method || "GET",
                            status: 0,
                            statusText: "XHR Error",
                            requestHeaders: this._reqHeaders || {},
                            requestBody: this._reqBody,
                            responseHeaders: {},
                            responseBody: "XHR Network Error",
                            durationMs: durationMs,
                            mimeType: ""
                        });
                    });

                    return send.apply(this, arguments);
                };
            })();
        """.trimIndent()
    }
}
