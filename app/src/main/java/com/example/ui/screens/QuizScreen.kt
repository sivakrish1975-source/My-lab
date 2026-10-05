package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuizGenerationParams
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizScoreReport
import com.example.data.repository.QuizBankUtil
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseError
import java.util.Locale

/**
 * Enhanced dialog to configure and filter quiz generation by:
 * - Subject (Combined Mathematics, Physics, Chemistry, Biology, ICT, All)
 * - Units (dynamically populated based on chosen subject)
 * - Question Types (Standard MCQ, Numerical/Calculation, Concept & Theory, Assertion & Reason, All)
 * - Number of Questions (3, 5, 10, 15, 20, 25)
 * - Time Limit & Countdown (Untimed, 3m, 5m, 10m, 15m, 20m, 30m)
 */
@Composable
fun EnhancedQuizFilterDialog(
    initialSubject: String,
    initialUnit: String,
    initialType: String,
    initialCount: Int,
    initialTimeLimit: Int,
    onDismiss: () -> Unit,
    onGenerateFromInternet: (QuizGenerationParams) -> Unit,
    onGeneratePractice: (QuizGenerationParams) -> Unit
) {
    var subject by remember { mutableStateOf(initialSubject) }
    var unit by remember { mutableStateOf(initialUnit) }
    var questionType by remember { mutableStateOf(initialType) }
    var count by remember { mutableIntStateOf(initialCount) }
    var timeLimit by remember { mutableIntStateOf(initialTimeLimit) }

    val subjects = listOf("All", "Physics", "Chemistry", "Combined Mathematics", "Biology", "ICT")
    val unitsForSubject = listOf("All Units") + (QuizBankUtil.SUBJECT_UNITS[subject]?.filter { it != "All Units" } ?: emptyList())
    val questionTypes = QuizBankUtil.QUESTION_TYPES
    val questionCounts = listOf(3, 5, 10, 15, 20, 25)
    val timeLimits = listOf(
        0 to "Untimed",
        3 to "3 min",
        5 to "5 min",
        10 to "10 min",
        15 to "15 min",
        20 to "20 min",
        30 to "30 min"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Customize Quiz Generation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Filter by Subject, Units, Types, Count & Timer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Subject
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("1. Subject", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(subjects) { subj ->
                        FilterChip(
                            selected = subject == subj,
                            onClick = {
                                subject = subj
                                unit = "All Units"
                            },
                            label = { Text(subj, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Unit / Syllabus Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("2. Syllabus Units (${subject})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(unitsForSubject) { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Question Types
                Text("3. Question Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(questionTypes) { qType ->
                        FilterChip(
                            selected = questionType == qType,
                            onClick = { questionType = qType },
                            label = { Text(qType, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Number of Questions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("4. Question Count", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("$count Questions", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(questionCounts) { c ->
                        FilterChip(
                            selected = count == c,
                            onClick = { count = c },
                            label = { Text("$c Qs", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Time Limit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("5. Time Limit & Countdown", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        if (timeLimit > 0) "$timeLimit min (${String.format(Locale.getDefault(), "%.1f", (timeLimit.toFloat() / count))} min/Q)" else "Untimed",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(timeLimits) { (mins, label) ->
                        FilterChip(
                            selected = timeLimit == mins,
                            onClick = { timeLimit = mins },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val params = QuizGenerationParams(
                            subject = subject,
                            unit = unit,
                            questionType = questionType,
                            questionCount = count,
                            timeLimitMinutes = timeLimit
                        )
                        onGenerateFromInternet(params)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate from Internet (AI / Net)")
                }

                FilledTonalButton(
                    onClick = {
                        val params = QuizGenerationParams(
                            subject = subject,
                            unit = unit,
                            questionType = questionType,
                            questionCount = count,
                            timeLimitMinutes = timeLimit
                        )
                        onGeneratePractice(params)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate Practice Quiz (Bank)")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun QuizScreen(
    questions: List<QuizQuestion>,
    currentIndex: Int,
    selectedOption: Int?,
    isAnswerSubmitted: Boolean,
    scoreReport: QuizScoreReport?,
    selectedSubject: String,
    selectedUnit: String = "All Units",
    selectedType: String = "All Types",
    questionCount: Int = 5,
    timeLimitMinutes: Int = 5,
    remainingSeconds: Int = 5 * 60,
    isTimerRunning: Boolean = false,
    isGeneratingFromInternet: Boolean = false,
    onSelectSubject: (String) -> Unit,
    onSelectOption: (Int) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onRestartQuiz: () -> Unit,
    onGenerateNewFromInternet: () -> Unit = {},
    onGenerateCustomQuiz: (QuizGenerationParams) -> Unit = {},
    onGenerateCustomFromInternet: (QuizGenerationParams) -> Unit = {}
) {
    val subjects = listOf("All", "Physics", "Biology", "Chemistry", "Combined Mathematics", "ICT")
    val context = LocalContext.current
    var showFilterDialog by remember { mutableStateOf(false) }

    if (showFilterDialog) {
        EnhancedQuizFilterDialog(
            initialSubject = if (selectedSubject in subjects) selectedSubject else "All",
            initialUnit = selectedUnit,
            initialType = selectedType,
            initialCount = questionCount,
            initialTimeLimit = timeLimitMinutes,
            onDismiss = { showFilterDialog = false },
            onGenerateFromInternet = { params ->
                onGenerateCustomFromInternet(params)
            },
            onGeneratePractice = { params ->
                onGenerateCustomQuiz(params)
            }
        )
    }

    if (scoreReport != null) {
        // --- SCORE ANALYSING SECTION ---
        ScoreAnalysisView(
            report = scoreReport,
            isGenerating = isGeneratingFromInternet,
            onGenerateNewFromInternet = onGenerateNewFromInternet,
            onOpenFilterDialog = { showFilterDialog = true },
            onRestart = onRestartQuiz
        )
        return
    }

    val currentQ = questions.getOrNull(currentIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Subject Selector & Enhanced Filter Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "A/L Quiz Practice",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Prominent Countdown Timer Badge if quiz has a time limit
                            if (timeLimitMinutes > 0 && isTimerRunning) {
                                Spacer(modifier = Modifier.width(8.dp))
                                val mins = remainingSeconds / 60
                                val secs = remainingSeconds % 60
                                val timeText = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (remainingSeconds < 60) MaterialTheme.colorScheme.errorContainer else if (remainingSeconds < 120) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.primaryContainer,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (remainingSeconds < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = "Countdown Timer",
                                            tint = if (remainingSeconds < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = timeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (remainingSeconds < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Filter by subject, units, types, count & time limit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // "Customize / Filter" Button
                        FilledTonalButton(
                            onClick = { showFilterDialog = true },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("customize_quiz_filters_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Customize",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Filters", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Button(
                            onClick = onGenerateNewFromInternet,
                            enabled = !isGeneratingFromInternet,
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("quick_generate_quiz_button")
                        ) {
                            if (isGeneratingFromInternet) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("...", fontSize = 11.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Net", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Subject Selector Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(subjects) { subj ->
                        FilterChip(
                            selected = selectedSubject == subj,
                            onClick = { onSelectSubject(subj) },
                            label = { Text(subj, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Active Filter Summary Badges (1-Tap to adjust)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFilterDialog = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Text(
                            text = "Unit: $selectedUnit",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Text(
                            text = "Type: $selectedType",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Text(
                            text = "$questionCount Qs • ${if (timeLimitMinutes > 0) "${timeLimitMinutes}m" else "Untimed"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = isGeneratingFromInternet) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Generating fresh questions from internet...",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (currentQ == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No questions found for this subject filter.")
            }
            return
        }

        // Progress bar
        val progress = (currentIndex + 1).toFloat() / questions.size
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // Question Content (Scrollable)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Question ${currentIndex + 1} of ${questions.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${currentQ.subject} • ${currentQ.topic}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Question Text
            Text(
                text = currentQ.questionText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Visual Question Picture / Diagram if present!
            if (currentQ.drawableResName != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val imageResId = context.resources.getIdentifier(
                    currentQ.drawableResName,
                    "drawable",
                    context.packageName
                )
                if (imageResId != 0) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Image(
                                painter = painterResource(id = imageResId),
                                contentDescription = "Question Diagram Illustration",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Figure: Scientific Diagram for Question ${currentIndex + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5 Options for A/L
            currentQ.options.forEachIndexed { optIndex, optText ->
                val isSelected = selectedOption == optIndex
                val isCorrect = optIndex == currentQ.correctIndex

                val borderColor = when {
                    isAnswerSubmitted && isCorrect -> EmeraldGreen
                    isAnswerSubmitted && isSelected && !isCorrect -> RoseError
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outlineVariant
                }

                val bgColor = when {
                    isAnswerSubmitted && isCorrect -> EmeraldGreen.copy(alpha = 0.12f)
                    isAnswerSubmitted && isSelected && !isCorrect -> RoseError.copy(alpha = 0.12f)
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else -> MaterialTheme.colorScheme.surface
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (isSelected || (isAnswerSubmitted && isCorrect)) 2.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = !isAnswerSubmitted) {
                            onSelectOption(optIndex)
                        }
                        .testTag("quiz_option_$optIndex"),
                    color = bgColor
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "(${optIndex + 1})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = optText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        if (isAnswerSubmitted) {
                            if (isCorrect) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Correct",
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Incorrect",
                                    tint = RoseError,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Explanation Card upon submit
            AnimatedVisibility(visible = isAnswerSubmitted) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Marking Scheme & Rationale:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentQ.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Bottom Action Bar
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isAnswerSubmitted) {
                    Button(
                        onClick = onSubmitAnswer,
                        enabled = selectedOption != null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("quiz_submit_button")
                    ) {
                        Text("Confirm Answer", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onNextQuestion,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("quiz_next_button")
                    ) {
                        Text(
                            text = if (currentIndex < questions.size - 1) "Next Question →" else "Finish & View Score Analysis",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreAnalysisView(
    report: QuizScoreReport,
    isGenerating: Boolean = false,
    onGenerateNewFromInternet: () -> Unit = {},
    onOpenFilterDialog: () -> Unit = {},
    onRestart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Diagnostic Score Analysis",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Comprehensive performance diagnostic for ${report.subject}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Big Accuracy Score Gauge Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(140.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { report.accuracy / 100f },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 10.dp,
                        color = if (report.accuracy >= 75) EmeraldGreen else if (report.accuracy >= 50) Color(0xFFF59E0B) else RoseError,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${report.accuracy.toInt()}%",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${report.score} / ${report.total} Correct",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Speed", style = MaterialTheme.typography.labelSmall)
                        }
                        Text("${report.timeSeconds / report.total}s / question", fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Accuracy Rating", style = MaterialTheme.typography.labelSmall)
                        }
                        Text(if (report.accuracy >= 75) "A-Grade" else if (report.accuracy >= 50) "B-Grade" else "Needs Revision", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Diagnostic Breakdown: Strengths vs Weak Topics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Strong topics
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "✅ Strong Topics",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (report.strongTopics.isEmpty()) {
                        Text("None identified", style = MaterialTheme.typography.bodySmall)
                    } else {
                        report.strongTopics.forEach { topic ->
                            Text("• $topic", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Weak topics
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = RoseError.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "⚠️ Weak Topics",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = RoseError
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (report.weakTopics.isEmpty()) {
                        Text("No weak areas detected! Excellent work.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        report.weakTopics.forEach { topic ->
                            Text("• $topic", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Question-by-Question Review
        Text(
            text = "Detailed Question Review:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        report.questionAnswers.forEachIndexed { i, rev ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Q${i + 1}: ${rev.question.topic}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (rev.isCorrect) "CORRECT (+1)" else "INCORRECT (0)",
                            color = if (rev.isCorrect) EmeraldGreen else RoseError,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rev.question.questionText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your choice: ${rev.question.options.getOrNull(rev.selectedIndex) ?: "None"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (rev.isCorrect) EmeraldGreen else RoseError
                    )
                    if (!rev.isCorrect) {
                        Text(
                            text = "Correct answer: ${rev.question.options[rev.question.correctIndex]}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGreen
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Prominent button: Generate fresh new quiz from internet
        Button(
            onClick = onGenerateNewFromInternet,
            enabled = !isGenerating,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("generate_new_quiz_button")
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Generating Fresh Quiz from Internet...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("🌐 Generate New Quiz from Internet", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        FilledTonalButton(
            onClick = onOpenFilterDialog,
            enabled = !isGenerating,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("change_quiz_filters_button")
        ) {
            Icon(Icons.Default.Tune, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("⚙️ Customize Filters & Generate")
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onRestart,
            enabled = !isGenerating,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("retake_quiz_button")
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Practice Again (Replay This Set)")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
