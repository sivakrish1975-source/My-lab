package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.StudyScreen
import com.example.ui.StudyViewModel
import com.example.ui.components.AppBottomNav
import com.example.ui.components.AppTopBar
import com.example.ui.components.FloatingFocusWidget
import com.example.ui.components.PdfViewerDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CloudFilesScreen
import com.example.ui.screens.ConnectionDetailsScreen
import com.example.ui.screens.FocusSessionScreen
import com.example.ui.screens.GeminiAiScreen
import com.example.ui.screens.PastPapersScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var mainViewModel: StudyViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: StudyViewModel = viewModel()
            mainViewModel = viewModel
            val accentTheme by viewModel.accentTheme.collectAsStateWithLifecycle()

            // Handle initial launch with timeout intent
            LaunchedEffect(intent) {
                if (intent.getBooleanExtra("EXTRA_FOCUS_TIMEOUT", false)) {
                    viewModel.navigateTo(StudyScreen.FOCUS_SESSION)
                }
            }

            MyApplicationTheme(accentTheme = accentTheme) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("EXTRA_FOCUS_TIMEOUT", false)) {
            mainViewModel?.navigateTo(StudyScreen.FOCUS_SESSION)
        }
    }
}

@Composable
fun MainAppContent(viewModel: StudyViewModel) {
    val isSplashActive by viewModel.isSplashActive.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val showConnectionDetails by viewModel.showConnectionDetails.collectAsStateWithLifecycle()
    val showSettings by viewModel.showSettings.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val latencyMs by viewModel.latencyMs.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val isPdfViewerOpen by viewModel.isPdfViewerOpen.collectAsStateWithLifecycle()
    val activePdfFile by viewModel.activePdfFile.collectAsStateWithLifecycle()
    val activePdfTitle by viewModel.activePdfTitle.collectAsStateWithLifecycle()
    val isDownloadingFromWeb by viewModel.isDownloadingFromWeb.collectAsStateWithLifecycle()
    val downloadProgressStatus by viewModel.downloadProgressStatus.collectAsStateWithLifecycle()
    val fileCloseTrigger by viewModel.fileCloseTrigger.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearSnackbar()
        }
    }

    // 1. Animated Splash Screen on Launch
    if (isSplashActive) {
        SplashScreen(
            onSplashFinished = { viewModel.finishSplash() }
        )
        return
    }

    // 2. Authentication Gate: If no persistent session, show Sign In / Sign Up
    if (currentUser == null) {
        AuthScreen(
            onSignIn = { email, pass -> viewModel.signIn(email, pass) },
            onSignUp = { name, email, pass, stream -> viewModel.signUp(name, email, pass, stream) },
            onSignInWithGoogle = { viewModel.signInWithGoogle() },
            onRequestRecovery = { email -> viewModel.requestPasswordRecovery(email) }
        )
        return
    }

    // 3. Overlays: Connection & Telemetry Details
    if (showConnectionDetails) {
        BackHandler { viewModel.setConnectionDetailsVisible(false) }
        ConnectionDetailsScreen(
            currentUser = currentUser,
            latencyMs = latencyMs,
            isOffline = isOffline,
            onBack = { viewModel.setConnectionDetailsVisible(false) }
        )
        return
    }

    // 4. Overlays: Settings & Profile
    if (showSettings) {
        BackHandler { viewModel.setSettingsVisible(false) }
        val currentTheme by viewModel.accentTheme.collectAsStateWithLifecycle()
        val lastSync by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()

        SettingsScreen(
            currentUser = currentUser,
            isOffline = isOffline,
            currentTheme = currentTheme,
            lastSyncTime = lastSync,
            onBack = { viewModel.setSettingsVisible(false) },
            onUpdateProfile = { name, photoUri, stream -> viewModel.updateProfile(name, photoUri, stream) },
            onSendVerificationEmail = { viewModel.sendVerificationEmail() },
            onRequestPasswordRecovery = { email -> viewModel.requestPasswordRecovery(email) },
            onSignOut = { viewModel.signOut() },
            onToggleOffline = { viewModel.toggleOfflineMode() },
            onSetTheme = { theme -> viewModel.setAccentTheme(theme) },
            onSyncNow = { viewModel.syncMultiDevice() },
            onClearCache = { viewModel.clearStorageCache() }
        )
        return
    }

    // Back handling for main screen navigation
    BackHandler(enabled = currentScreen != StudyScreen.PAPERS) {
        viewModel.navigateTo(StudyScreen.PAPERS)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                currentUser = currentUser,
                latencyMs = latencyMs,
                isOffline = isOffline,
                onOpenConnectionDetails = { viewModel.setConnectionDetailsVisible(true) },
                onOpenSettings = { viewModel.setSettingsVisible(true) },
                onAvatarClick = { viewModel.setSettingsVisible(true) }
            )
        },
        bottomBar = {
            AppBottomNav(
                currentScreen = currentScreen,
                onScreenSelected = { viewModel.navigateTo(it) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                StudyScreen.CHAT -> {
                    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()

                    ChatScreen(
                        currentUser = currentUser,
                        messages = messages,
                        onSendMessage = { text -> viewModel.sendChatMessage(text) },
                        onOpenPaper = { paperId ->
                            viewModel.navigateTo(StudyScreen.PAPERS)
                        }
                    )
                }

                StudyScreen.PAPERS -> {
                    val papers by viewModel.pastPapers.collectAsStateWithLifecycle()
                    val selectedSubj by viewModel.selectedSubjectFilter.collectAsStateWithLifecycle()
                    val searchQuery by viewModel.paperSearchQuery.collectAsStateWithLifecycle()
                    val isFetching by viewModel.isFetchingPapers.collectAsStateWithLifecycle()

                    PastPapersScreen(
                        papers = papers,
                        selectedSubject = selectedSubj,
                        searchQuery = searchQuery,
                        isFetching = isFetching,
                        onSubjectChange = { viewModel.setPaperSubjectFilter(it) },
                        onSearchChange = { viewModel.setPaperSearchQuery(it) },
                        onAutoFetch = { viewModel.autoFetchPapers() },
                        onToggleCache = { viewModel.toggleCachePaper(it) },
                        onShareToChat = { viewModel.sharePaperInChat(it) },
                        onStartFocus = { paper ->
                            viewModel.configureFocusSession(
                                durationMinutes = 45,
                                paper = paper,
                                companionScheme = null,
                                actionOnTimeout = "SWITCH_TO_SCHEME",
                                sound = "Library"
                            )
                            viewModel.navigateTo(StudyScreen.FOCUS_SESSION)
                        },
                        onOpenPdfViewer = { paper -> viewModel.openPdfViewerForPaper(paper) },
                        onDownloadWebUrl = { url -> viewModel.downloadFromWebUrl(url) },
                        onDownloadSample = { sampleId -> viewModel.downloadSamplePaper(sampleId) },
                        isDownloadingFromWeb = isDownloadingFromWeb,
                        downloadProgressText = downloadProgressStatus,
                        fileCloseTrigger = fileCloseTrigger
                    )
                }

                StudyScreen.CLOUD_FILES -> {
                    val files by viewModel.cloudFiles.collectAsStateWithLifecycle()

                    CloudFilesScreen(
                        currentUser = currentUser,
                        files = files,
                        onUploadRealFile = { uri, subj, type, isScheme ->
                            viewModel.uploadRealFile(uri, subj, type, isScheme)
                        },
                        onDeleteFile = { file -> viewModel.deleteCloudFile(file) },
                        onOpenInFocus = { file -> viewModel.openUploadedFileInFocus(file) },
                        onOpenInPdf = { file -> viewModel.openPdfViewerForCloudFile(file) }
                    )
                }

                StudyScreen.FOCUS_SESSION -> {
                    val isRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
                    val remaining by viewModel.focusRemainingSeconds.collectAsStateWithLifecycle()
                    val total by viewModel.focusTotalSeconds.collectAsStateWithLifecycle()
                    val selectedPaper by viewModel.focusSelectedPaper.collectAsStateWithLifecycle()
                    val companionScheme by viewModel.focusCompanionScheme.collectAsStateWithLifecycle()
                    val actionOnTimeout by viewModel.focusActionOnTimeout.collectAsStateWithLifecycle()
                    val ambientSound by viewModel.ambientSound.collectAsStateWithLifecycle()
                    val activeDocTitle by viewModel.activeViewingDocumentTitle.collectAsStateWithLifecycle()
                    val activeDocContent by viewModel.activeViewingDocumentContent.collectAsStateWithLifecycle()
                    val isViewingScheme by viewModel.isViewingSchemeNow.collectAsStateWithLifecycle()
                    val availablePapers by viewModel.pastPapers.collectAsStateWithLifecycle()
                    val availableFiles by viewModel.cloudFiles.collectAsStateWithLifecycle()
                    val focusPdf by viewModel.focusPdfFile.collectAsStateWithLifecycle()
                    val isWidgetFloating by viewModel.isFocusWidgetFloating.collectAsStateWithLifecycle()

                    FocusSessionScreen(
                        isTimerRunning = isRunning,
                        remainingSeconds = remaining,
                        totalSeconds = total,
                        selectedPaper = selectedPaper,
                        companionScheme = companionScheme,
                        actionOnTimeout = actionOnTimeout,
                        ambientSound = ambientSound,
                        activeDocumentTitle = activeDocTitle,
                        activeDocumentContent = activeDocContent,
                        isViewingSchemeNow = isViewingScheme,
                        availablePapers = availablePapers,
                        availableFiles = availableFiles,
                        activePdfFile = focusPdf,
                        isWidgetFloating = isWidgetFloating,
                        onToggleFloatingWidget = { viewModel.setFocusWidgetFloating(!isWidgetFloating) },
                        onOpenFullPdf = { file -> viewModel.openPdfViewerForFile(file, activeDocTitle ?: "Examination Document") },
                        onConfigureSession = { mins, p, s, action, sound ->
                            viewModel.configureFocusSession(mins, p, s, action, sound)
                        },
                        onStartTimer = { viewModel.startFocusTimer() },
                        onPauseTimer = { viewModel.pauseFocusTimer() },
                        onResetTimer = { viewModel.resetFocusTimer() }
                    )
                }

                StudyScreen.QUIZ -> {
                    val questions by viewModel.quizQuestions.collectAsStateWithLifecycle()
                    val currentIndex by viewModel.currentQuizIndex.collectAsStateWithLifecycle()
                    val selectedOption by viewModel.selectedQuizOption.collectAsStateWithLifecycle()
                    val isAnswerSubmitted by viewModel.isAnswerSubmitted.collectAsStateWithLifecycle()
                    val scoreReport by viewModel.quizScoreReport.collectAsStateWithLifecycle()
                    val selectedSubj by viewModel.quizSubjectFilter.collectAsStateWithLifecycle()
                    val selectedUnit by viewModel.quizUnitFilter.collectAsStateWithLifecycle()
                    val selectedType by viewModel.quizTypeFilter.collectAsStateWithLifecycle()
                    val questionCount by viewModel.quizQuestionCount.collectAsStateWithLifecycle()
                    val timeLimitMinutes by viewModel.quizTimeLimitMinutes.collectAsStateWithLifecycle()
                    val remainingSeconds by viewModel.quizRemainingSeconds.collectAsStateWithLifecycle()
                    val isTimerRunning by viewModel.isQuizTimerRunning.collectAsStateWithLifecycle()
                    val isGeneratingQuiz by viewModel.isGeneratingQuiz.collectAsStateWithLifecycle()

                    QuizScreen(
                        questions = questions,
                        currentIndex = currentIndex,
                        selectedOption = selectedOption,
                        isAnswerSubmitted = isAnswerSubmitted,
                        scoreReport = scoreReport,
                        selectedSubject = selectedSubj,
                        selectedUnit = selectedUnit,
                        selectedType = selectedType,
                        questionCount = questionCount,
                        timeLimitMinutes = timeLimitMinutes,
                        remainingSeconds = remainingSeconds,
                        isTimerRunning = isTimerRunning,
                        isGeneratingFromInternet = isGeneratingQuiz,
                        onSelectSubject = { viewModel.loadQuizQuestions(subject = it) },
                        onSelectOption = { viewModel.selectQuizOption(it) },
                        onSubmitAnswer = { viewModel.submitCurrentQuizAnswer() },
                        onNextQuestion = { viewModel.nextQuizQuestion() },
                        onRestartQuiz = { viewModel.restartQuiz() },
                        onGenerateNewFromInternet = { viewModel.generateNewQuizFromInternet() },
                        onGenerateCustomQuiz = { params -> viewModel.generateQuizFromBank(params) },
                        onGenerateCustomFromInternet = { params -> viewModel.generateCustomQuiz(params) }
                    )
                }

                StudyScreen.GEMINI_AI -> {
                    val chatHistory by viewModel.aiChatHistory.collectAsStateWithLifecycle()
                    val isLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
                    val selectedModel by viewModel.selectedGeminiModel.collectAsStateWithLifecycle()
                    val geminiApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
                    val chatSessions by viewModel.aiChatSessions.collectAsStateWithLifecycle()
                    val activeSessionId by viewModel.activeAiChatSessionId.collectAsStateWithLifecycle()

                    GeminiAiScreen(
                        chatHistory = chatHistory,
                        isLoading = isLoading,
                        currentModel = selectedModel,
                        apiKey = geminiApiKey,
                        chatSessions = chatSessions,
                        activeSessionId = activeSessionId,
                        onSelectModel = { viewModel.setGeminiModel(it) },
                        onSaveApiKey = { viewModel.setGeminiApiKey(it) },
                        onClearChat = { viewModel.clearAiChat() },
                        onNewChat = { viewModel.createNewAiChatSession() },
                        onSelectSession = { viewModel.selectAiChatSession(it) },
                        onDeleteSession = { viewModel.deleteAiChatSession(it) },
                        onSyncAll = { viewModel.syncMultiDevice() },
                        onAskGemini = { prompt, attachments -> viewModel.askGemini(prompt, attachments) }
                    )
                }
            }
        }
    }

    // Movable / Floating On-Screen Focus Tool Widget states
    val isWidgetFloating by viewModel.isFocusWidgetFloating.collectAsStateWithLifecycle()
    val isTimerRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
    val remainingSeconds by viewModel.focusRemainingSeconds.collectAsStateWithLifecycle()
    val totalSeconds by viewModel.focusTotalSeconds.collectAsStateWithLifecycle()
    val activeDocTitle by viewModel.activeViewingDocumentTitle.collectAsStateWithLifecycle()
    val focusPdf by viewModel.focusPdfFile.collectAsStateWithLifecycle()

    val floatingWidgetComposable: @Composable () -> Unit = {
        FloatingFocusWidget(
            remainingSeconds = remainingSeconds,
            totalSeconds = totalSeconds,
            isTimerRunning = isTimerRunning,
            activeDocTitle = activeDocTitle,
            activePdfFile = focusPdf,
            onToggleTimer = {
                if (isTimerRunning) viewModel.pauseFocusTimer() else viewModel.startFocusTimer()
            },
            onResetTimer = { viewModel.resetFocusTimer() },
            onExpandFocusRoom = {
                viewModel.closePdfViewer()
                viewModel.navigateTo(StudyScreen.FOCUS_SESSION)
                viewModel.setFocusWidgetFloating(false)
            },
            onOpenPdfViewer = { file ->
                viewModel.openPdfViewerForFile(file, activeDocTitle ?: "Exam Document")
            },
            onDismissWidget = { viewModel.setFocusWidgetFloating(false) }
        )
    }

    // Embedded Native PDF Viewer Tool Modal
    if (isPdfViewerOpen && activePdfFile != null) {
        PdfViewerDialog(
            pdfFile = activePdfFile!!,
            title = activePdfTitle.ifBlank { activePdfFile!!.name },
            onDismiss = { viewModel.closePdfViewer() },
            onOpenInFocus = { file ->
                viewModel.configureFocusSession(
                    durationMinutes = 45,
                    paper = null,
                    companionScheme = null,
                    actionOnTimeout = "SWITCH_TO_SCHEME",
                    sound = "Library"
                )
                viewModel.closePdfViewer()
                viewModel.navigateTo(StudyScreen.FOCUS_SESSION)
            },
            floatingFocusWidget = if (isWidgetFloating) floatingWidgetComposable else null
        )
    }

    // When PDF viewer is not active, floating focus widget shows over standard screens
    if (isWidgetFloating && !isPdfViewerOpen) {
        floatingWidgetComposable()
    }
}
