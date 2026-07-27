package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.Bookmark
import com.example.model.NetworkRequest
import com.example.model.PresetBookmarks
import com.example.model.ResourceType
import com.example.model.WebTab
import com.example.network.NetworkInspectorRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class DevToolsViewMode {
    CLOSED,
    SPLIT_SCREEN,
    FULL_SCREEN
}

class BrowserViewModel(
    val repository: NetworkInspectorRepository = NetworkInspectorRepository()
) : ViewModel() {

    private val _tabs = MutableStateFlow<List<WebTab>>(
        listOf(
            WebTab(
                id = UUID.randomUUID().toString(),
                title = "JSONPlaceholder",
                url = "https://jsonplaceholder.typicode.com/posts/1"
            )
        )
    )
    val tabs: StateFlow<List<WebTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(_tabs.value.first().id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _urlInput = MutableStateFlow(_tabs.value.first().url)
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _devToolsViewMode = MutableStateFlow(DevToolsViewMode.CLOSED)
    val devToolsViewMode: StateFlow<DevToolsViewMode> = _devToolsViewMode.asStateFlow()

    private val _selectedRequest = MutableStateFlow<NetworkRequest?>(null)
    val selectedRequest: StateFlow<NetworkRequest?> = _selectedRequest.asStateFlow()

    private val _showTabsSheet = MutableStateFlow(false)
    val showTabsSheet: StateFlow<Boolean> = _showTabsSheet.asStateFlow()

    private val _showBookmarksSheet = MutableStateFlow(false)
    val showBookmarksSheet: StateFlow<Boolean> = _showBookmarksSheet.asStateFlow()

    private val _history = MutableStateFlow<List<Bookmark>>(emptyList())
    val history: StateFlow<List<Bookmark>> = _history.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Filtered requests flow
    val filteredRequests: StateFlow<List<NetworkRequest>> = combine(
        repository.requests,
        repository.selectedFilter,
        repository.searchQuery
    ) { requests, filter, query ->
        requests.filter { req ->
            val matchesFilter = when (filter) {
                ResourceType.ALL -> true
                ResourceType.XHR_FETCH -> req.resourceType == ResourceType.XHR_FETCH || req.mimeType?.contains("json", true) == true
                ResourceType.IMAGE -> req.resourceType == ResourceType.IMAGE || req.isImage
                ResourceType.DOCUMENT -> req.resourceType == ResourceType.DOCUMENT
                ResourceType.JS_CSS -> req.resourceType == ResourceType.JS_CSS
                ResourceType.MEDIA -> req.resourceType == ResourceType.MEDIA
                ResourceType.OTHER -> req.resourceType == ResourceType.OTHER
            }

            val matchesQuery = query.isBlank() ||
                    req.url.contains(query, ignoreCase = true) ||
                    req.method.contains(query, ignoreCase = true) ||
                    req.statusCode.toString().contains(query) ||
                    req.mimeType?.contains(query, ignoreCase = true) == true

            matchesFilter && matchesQuery
        }
    }.let { flow ->
        val state = MutableStateFlow<List<NetworkRequest>>(emptyList())
        viewModelScope.launch {
            flow.collect { state.value = it }
        }
        state.asStateFlow()
    }

    val activeTab: WebTab?
        get() = _tabs.value.find { it.id == _activeTabId.value }

    fun onUrlInputChanged(newUrl: String) {
        _urlInput.value = newUrl
    }

    fun navigateToUrl(rawUrl: String = _urlInput.value) {
        var formattedUrl = rawUrl.trim()
        if (formattedUrl.isBlank()) return

        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = if (formattedUrl.contains(".") && !formattedUrl.contains(" ")) {
                "https://$formattedUrl"
            } else {
                "https://www.google.com/search?q=${java.net.URLEncoder.encode(formattedUrl, "UTF-8")}"
            }
        }

        _urlInput.value = formattedUrl
        updateActiveTab { it.copy(url = formattedUrl, isLoading = true) }

        // Add to history
        val newHistoryItem = Bookmark(
            title = formattedUrl,
            url = formattedUrl,
            category = "History",
            description = "Visited ${System.currentTimeMillis()}"
        )
        _history.update { listOf(newHistoryItem) + it.take(49) }
    }

    fun updateActiveTab(transform: (WebTab) -> WebTab) {
        val currentId = _activeTabId.value
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == currentId) {
                    val updated = transform(tab)
                    if (updated.url != _urlInput.value && !updated.isLoading) {
                        _urlInput.value = updated.url
                    }
                    updated
                } else tab
            }
        }
    }

    fun onTabUpdated(updatedTab: WebTab) {
        updateActiveTab { updatedTab }
    }

    fun selectTab(id: String) {
        _activeTabId.value = id
        _tabs.value.find { it.id == id }?.let {
            _urlInput.value = it.url
        }
        _showTabsSheet.value = false
    }

    fun openNewTab(url: String = "https://jsonplaceholder.typicode.com/posts/1") {
        val newTab = WebTab(
            id = UUID.randomUUID().toString(),
            title = "New Tab",
            url = url
        )
        _tabs.update { it + newTab }
        _activeTabId.value = newTab.id
        _urlInput.value = url
        _showTabsSheet.value = false
    }

    fun closeTab(id: String) {
        if (_tabs.value.size <= 1) {
            // Keep at least one tab
            openNewTab("https://jsonplaceholder.typicode.com/posts/1")
        }
        val currentList = _tabs.value
        val newList = currentList.filterNot { it.id == id }
        _tabs.value = newList
        if (_activeTabId.value == id) {
            val nextTab = newList.lastOrNull() ?: return
            _activeTabId.value = nextTab.id
            _urlInput.value = nextTab.url
        }
    }

    fun toggleDevToolsViewMode() {
        _devToolsViewMode.update { current ->
            when (current) {
                DevToolsViewMode.CLOSED -> DevToolsViewMode.SPLIT_SCREEN
                DevToolsViewMode.SPLIT_SCREEN -> DevToolsViewMode.FULL_SCREEN
                DevToolsViewMode.FULL_SCREEN -> DevToolsViewMode.CLOSED
            }
        }
    }

    fun setDevToolsViewMode(mode: DevToolsViewMode) {
        _devToolsViewMode.value = mode
    }

    fun selectRequest(request: NetworkRequest?) {
        _selectedRequest.value = request
    }

    fun toggleTabsSheet(show: Boolean? = null) {
        _showTabsSheet.update { show ?: !it }
    }

    fun toggleBookmarksSheet(show: Boolean? = null) {
        _showBookmarksSheet.update { show ?: !it }
    }

    fun replayRequest(request: NetworkRequest) {
        viewModelScope.launch {
            _userMessage.emit("Replaying request to ${request.host}...")
            val replayed = repository.replayRequest(request)
            _selectedRequest.value = replayed
            _userMessage.emit("Replay finished with status ${replayed.statusCode}")
        }
    }

    fun emitMessage(msg: String) {
        viewModelScope.launch {
            _userMessage.emit(msg)
        }
    }
}
