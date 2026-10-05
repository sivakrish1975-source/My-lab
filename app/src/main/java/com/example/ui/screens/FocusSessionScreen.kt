package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CloudFile
import com.example.data.model.PastPaper
import com.example.ui.components.FocusRoomPdfViewer
import com.example.ui.components.ZoomableDocumentViewer
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSessionScreen(
    isTimerRunning: Boolean,
    remainingSeconds: Int,
    totalSeconds: Int,
    selectedPaper: PastPaper?,
    companionScheme: CloudFile?,
    actionOnTimeout: String,
    ambientSound: String,
    activeDocumentTitle: String?,
    activeDocumentContent: String,
    isViewingSchemeNow: Boolean,
    availablePapers: List<PastPaper>,
    availableFiles: List<CloudFile>,
    activePdfFile: File? = null,
    isWidgetFloating: Boolean = false,
    onToggleFloatingWidget: () -> Unit = {},
    onOpenFullPdf: ((File) -> Unit)? = null,
    onConfigureSession: (Int, PastPaper?, CloudFile?, String, String) -> Unit,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResetTimer: () -> Unit
) {
    val context = LocalContext.current
    var selectedDurationMinutes by remember { mutableIntStateOf(totalSeconds / 60) }
    var chosenPaper by remember { mutableStateOf<PastPaper?>(selectedPaper ?: availablePapers.firstOrNull()) }
    var chosenScheme by remember {
        mutableStateOf<CloudFile?>(companionScheme ?: availableFiles.firstOrNull { it.isScheme })
    }
    var chosenAction by remember { mutableStateOf(actionOnTimeout) }
    var chosenSound by remember { mutableStateOf(ambientSound) }
    var previewMode by remember { mutableStateOf(if (activePdfFile != null && activePdfFile.exists()) "PDF" else "TEXT") }

    var paperDropdownExpanded by remember { mutableStateOf(false) }
    var schemeDropdownExpanded by remember { mutableStateOf(false) }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    val progress = if (totalSeconds > 0) (remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f) else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Exam Focus Room",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Pinch to zoom, pan, select text, and auto-transition to companion scheme on timeout",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Timer Display Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(170.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 9.dp,
                        color = if (remainingSeconds < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 38.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("focus_timer_display")
                        )
                        Text(
                            text = if (isTimerRunning) "Exam In Progress" else if (remainingSeconds == 0) "Time Out!" else "Paused",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Timer Action Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onResetTimer,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("focus_reset_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset timer")
                    }

                    Button(
                        onClick = {
                            if (isTimerRunning) onPauseTimer() else onStartTimer()
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .width(150.dp)
                            .testTag("focus_toggle_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTimerRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isTimerRunning) "Pause" else "Start Exam")
                    }

                    // Quick test 10s shortcut
                    OutlinedButton(
                        onClick = {
                            onConfigureSession(1, chosenPaper, chosenScheme, chosenAction, chosenSound)
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(46.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("1m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Movable / Floating Accessibility Tool Button
                OutlinedButton(
                    onClick = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                            !android.provider.Settings.canDrawOverlays(context) && !isWidgetFloating) {
                            try {
                                val intent = android.content.Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                                android.widget.Toast.makeText(
                                    context,
                                    "Allow 'Display over other apps' so the timer floats on your home screen",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } catch (_: Exception) {}
                        }
                        onToggleFloatingWidget()
                    },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("float_widget_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isWidgetFloating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isWidgetFloating) "Floating Tool Active" else "Float On-Screen Tool",
                        fontSize = 12.sp,
                        fontWeight = if (isWidgetFloating) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // REAL HIGH-RES PDF & DOCUMENT VIEWER (Embedded directly into the Focus Session)
        if (activeDocumentTitle != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isViewingSchemeNow) "📑 Official Marking Scheme:" else "📝 Active Examination Paper:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isViewingSchemeNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )

                if (activePdfFile != null && activePdfFile.exists()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = previewMode == "PDF",
                            onClick = { previewMode = "PDF" },
                            label = { Text("PDF View", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = previewMode == "TEXT",
                            onClick = { previewMode = "TEXT" },
                            label = { Text("Text View", fontSize = 11.sp) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            if (previewMode == "PDF" && activePdfFile != null && activePdfFile.exists()) {
                FocusRoomPdfViewer(
                    pdfFile = activePdfFile,
                    title = activeDocumentTitle,
                    onOpenFullViewer = { onOpenFullPdf?.invoke(activePdfFile) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ZoomableDocumentViewer(
                    title = activeDocumentTitle,
                    content = activeDocumentContent,
                    isMarkingScheme = isViewingSchemeNow,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No document currently opened. Select a paper or uploaded file below to study!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Session Settings & Scheme Pairing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Presets
                Text("Select Timer Duration:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(15, 30, 45, 60, 180).forEach { mins ->
                        FilterChip(
                            selected = selectedDurationMinutes == mins,
                            onClick = {
                                selectedDurationMinutes = mins
                                onConfigureSession(mins, chosenPaper, chosenScheme, chosenAction, chosenSound)
                            },
                            label = { Text("${mins}m") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Question Paper Selector
                ExposedDropdownMenuBox(
                    expanded = paperDropdownExpanded,
                    onExpandedChange = { paperDropdownExpanded = !paperDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = chosenPaper?.title ?: "Select Paper to Study",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Question Paper") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paperDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = paperDropdownExpanded,
                        onDismissRequest = { paperDropdownExpanded = false }
                    ) {
                        availablePapers.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("${p.year} ${p.subject} (${p.paperType})") },
                                onClick = {
                                    chosenPaper = p
                                    paperDropdownExpanded = false
                                    onConfigureSession(selectedDurationMinutes, p, chosenScheme, chosenAction, chosenSound)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Companion Scheme Selector
                ExposedDropdownMenuBox(
                    expanded = schemeDropdownExpanded,
                    onExpandedChange = { schemeDropdownExpanded = !schemeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = chosenScheme?.fileName ?: "Companion Marking Scheme",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Companion File Flagged as Scheme") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = schemeDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = schemeDropdownExpanded,
                        onDismissRequest = { schemeDropdownExpanded = false }
                    ) {
                        availableFiles.filter { it.isScheme }.forEach { f ->
                            DropdownMenuItem(
                                text = { Text("${f.fileName} (${f.subject})") },
                                onClick = {
                                    chosenScheme = f
                                    schemeDropdownExpanded = false
                                    onConfigureSession(selectedDurationMinutes, chosenPaper, f, chosenAction, chosenSound)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action on Timeout
                Text("Action on Timeout:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = chosenAction == "SWITCH_TO_SCHEME",
                        onClick = {
                            chosenAction = "SWITCH_TO_SCHEME"
                            onConfigureSession(selectedDurationMinutes, chosenPaper, chosenScheme, "SWITCH_TO_SCHEME", chosenSound)
                        }
                    )
                    Text("Auto-Open Flagged Marking Scheme for Self-Evaluation", style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = chosenAction == "CLOSE_PAPER",
                        onClick = {
                            chosenAction = "CLOSE_PAPER"
                            onConfigureSession(selectedDurationMinutes, chosenPaper, chosenScheme, "CLOSE_PAPER", chosenSound)
                        }
                    )
                    Text("Automatically Close Question Paper", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Ambient Study Sound
                Text("Ambient Focus Sound:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("None", "Library", "Rain", "White Noise").forEach { sound ->
                        FilterChip(
                            selected = chosenSound == sound,
                            onClick = {
                                chosenSound = sound
                                onConfigureSession(selectedDurationMinutes, chosenPaper, chosenScheme, chosenAction, sound)
                            },
                            label = { Text(sound) },
                            leadingIcon = if (chosenSound == sound && sound != "None") {
                                { Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
