package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PastPaper
import com.example.ui.components.PastPapersWebPortal
import com.example.ui.components.ZoomableDocumentViewer
import java.io.File

@Composable
fun PastPapersScreen(
    papers: List<PastPaper>,
    selectedSubject: String,
    searchQuery: String,
    isFetching: Boolean,
    onSubjectChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onAutoFetch: () -> Unit,
    onToggleCache: (PastPaper) -> Unit,
    onShareToChat: (PastPaper) -> Unit,
    onStartFocus: (PastPaper) -> Unit,
    onOpenPdfViewer: (PastPaper) -> Unit,
    onDownloadWebUrl: (String) -> Unit,
    onDownloadSample: (String) -> Unit,
    onDeletePaper: (PastPaper) -> Unit = {},
    isDownloadingFromWeb: Boolean = false,
    downloadProgressText: String = "",
    fileCloseTrigger: Int = 0
) {
    // 0: Web Portal (ReVanced Manager Style), 1: My Downloaded Papers Library
    var selectedTab by remember { mutableIntStateOf(1) }
    var previewPaper by remember { mutableStateOf<PastPaper?>(null) }
    var isViewingSchemeInModal by remember { mutableStateOf(false) }
    var isPortalFullScreen by remember { mutableStateOf(false) }

    // Close preview modal whenever focus or quiz timer expires
    LaunchedEffect(fileCloseTrigger) {
        if (fileCloseTrigger > 0) {
            previewPaper = null
            isViewingSchemeInModal = false
        }
    }

    val subjects = listOf("All", "Combined Mathematics", "Physics", "Chemistry", "Biology", "ICT")

    val filteredPapers = papers.filter { paper ->
        val matchesSubject = selectedSubject == "All" || paper.subject.equals(selectedSubject, ignoreCase = true)
        val matchesQuery = searchQuery.isBlank() ||
                paper.title.contains(searchQuery, ignoreCase = true) ||
                paper.year.toString().contains(searchQuery) ||
                paper.paperType.contains(searchQuery, ignoreCase = true)
        matchesSubject && matchesQuery
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Top Mode Switcher (ReVanced Portal vs Downloaded Library) ---
        AnimatedVisibility(visible = !isPortalFullScreen || selectedTab != 0) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = {
                            Text(
                                text = "Web Portal",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("tab_web_portal")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Downloaded Papers",
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.padding(start = 2.dp)
                                ) {
                                    Text(
                                        text = "${papers.size}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.testTag("tab_downloaded_papers")
                    )
                }
            }
        }

        // --- Content View according to selected tab ---
        when (selectedTab) {
            0 -> {
                // ReVanced Manager Style Sri Lanka Past Papers Web Portal & Downloader
                PastPapersWebPortal(
                    downloadedPapers = papers,
                    onDeletePaper = onDeletePaper,
                    onPaperDownloaded = { paper, file ->
                        // Switch to library view or PDF viewer
                        onOpenPdfViewer(paper)
                    },
                    onOneTapDownloadSample = { sampleIdOrUrl ->
                        if (sampleIdOrUrl.startsWith("http://") || sampleIdOrUrl.startsWith("https://")) {
                            onDownloadWebUrl(sampleIdOrUrl)
                        } else {
                            onDownloadSample(sampleIdOrUrl)
                        }
                    },
                    isDownloading = isDownloadingFromWeb,
                    downloadProgressText = downloadProgressText,
                    isFullScreen = isPortalFullScreen,
                    onToggleFullScreen = { isPortalFullScreen = !isPortalFullScreen },
                    modifier = Modifier.fillMaxSize()
                )
            }

            1 -> {
                // Downloaded Papers Library & PDF Viewer Launcher
                Column(modifier = Modifier.fillMaxSize()) {
                    // Quick Action Banner to open Web Portal
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTab = 0 }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Download More A/L Papers",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Open IDNS, DoENets & e-Thaksalawa portals with in-app downloader",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = { selectedTab = 0 },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Open Portal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Search by year, topic, or paper type...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("papers_search_bar"),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    // Subject Filter Chips
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(subjects) { subj ->
                            FilterChip(
                                selected = selectedSubject == subj,
                                onClick = { onSubjectChange(subj) },
                                label = { Text(subj, fontSize = 13.sp) },
                                modifier = Modifier.testTag("subject_chip_$subj")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Papers List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredPapers, key = { it.id }) { paper ->
                            PastPaperCard(
                                paper = paper,
                                onOpenPdf = { onOpenPdfViewer(paper) },
                                onPreview = {
                                    previewPaper = paper
                                    isViewingSchemeInModal = false
                                },
                                onToggleCache = { onToggleCache(paper) },
                                onShareToChat = { onShareToChat(paper) },
                                onStartFocus = { onStartFocus(paper) },
                                onDelete = { onDeletePaper(paper) }
                            )
                        }

                        if (filteredPapers.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "No papers found for \"$selectedSubject\".",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        ElevatedButton(onClick = { selectedTab = 0 }) {
                                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Download from Sri Lanka Exam Portals")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Zoomable Text Document Viewer Dialog for Past Papers (Secondary Preview)
    if (previewPaper != null) {
        val paper = previewPaper!!
        AlertDialog(
            onDismissRequest = { previewPaper = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isViewingSchemeInModal) "Marking Scheme: ${paper.title}" else paper.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pinch to zoom, pan & scroll. Text is selectable.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (paper.markingSchemeContent.isNotBlank()) {
                            TextButton(onClick = { isViewingSchemeInModal = !isViewingSchemeInModal }) {
                                Text(if (isViewingSchemeInModal) "View Questions" else "View Scheme", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val displayContent = if (isViewingSchemeInModal && paper.markingSchemeContent.isNotBlank()) {
                        paper.markingSchemeContent
                    } else {
                        paper.officialExamContent.ifBlank { "Full Question Paper Loaded:\n\n${paper.title}\nSubject: ${paper.subject}\nMedium: ${paper.medium}" }
                    }

                    ZoomableDocumentViewer(
                        title = if (isViewingSchemeInModal) "Official Marking Scheme" else paper.title,
                        content = displayContent,
                        isMarkingScheme = isViewingSchemeInModal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row {
                    FilledTonalButton(onClick = {
                        onOpenPdfViewer(paper)
                        previewPaper = null
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open Native PDF")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        onStartFocus(paper)
                        previewPaper = null
                    }) {
                        Text("Practice in Focus Room")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { previewPaper = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun PastPaperCard(
    paper: PastPaper,
    onOpenPdf: () -> Unit,
    onPreview: () -> Unit,
    onToggleCache: () -> Unit,
    onShareToChat: () -> Unit,
    onStartFocus: () -> Unit,
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("paper_card_${paper.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = paper.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = paper.subject,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${paper.year} • ${paper.paperType} • ${paper.fileSizeMb} MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onToggleCache,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (paper.isCached) Icons.Default.DownloadDone else Icons.Default.CloudDownload,
                        contentDescription = "Cache paper",
                        tint = if (paper.isCached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row with Native PDF Viewer button as primary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onOpenPdf,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("open_pdf_btn_${paper.id}")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF Tool", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onPreview,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.0f)
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Zoom", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onStartFocus,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Focus", fontSize = 11.sp)
                }

                IconButton(
                    onClick = onShareToChat,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Log paper to squad chat", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
