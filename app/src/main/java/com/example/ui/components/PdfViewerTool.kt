package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun openPdfInExternalApp(context: Context, file: File) {
    try {
        if (!file.exists() || file.length() == 0L) {
            Toast.makeText(context, "PDF file is not ready or empty", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open PDF with..."))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open external app: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Embedded Native PDF Viewer Tool using Android's PdfRenderer.
 * Renders authentic pages with high-definition crisp rendering, zoom, pan, and page navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerDialog(
    pdfFile: File,
    title: String,
    onDismiss: () -> Unit,
    onOpenInFocus: ((File) -> Unit)? = null,
    onShare: ((File) -> Unit)? = null,
    floatingFocusWidget: (@Composable () -> Unit)? = null
) {
    var isFullScreen by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullScreen) 0.dp else 8.dp),
            shape = if (isFullScreen) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PdfViewerContent(
                    pdfFile = pdfFile,
                    title = title,
                    onDismiss = onDismiss,
                    onOpenInFocus = onOpenInFocus,
                    onShare = onShare,
                    isFullScreen = isFullScreen,
                    onToggleFullScreen = { isFullScreen = !isFullScreen }
                )

                // Movable floating focus widget renders inside the PDF viewer and stays accessible even in fullscreen!
                if (floatingFocusWidget != null) {
                    floatingFocusWidget()
                }
            }
        }
    }
}

@Composable
fun PdfViewerContent(
    pdfFile: File,
    title: String,
    onDismiss: () -> Unit,
    onOpenInFocus: ((File) -> Unit)? = null,
    onShare: ((File) -> Unit)? = null,
    isFullScreen: Boolean = false,
    onToggleFullScreen: (() -> Unit)? = null
) {
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    // Zoom & Pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val coroutineScope = rememberCoroutineScope()

    // Initialize PdfRenderer
    DisposableEffect(pdfFile) {
        try {
            if (!pdfFile.exists() || pdfFile.length() == 0L) {
                loadError = "PDF file not found or empty: ${pdfFile.name}"
            } else {
                val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                fileDescriptor = pfd
                val rend = PdfRenderer(pfd)
                renderer = rend
                pageCount = rend.pageCount
                currentPageIndex = 0
                loadError = null
            }
        } catch (e: Exception) {
            Log.e("PdfViewerTool", "Failed to open PDF: ${e.message}", e)
            loadError = "Unable to parse PDF: ${e.localizedMessage ?: "Invalid PDF format"}"
        }

        onDispose {
            try {
                renderer?.close()
                fileDescriptor?.close()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    // Render current page when renderer or pageIndex changes
    fun renderPage(index: Int) {
        val rend = renderer ?: return
        if (index !in 0 until pageCount) return

        isLoadingPage = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val page = rend.openPage(index)

                // Calculate safe dimensions to avoid OutOfMemory on Android 7.1.1 devices
                val maxDim = 1200f
                val maxPageDim = maxOf(page.width, page.height).toFloat()
                val scaleFactor = if (maxPageDim > 0) (maxDim / maxPageDim).coerceIn(0.8f, 1.6f) else 1.2f
                val width = (page.width * scaleFactor).toInt().coerceAtLeast(1)
                val height = (page.height * scaleFactor).toInt().coerceAtLeast(1)

                val bitmap = try {
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                } catch (oom: Throwable) {
                    val safeW = (width / 2).coerceAtLeast(1)
                    val safeH = (height / 2).coerceAtLeast(1)
                    Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
                }

                // Pre-fill solid white background to eliminate anti-aliasing white gaps on Android 7.1.1
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                withContext(Dispatchers.Main) {
                    currentBitmap?.recycle()
                    currentBitmap = bitmap
                    isLoadingPage = false
                }
            } catch (e: Throwable) {
                Log.e("PdfViewerTool", "Error rendering page $index: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    isLoadingPage = false
                    loadError = "Failed rendering page ${index + 1}: ${e.message}"
                }
            }
        }
    }

    LaunchedEffect(renderer, currentPageIndex) {
        if (renderer != null && pageCount > 0) {
            renderPage(currentPageIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("embedded_pdf_viewer_tool")
    ) {
        // --- 1. Top Header Bar ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "PDF Icon",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = if (pageCount > 0) "Page ${currentPageIndex + 1} of $pageCount • Native PDF Tool" else "Loading PDF...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { openPdfInExternalApp(context, pdfFile) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open in External App",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (onOpenInFocus != null) {
                        FilledTonalButton(
                            onClick = { onOpenInFocus(pdfFile) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Focus Room", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (onToggleFullScreen != null) {
                        IconButton(
                            onClick = onToggleFullScreen,
                            modifier = Modifier.testTag("toggle_fullscreen_button")
                        ) {
                            Icon(
                                imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (isFullScreen) "Exit Fullscreen" else "Enter Fullscreen"
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_pdf_viewer_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close PDF")
                    }
                }
            }
        }

        // --- 2. Interactive PDF Canvas ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF333338))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4.5f)
                        if (scale > 1f) {
                            val maxOffsetX = (size.width * (scale - 1)) / 2f
                            val maxOffsetY = (size.height * (scale - 1)) / 2f
                            val newX = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                            val newY = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            offset = Offset(newX, newY)
                        } else {
                            offset = Offset.Zero
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                loadError != null -> {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "PDF Display Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = loadError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { openPdfInExternalApp(context, pdfFile) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open in Device PDF App")
                        }
                    }
                }

                currentBitmap != null -> {
                    Image(
                        bitmap = currentBitmap!!.asImageBitmap(),
                        contentDescription = "PDF Page ${currentPageIndex + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                    )
                }
            }

            if (isLoadingPage) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Rendering Page ${currentPageIndex + 1}...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Floating Quick Zoom Badge
            if (scale > 1f) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "${(scale * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // --- 3. Bottom Controls Toolbar ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                // Page slider if multiple pages
                if (pageCount > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = currentPageIndex.toFloat(),
                            onValueChange = { currentPageIndex = it.toInt() },
                            valueRange = 0f..(pageCount - 1).toFloat(),
                            steps = (pageCount - 2).coerceAtLeast(0),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Text(
                            text = "$pageCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Paging Buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (currentPageIndex > 0) {
                                    currentPageIndex -= 1
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                            },
                            enabled = currentPageIndex > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Page")
                        }

                        Text(
                            text = "${currentPageIndex + 1} / $pageCount",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                if (currentPageIndex < pageCount - 1) {
                                    currentPageIndex += 1
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                            },
                            enabled = currentPageIndex < pageCount - 1
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page")
                        }
                    }

                    // Zoom Controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                scale = (scale - 0.35f).coerceAtLeast(1f)
                                if (scale == 1f) offset = Offset.Zero
                            }
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                        }

                        IconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            }
                        ) {
                            Icon(Icons.Default.FitScreen, contentDescription = "Reset Zoom")
                        }

                        IconButton(
                            onClick = {
                                scale = (scale + 0.35f).coerceAtMost(4.5f)
                            }
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Embedded PDF Viewer specifically tailored for the Exam Focus Room.
 * Shows high-definition rendered exam pages with pinch-to-zoom, pan, page navigation,
 * and a direct shortcut to expand into full-screen PDF view.
 */
@Composable
fun FocusRoomPdfViewer(
    pdfFile: File,
    title: String,
    onOpenFullViewer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(pdfFile) {
        try {
            if (!pdfFile.exists() || pdfFile.length() == 0L) {
                loadError = "PDF file not available: ${pdfFile.name}"
            } else {
                val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                fileDescriptor = pfd
                val rend = PdfRenderer(pfd)
                renderer = rend
                pageCount = rend.pageCount
                currentPageIndex = 0
                loadError = null
            }
        } catch (e: Exception) {
            Log.e("FocusRoomPdfViewer", "Error opening PDF: ${e.message}", e)
            loadError = "PDF rendering error: ${e.localizedMessage}"
        }

        onDispose {
            try {
                renderer?.close()
                fileDescriptor?.close()
            } catch (e: Exception) {}
        }
    }

    val context = LocalContext.current

    fun renderPage(index: Int) {
        val rend = renderer ?: return
        if (index !in 0 until pageCount) return

        isLoadingPage = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val page = rend.openPage(index)

                val maxDim = 1200f
                val maxPageDim = maxOf(page.width, page.height).toFloat()
                val scaleFactor = if (maxPageDim > 0) (maxDim / maxPageDim).coerceIn(0.8f, 1.6f) else 1.2f
                val width = (page.width * scaleFactor).toInt().coerceAtLeast(1)
                val height = (page.height * scaleFactor).toInt().coerceAtLeast(1)

                val bitmap = try {
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                } catch (oom: Throwable) {
                    val safeW = (width / 2).coerceAtLeast(1)
                    val safeH = (height / 2).coerceAtLeast(1)
                    Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
                }

                // Pre-fill solid white background to eliminate anti-aliasing white gaps on Android 7.1.1
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                withContext(Dispatchers.Main) {
                    currentBitmap?.recycle()
                    currentBitmap = bitmap
                    isLoadingPage = false
                }
            } catch (e: Throwable) {
                Log.e("FocusRoomPdfViewer", "Page rendering failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    isLoadingPage = false
                    loadError = "Could not render page ${index + 1}: ${e.message}"
                }
            }
        }
    }

    LaunchedEffect(renderer, currentPageIndex) {
        if (renderer != null && pageCount > 0) {
            renderPage(currentPageIndex)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { openPdfInExternalApp(context, pdfFile) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open in External App",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        FilledTonalButton(
                            onClick = onOpenFullViewer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fullscreen", fontSize = 11.sp)
                        }
                    }
                }
            }

            // PDF Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF2E2E32))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4.0f)
                            if (scale > 1f) {
                                val maxOffsetX = (size.width * (scale - 1)) / 2f
                                val maxOffsetY = (size.height * (scale - 1)) / 2f
                                val newX = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                val newY = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                offset = Offset(newX, newY)
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    loadError != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = loadError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { openPdfInExternalApp(context, pdfFile) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open in Device PDF App", fontSize = 11.sp)
                            }
                        }
                    }
                    currentBitmap != null -> {
                        Image(
                            bitmap = currentBitmap!!.asImageBitmap(),
                            contentDescription = "PDF Page",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                        )
                    }
                }

                if (isLoadingPage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color.White
                    )
                }
            }

            // Bottom Navigation Footer
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (currentPageIndex > 0) {
                                    currentPageIndex -= 1
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                            },
                            enabled = currentPageIndex > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = if (pageCount > 0) "Page ${currentPageIndex + 1} of $pageCount" else "Loading...",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                if (currentPageIndex < pageCount - 1) {
                                    currentPageIndex += 1
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                            },
                            enabled = currentPageIndex < pageCount - 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", modifier = Modifier.size(16.dp))
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { scale = (scale - 0.3f).coerceAtLeast(1f) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Out", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { scale = 1f; offset = Offset.Zero },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.FitScreen, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { scale = (scale + 0.3f).coerceAtMost(4f) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "In", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
