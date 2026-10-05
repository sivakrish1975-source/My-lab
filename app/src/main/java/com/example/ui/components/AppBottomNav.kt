package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.StudyScreen

@Composable
fun AppBottomNav(
    currentScreen: StudyScreen,
    onScreenSelected: (StudyScreen) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("app_bottom_navigation")
    ) {
        NavigationBarItem(
            selected = currentScreen == StudyScreen.CHAT,
            onClick = { onScreenSelected(StudyScreen.CHAT) },
            icon = { Icon(Icons.Default.Chat, contentDescription = "Squad Chat") },
            label = { Text("Chat") },
            modifier = Modifier.testTag("tab_chat")
        )
        NavigationBarItem(
            selected = currentScreen == StudyScreen.PAPERS,
            onClick = { onScreenSelected(StudyScreen.PAPERS) },
            icon = { Icon(Icons.Default.Description, contentDescription = "Past Papers") },
            label = { Text("Papers") },
            modifier = Modifier.testTag("tab_papers")
        )
        NavigationBarItem(
            selected = currentScreen == StudyScreen.CLOUD_FILES,
            onClick = { onScreenSelected(StudyScreen.CLOUD_FILES) },
            icon = { Icon(Icons.Default.CloudUpload, contentDescription = "Cloud Files") },
            label = { Text("Files") },
            modifier = Modifier.testTag("tab_files")
        )
        NavigationBarItem(
            selected = currentScreen == StudyScreen.FOCUS_SESSION,
            onClick = { onScreenSelected(StudyScreen.FOCUS_SESSION) },
            icon = { Icon(Icons.Default.Timer, contentDescription = "Focus Session") },
            label = { Text("Focus") },
            modifier = Modifier.testTag("tab_focus")
        )
        NavigationBarItem(
            selected = currentScreen == StudyScreen.QUIZ,
            onClick = { onScreenSelected(StudyScreen.QUIZ) },
            icon = { Icon(Icons.Default.Quiz, contentDescription = "Interactive Quiz") },
            label = { Text("Quiz") },
            modifier = Modifier.testTag("tab_quiz")
        )
        NavigationBarItem(
            selected = currentScreen == StudyScreen.GEMINI_AI,
            onClick = { onScreenSelected(StudyScreen.GEMINI_AI) },
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Gemini AI Tutor") },
            label = { Text("AI Tutor") },
            modifier = Modifier.testTag("tab_ai_tutor")
        )
    }
}
