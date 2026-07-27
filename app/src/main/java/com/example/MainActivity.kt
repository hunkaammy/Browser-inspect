package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BookmarksAndHistorySheet
import com.example.ui.components.BrowserTopBar
import com.example.ui.components.CustomWebView
import com.example.ui.components.DevToolsNetworkPanel
import com.example.ui.components.RequestDetailViewer
import com.example.ui.components.TabsManagerSheet
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.DevToolsViewMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DevInspectApp()
            }
        }
    }
}

@Composable
fun DevInspectApp(viewModel: BrowserViewModel = viewModel()) {
    val context = LocalContext.current
    val activeTabId by viewModel.activeTabId.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val urlInput by viewModel.urlInput.collectAsState()
    val devToolsViewMode by viewModel.devToolsViewMode.collectAsState()
    val requests by viewModel.filteredRequests.collectAsState()
    val rawRequests by viewModel.repository.requests.collectAsState()
    val isRecording by viewModel.repository.isRecording.collectAsState()
    val selectedFilter by viewModel.repository.selectedFilter.collectAsState()
    val searchQuery by viewModel.repository.searchQuery.collectAsState()
    val selectedRequest by viewModel.selectedRequest.collectAsState()
    val showTabsSheet by viewModel.showTabsSheet.collectAsState()
    val showBookmarksSheet by viewModel.showBookmarksSheet.collectAsState()
    val history by viewModel.history.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val activeTab = viewModel.activeTab

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.navigationBars),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BrowserTopBar(
                tab = activeTab,
                urlInput = urlInput,
                requestCount = rawRequests.size,
                tabsCount = tabs.size,
                isRecording = isRecording,
                onUrlChange = viewModel::onUrlInputChanged,
                onNavigate = viewModel::navigateToUrl,
                onBack = {
                    // Handled in webview, or trigger back
                },
                onForward = {
                    // Handled in webview
                },
                onRefresh = {
                    activeTab?.let { tab ->
                        viewModel.navigateToUrl(tab.url)
                    }
                },
                onToggleDevTools = viewModel::toggleDevToolsViewMode,
                onToggleTabs = { viewModel.toggleTabsSheet(true) },
                onToggleBookmarks = { viewModel.toggleBookmarksSheet(true) },
                onToggleRecording = viewModel.repository::toggleRecording,
                onClearLogs = viewModel.repository::clearLogs
            )
        },
        floatingActionButton = {
            if (devToolsViewMode == DevToolsViewMode.CLOSED) {
                FloatingActionButton(
                    onClick = viewModel::toggleDevToolsViewMode,
                    containerColor = Color(0xFF06B6D4),
                    contentColor = Color.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Open DevTools Inspector"
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = Color(0xFF0B0F19)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Browser WebView Container Area
                if (devToolsViewMode != DevToolsViewMode.FULL_SCREEN && activeTab != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(if (devToolsViewMode == DevToolsViewMode.SPLIT_SCREEN) 0.5f else 1.0f)
                    ) {
                        CustomWebView(
                            tab = activeTab,
                            onTabUpdate = viewModel::onTabUpdated,
                            onRequestCaptured = viewModel.repository::addRequest
                        )
                    }
                }

                // DevTools Network Inspector Panel Area
                if (devToolsViewMode != DevToolsViewMode.CLOSED) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(if (devToolsViewMode == DevToolsViewMode.SPLIT_SCREEN) 0.5f else 1.0f)
                    ) {
                        DevToolsNetworkPanel(
                            requests = requests,
                            isRecording = isRecording,
                            selectedFilter = selectedFilter,
                            searchQuery = searchQuery,
                            viewMode = devToolsViewMode,
                            onToggleRecording = viewModel.repository::toggleRecording,
                            onClearLogs = viewModel.repository::clearLogs,
                            onFilterSelected = viewModel.repository::setFilter,
                            onSearchQueryChanged = viewModel.repository::setSearchQuery,
                            onViewModeChanged = viewModel::setDevToolsViewMode,
                            onRequestClick = viewModel::selectRequest
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet for Request Details
    if (selectedRequest != null) {
        RequestDetailViewer(
            request = selectedRequest,
            repository = viewModel.repository,
            onDismiss = { viewModel.selectRequest(null) },
            onReplay = viewModel::replayRequest
        )
    }

    // Modal Sheet for Presets / Bookmarks Catalog
    if (showBookmarksSheet) {
        BookmarksAndHistorySheet(
            history = history,
            onSelectUrl = { url ->
                viewModel.navigateToUrl(url)
            },
            onDismiss = { viewModel.toggleBookmarksSheet(false) }
        )
    }

    // Modal Sheet for Tabs Manager
    if (showTabsSheet) {
        TabsManagerSheet(
            tabs = tabs,
            activeTabId = activeTabId,
            onSelectTab = viewModel::selectTab,
            onOpenNewTab = { viewModel.openNewTab() },
            onCloseTab = viewModel::closeTab,
            onDismiss = { viewModel.toggleTabsSheet(false) }
        )
    }
}
