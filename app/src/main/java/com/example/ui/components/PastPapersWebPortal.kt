package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.DownloadListener
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.PastPaper
import kotlinx.coroutines.launch
import java.io.File

data class WebPortalPreset(
    val name: String,
    val shortName: String,
    val url: String,
    val iconEmoji: String,
    val description: String
)

val SRI_LANKA_PORTALS = listOf(
    WebPortalPreset(
        name = "Google Search",
        shortName = "Google",
        url = "https://www.google.com",
        iconEmoji = "🔍",
        description = "Search all Sri Lankan A/L past papers, marking schemes, and educational notes"
    ),
    WebPortalPreset(
        name = "Department of Examinations (IDNS / DoENets)",
        shortName = "IDNS / DoENets",
        url = "https://onlineexams.gov.lk/eic/",
        iconEmoji = "🏛️",
        description = "Official national exam results, past papers & circulars portal"
    ),
    WebPortalPreset(
        name = "e-Thaksalawa National Learning Portal",
        shortName = "e-Thaksalawa",
        url = "https://e-thaksalawa.moe.gov.lk",
        iconEmoji = "🎓",
        description = "Ministry of Education official e-learning & paper repository"
    ),
    WebPortalPreset(
        name = "PastPapers.wiki A/L Hub",
        shortName = "PastPapers.wiki",
        url = "https://pastpapers.wiki/advanced-level/",
        iconEmoji = "📚",
        description = "Sri Lanka's largest free archive of A/L past papers & marking schemes"
    ),
    WebPortalPreset(
        name = "National Institute of Education (NIE)",
        shortName = "NIE Sri Lanka",
        url = "https://nie.lk",
        iconEmoji = "🏫",
        description = "Teacher guides, syllabi, model papers & examination resources"
    ),
    WebPortalPreset(
        name = "AlevelApi Exam Resources",
        shortName = "AlevelApi",
        url = "https://alevelapi.com",
        iconEmoji = "⚡",
        description = "Curated past papers for Maths, Science, Commerce & Arts"
    )
)

/**
 * ReVanced-Manager style Web Browser for Sri Lankan examination portals with real-time
 * in-app download interception and automatic embedded PDF viewer trigger.
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastPapersWebPortal(
    downloadedPapers: List<PastPaper> = emptyList(),
    onDeletePaper: (PastPaper) -> Unit = {},
    onPaperDownloaded: (PastPaper, File) -> Unit,
    onOneTapDownloadSample: (String) -> Unit,
    isDownloading: Boolean,
    downloadProgressText: String,
    isFullScreen: Boolean = false,
    onToggleFullScreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(SRI_LANKA_PORTALS[0].url) }
    var addressInput by remember { mutableStateOf(SRI_LANKA_PORTALS[0].url) }
    var pageTitle by remember { mutableStateOf("Google Search") }
    var loadProgress by remember { mutableIntStateOf(0) }
    var isLoadingPage by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(SRI_LANKA_PORTALS[0]) }
    var showDownloadsDialog by remember { mutableStateOf(false) }
    var rendererCrashed by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Safely cleanup WebView instance when leaving composable
    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewInstance?.stopLoading()
                (webViewInstance?.parent as? ViewGroup)?.removeView(webViewInstance)
                webViewInstance?.destroy()
                webViewInstance = null
            } catch (_: Throwable) {}
        }
    }

    // Handle back button for in-webview navigation or exiting fullscreen
    BackHandler(enabled = isFullScreen || webViewInstance?.canGoBack() == true) {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else if (isFullScreen) {
            onToggleFullScreen()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("past_papers_web_portal")
    ) {
        // --- 1. ReVanced Header Bar: Portal Switcher Chips ---
        AnimatedVisibility(visible = !isFullScreen) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 3.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(selectedPreset.iconEmoji, fontSize = 14.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "A/L Web Downloader",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clickable { showDownloadsDialog = true }
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Downloads (${downloadedPapers.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Presets Horizontal Row (ReVanced Style)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SRI_LANKA_PORTALS.forEach { preset ->
                            val isSelected = selectedPreset == preset
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedPreset = preset
                                    currentUrl = preset.url
                                    addressInput = preset.url
                                    webViewInstance?.loadUrl(preset.url)
                                },
                                label = {
                                    Text(
                                        text = "${preset.iconEmoji} ${preset.shortName}",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("portal_chip_${preset.shortName}")
                            )
                        }
                    }
                }
            }
        }

        // Dialog displaying downloaded files with Remove buttons
        if (showDownloadsDialog) {
            AlertDialog(
                onDismissRequest = { showDownloadsDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Downloaded A/L Exam Papers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Text(
                                text = "📁 Saved on device: Download / AL_Study_Papers",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        if (downloadedPapers.isEmpty()) {
                            Text(
                                text = "No downloaded papers yet. Browse portals below to download papers directly into device storage.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                downloadedPapers.forEach { paper ->
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = paper.title,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "${paper.subject} • ${paper.year} • ${paper.fileSizeMb} MB",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        onPaperDownloaded(paper, File(paper.localPdfPath ?: ""))
                                                        showDownloadsDialog = false
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Fullscreen, contentDescription = "View PDF", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(
                                                    onClick = { onDeletePaper(paper) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Remove downloaded paper", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDownloadsDialog = false }) {
                        Text("Done")
                    }
                }
            )
        }

        // --- 3. Browser Navigation Bar ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { webViewInstance?.goBack() },
                        enabled = webViewInstance?.canGoBack() == true,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = { webViewInstance?.goForward() },
                        enabled = webViewInstance?.canGoForward() == true,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = { webViewInstance?.reload() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload", modifier = Modifier.size(20.dp))
                    }

                    // Address Input
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .padding(horizontal = 4.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                val url = if (!addressInput.startsWith("http://") && !addressInput.startsWith("https://")) {
                                    "https://$addressInput"
                                } else {
                                    addressInput
                                }
                                currentUrl = url
                                webViewInstance?.loadUrl(url)
                            }
                        )
                    )

                    IconButton(
                        onClick = {
                            currentUrl = selectedPreset.url
                            addressInput = selectedPreset.url
                            webViewInstance?.loadUrl(selectedPreset.url)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = onToggleFullScreen,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("web_portal_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullScreen) "Exit Fullscreen" else "Enter Fullscreen",
                            tint = if (isFullScreen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Loading bar
                if (isLoadingPage) {
                    LinearProgressIndicator(
                        progress = { loadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // --- 4. Main In-App WebView with Real Download Interception ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (rendererCrashed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Web Portal Renderer Reset",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "The web renderer process exited to conserve system memory in this virtualized environment. You can reload this page or open official exam portals directly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { rendererCrashed = false }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reload Page")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val target = SRI_LANKA_PORTALS[1]
                                        selectedPreset = target
                                        currentUrl = target.url
                                        addressInput = target.url
                                        rendererCrashed = false
                                    }
                                ) {
                                    Text("Open DoENets")
                                }
                            }
                        }
                    }
                }
            } else {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                builtInZoomControls = true
                                displayZoomControls = false
                                setSupportZoom(true)
                                mediaPlaybackRequiresUserGesture = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                cacheMode = WebSettings.LOAD_DEFAULT
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoadingPage = true
                                    url?.let {
                                        currentUrl = it
                                        addressInput = it
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoadingPage = false
                                    pageTitle = view?.title ?: pageTitle
                                }

                                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                    handler?.proceed()
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    if (url.endsWith(".pdf", ignoreCase = true) ||
                                        url.contains("/download/", ignoreCase = true) ||
                                        url.contains("/get-paper", ignoreCase = true)
                                    ) {
                                        val guessed = URLUtil.guessFileName(url, null, "application/pdf")
                                        onOneTapDownloadSample(url)
                                        return true
                                    }
                                    return false
                                }

                                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                    val didCrash = detail?.didCrash() ?: true
                                    Log.w("PastPapersWebPortal", "WebView render process exited (crashed=$didCrash). Switching to graceful recovery.")
                                    try {
                                        (view?.parent as? ViewGroup)?.removeView(view)
                                        view?.destroy()
                                    } catch (_: Throwable) {}
                                    webViewInstance = null
                                    rendererCrashed = true
                                    return true // Prevents host app process termination!
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadProgress = newProgress
                                    if (newProgress == 100) isLoadingPage = false
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    title?.let { pageTitle = it }
                                }
                            }

                            setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                                Log.d("PastPapersWebPortal", "Download intercepted: $url ($mimetype)")
                                onOneTapDownloadSample(url)
                            })

                            loadUrl(currentUrl)
                            webViewInstance = this
                        }
                    },
                    update = { webView ->
                        webViewInstance = webView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // In-App Download Progress Banner
            if (isDownloading) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Downloading Past Paper...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = downloadProgressText.ifBlank { "Saving inside app & opening in PDF viewer..." },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
        }
    }
}

