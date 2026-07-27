package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NetworkRequest
import com.example.model.ResourceType
import com.example.viewmodel.DevToolsViewMode

@Composable
fun DevToolsNetworkPanel(
    requests: List<NetworkRequest>,
    isRecording: Boolean,
    selectedFilter: ResourceType,
    searchQuery: String,
    viewMode: DevToolsViewMode,
    onToggleRecording: () -> Unit,
    onClearLogs: () -> Unit,
    onFilterSelected: (ResourceType) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onViewModeChanged: (DevToolsViewMode) -> Unit,
    onRequestClick: (NetworkRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF111827), // Dark VS Code / DevTools panel background
        contentColor = Color(0xFFF3F4F6)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Control Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1F2937))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Record / Pause Toggle Button
                    IconButton(
                        onClick = onToggleRecording,
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (isRecording) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = "Recording",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Paused",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Clear Logs
                    IconButton(
                        onClick = onClearLogs,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear logs",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "Network Inspector",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF06B6D4)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Split / Fullscreen / Close Buttons
                    IconButton(
                        onClick = {
                            val nextMode = if (viewMode == DevToolsViewMode.FULL_SCREEN)
                                DevToolsViewMode.SPLIT_SCREEN
                            else DevToolsViewMode.FULL_SCREEN
                            onViewModeChanged(nextMode)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (viewMode == DevToolsViewMode.FULL_SCREEN)
                                Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Toggle Fullscreen",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { onViewModeChanged(DevToolsViewMode.CLOSED) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Inspector",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Search Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Filter by URL, status, method...", fontSize = 12.sp, color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1F2937),
                        unfocusedContainerColor = Color(0xFF1F2937),
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color(0xFF374151),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                )
            }

            // Category Filter Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(ResourceType.values()) { type ->
                    val isSelected = selectedFilter == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterSelected(type) },
                        label = {
                            Text(
                                text = type.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = getResourceTypeIcon(type),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF06B6D4),
                            selectedLabelColor = Color.Black,
                            selectedLeadingIconColor = Color.Black,
                            containerColor = Color(0xFF1F2937),
                            labelColor = Color(0xFFD1D5DB),
                            iconColor = Color(0xFF9CA3AF)
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            // Metrics Summary Pill Bar
            val totalBytes = requests.sumOf { it.contentLength }
            val formattedBytes = when {
                totalBytes > 1024 * 1024 -> String.format("%.1f MB", totalBytes / (1024f * 1024f))
                totalBytes > 1024 -> String.format("%.1f KB", totalBytes / 1024f)
                else -> "$totalBytes B"
            }
            val avgDuration = if (requests.isNotEmpty()) requests.map { it.durationMs }.average().toInt() else 0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${requests.size} requests",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Transferred: $formattedBytes",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Avg Latency: ${avgDuration}ms",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    fontFamily = FontFamily.Monospace
                )
            }

            // Requests List
            if (requests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Http,
                            contentDescription = null,
                            tint = Color(0xFF4B5563),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRecording) "Waiting for network activity..." else "Logging is paused",
                            color = Color(0xFF9CA3AF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Browse any web page or test preset APIs above",
                            color = Color(0xFF6B7280),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(requests, key = { it.id }) { req ->
                        NetworkRequestCard(
                            request = req,
                            onClick = { onRequestClick(req) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkRequestCard(
    request: NetworkRequest,
    onClick: () -> Unit
) {
    val methodColor = when (request.method.uppercase()) {
        "GET" -> Color(0xFF22C55E) // Green
        "POST" -> Color(0xFF06B6D4) // Cyan
        "PUT" -> Color(0xFFF59E0B) // Amber
        "DELETE" -> Color(0xFFEF4444) // Red
        else -> Color(0xFFA855F7) // Purple
    }

    val statusColor = when {
        request.statusCode in 200..299 -> Color(0xFF22C55E)
        request.statusCode in 300..399 -> Color(0xFF3B82F6)
        request.statusCode in 400..599 -> Color(0xFFEF4444)
        else -> Color.Gray
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color(0xFF1F2937),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Method Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(methodColor.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = request.method,
                    color = methodColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status Code Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusColor.copy(alpha = 0.2f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (request.statusCode > 0) request.statusCode.toString() else "ERR",
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // URL Endpoint Path & Host
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.pathWithQuery,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = request.host,
                    color = Color(0xFF9CA3AF),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Duration & Size
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${request.durationMs}ms",
                    color = Color(0xFF06B6D4),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                val sizeText = when {
                    request.contentLength > 1024 -> String.format("%.1f KB", request.contentLength / 1024f)
                    request.contentLength > 0 -> "${request.contentLength} B"
                    else -> "-"
                }
                Text(
                    text = sizeText,
                    color = Color(0xFF9CA3AF),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun getResourceTypeIcon(type: ResourceType): ImageVector {
    return when (type) {
        ResourceType.ALL -> Icons.Default.Http
        ResourceType.XHR_FETCH -> Icons.Default.Code
        ResourceType.IMAGE -> Icons.Default.Image
        ResourceType.DOCUMENT -> Icons.Default.Description
        ResourceType.JS_CSS -> Icons.Default.Code
        ResourceType.MEDIA -> Icons.Default.Movie
        ResourceType.OTHER -> Icons.Default.Http
    }
}
