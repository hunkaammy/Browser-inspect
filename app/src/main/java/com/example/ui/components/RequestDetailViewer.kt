package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.NetworkRequest
import com.example.network.NetworkInspectorRepository
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailViewer(
    request: NetworkRequest?,
    repository: NetworkInspectorRepository,
    onDismiss: () -> Unit,
    onReplay: (NetworkRequest) -> Unit
) {
    if (request == null) return

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf("Overview", "Headers", "Response", "Payload", "Preview", "cURL / Replay")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF111827), // Dark VS Code theme
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = if (request.isSuccess) Color(0xFF22C55E) else Color(0xFFEF4444)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${request.method} ${request.statusCode}",
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = request.host,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            // Tab Bar
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFF1F2937),
                contentColor = Color(0xFF06B6D4),
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) Color(0xFF06B6D4) else Color(0xFF9CA3AF)
                            )
                        }
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF374151))

            // Tab Content Body
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> RequestOverviewTab(request = request, context = context)
                    1 -> RequestHeadersTab(request = request, context = context)
                    2 -> RequestResponseTab(request = request, context = context)
                    3 -> RequestPayloadTab(request = request, context = context)
                    4 -> RequestPreviewTab(request = request)
                    5 -> RequestCurlAndReplayTab(
                        request = request,
                        repository = repository,
                        context = context,
                        onReplay = onReplay
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestOverviewTab(request: NetworkRequest, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DetailSectionCard(title = "General Information") {
            DetailRow("Request URL", request.url, isCopyable = true, context = context)
            DetailRow("Request Method", request.method)
            DetailRow("Status Code", "${request.statusCode} ${request.statusText}")
            DetailRow("Duration", "${request.durationMs} ms")
            DetailRow("Content Length", "${request.contentLength} bytes")
            DetailRow("MIME Type", request.mimeType ?: "Unknown")
            val dateStr = try {
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(request.timestamp))
            } catch (e: Exception) {
                request.timestamp.toString()
            }
            DetailRow("Timestamp", dateStr)
        }
    }
}

@Composable
private fun RequestHeadersTab(request: NetworkRequest, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DetailSectionCard(title = "Request Headers (${request.requestHeaders.size})") {
            if (request.requestHeaders.isEmpty()) {
                Text("No request headers captured", fontSize = 12.sp, color = Color.Gray)
            } else {
                request.requestHeaders.forEach { (key, value) ->
                    DetailRow(key, value, isCopyable = true, context = context)
                }
            }
        }

        DetailSectionCard(title = "Response Headers (${request.responseHeaders.size})") {
            if (request.responseHeaders.isEmpty()) {
                Text("No response headers captured", fontSize = 12.sp, color = Color.Gray)
            } else {
                request.responseHeaders.forEach { (key, value) ->
                    DetailRow(key, value, isCopyable = true, context = context)
                }
            }
        }
    }
}

@Composable
private fun RequestResponseTab(request: NetworkRequest, context: Context) {
    val rawBody = request.responseBody ?: "No response body available"
    val prettyBody = remember(rawBody) { formatJsonPretty(rawBody) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Response Payload", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF06B6D4))

            Button(
                onClick = { copyToClipboard(context, "Response Body", prettyBody) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy Raw JSON", fontSize = 11.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF374151), RoundedCornerShape(8.dp)),
            color = Color(0xFF0F172A)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = prettyBody,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun RequestPayloadTab(request: NetworkRequest, context: Context) {
    val rawPayload = request.requestBody ?: "No request body (e.g. GET parameters)"
    val prettyPayload = remember(rawPayload) { formatJsonPretty(rawPayload) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Request Body / Parameters", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF06B6D4))

            if (!request.requestBody.isNull_or_empty()) {
                Button(
                    onClick = { copyToClipboard(context, "Request Body", prettyPayload) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Body", fontSize = 11.sp, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF374151), RoundedCornerShape(8.dp)),
            color = Color(0xFF0F172A)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = prettyPayload,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFFF1F5F9),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun RequestPreviewTab(request: NetworkRequest) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (request.isImage || request.mimeType?.startsWith("image") == true) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF374151), RoundedCornerShape(12.dp)),
                color = Color(0xFF1E293B)
            ) {
                AsyncImage(
                    model = request.url,
                    contentDescription = "Image preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Rendered Image Preview", fontSize = 12.sp, color = Color(0xFF94A3B8))
        } else {
            val responseText = request.responseBody ?: "No preview available"
            val prettyText = remember(responseText) { formatJsonPretty(responseText) }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF374151), RoundedCornerShape(8.dp)),
                color = Color(0xFF0F172A)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = prettyText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestCurlAndReplayTab(
    request: NetworkRequest,
    repository: NetworkInspectorRepository,
    context: Context,
    onReplay: (NetworkRequest) -> Unit
) {
    val curlCmd = remember(request) { repository.generateCurl(request) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Replay API Button
        Button(
            onClick = { onReplay(request) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Replay API Request Live", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        // cURL Command Viewer
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("cURL Command (Terminal / Postman)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF06B6D4))

                Button(
                    onClick = { copyToClipboard(context, "cURL Command", curlCmd) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy cURL", fontSize = 11.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF374151), RoundedCornerShape(8.dp)),
                color = Color(0xFF0F172A)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = curlCmd,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFF59E0B),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF06B6D4)
            )
            HorizontalDivider(color = Color(0xFF374151))
            content()
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isCopyable: Boolean = false,
    context: Context? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCopyable && context != null) {
                    Modifier.clickable { copyToClipboard(context, label, value) }
                } else Modifier
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF9CA3AF),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(0.65f)
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
}

private fun formatJsonPretty(jsonString: String): String {
    if (jsonString.isBlank()) return "Empty Body"
    return try {
        val trimmed = jsonString.trim()
        if (trimmed.startsWith("{")) {
            JSONObject(trimmed).toString(2)
        } else if (trimmed.startsWith("[")) {
            JSONArray(trimmed).toString(2)
        } else {
            jsonString
        }
    } catch (e: Exception) {
        jsonString
    }
}

private fun String?.isNull_or_empty(): Boolean = this.isNullOrBlank()
