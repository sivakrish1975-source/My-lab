package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AiChatAttachment
import com.example.data.model.AiChatMessage
import com.example.data.model.AiChatSession
import com.example.data.model.CloudFile
import com.example.data.model.PastPaper
import com.example.data.model.QuestionReview
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizScoreReport
import com.example.data.model.UserProfile
import com.example.data.repository.StudyRepository
import com.example.service.FloatingFocusService
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StudyScreen {
    CHAT,
    PAPERS,
    CLOUD_FILES,
    FOCUS_SESSION,
    QUIZ,
    GEMINI_AI
}

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    val repository = StudyRepository(application)

    // Splash State
    private val _isSplashActive = MutableStateFlow(true)
    val isSplashActive: StateFlow<Boolean> = _isSplashActive.asStateFlow()

    // Navigation State
    private val _currentScreen = MutableStateFlow(StudyScreen.PAPERS)
    val currentScreen: StateFlow<StudyScreen> = _currentScreen.asStateFlow()

    private val _showConnectionDetails = MutableStateFlow(false)
    val showConnectionDetails: StateFlow<Boolean> = _showConnectionDetails.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Auth & Persistent User Session
    val currentUser = repository.currentUser
    val latencyMs = repository.latencyMs
    val isOfflineMode = repository.isOfflineMode
    val accentTheme = repository.accentTheme
    val lastSyncTimestamp = repository.lastSyncTimestamp

    // Chat / Study Log
    val chatMessages = repository.chatMessages.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Past Papers
    val pastPapers = repository.pastPapers.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _selectedSubjectFilter = MutableStateFlow("All")
    val selectedSubjectFilter: StateFlow<String> = _selectedSubjectFilter.asStateFlow()

    private val _paperSearchQuery = MutableStateFlow("")
    val paperSearchQuery: StateFlow<String> = _paperSearchQuery.asStateFlow()

    private val _isFetchingPapers = MutableStateFlow(false)
    val isFetchingPapers: StateFlow<Boolean> = _isFetchingPapers.asStateFlow()

    // Embedded Native PDF Viewer Tool State
    private val _activePdfFile = MutableStateFlow<File?>(null)
    val activePdfFile: StateFlow<File?> = _activePdfFile.asStateFlow()

    private val _activePdfTitle = MutableStateFlow<String>("")
    val activePdfTitle: StateFlow<String> = _activePdfTitle.asStateFlow()

    private val _isPdfViewerOpen = MutableStateFlow(false)
    val isPdfViewerOpen: StateFlow<Boolean> = _isPdfViewerOpen.asStateFlow()

    // Web Portal & In-App Downloader State
    private val _isDownloadingFromWeb = MutableStateFlow(false)
    val isDownloadingFromWeb: StateFlow<Boolean> = _isDownloadingFromWeb.asStateFlow()

    private val _downloadProgressStatus = MutableStateFlow("")
    val downloadProgressStatus: StateFlow<String> = _downloadProgressStatus.asStateFlow()

    // Cloud Files
    val cloudFiles = repository.cloudFiles.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Focus Session & Zoomable Reader State
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _focusRemainingSeconds = MutableStateFlow(45 * 60)
    val focusRemainingSeconds: StateFlow<Int> = _focusRemainingSeconds.asStateFlow()

    private val _focusTotalSeconds = MutableStateFlow(45 * 60)
    val focusTotalSeconds: StateFlow<Int> = _focusTotalSeconds.asStateFlow()

    private val _focusSelectedPaper = MutableStateFlow<PastPaper?>(null)
    val focusSelectedPaper: StateFlow<PastPaper?> = _focusSelectedPaper.asStateFlow()

    private val _focusCompanionScheme = MutableStateFlow<CloudFile?>(null)
    val focusCompanionScheme: StateFlow<CloudFile?> = _focusCompanionScheme.asStateFlow()

    private val _focusActionOnTimeout = MutableStateFlow("SWITCH_TO_SCHEME")
    val focusActionOnTimeout: StateFlow<String> = _focusActionOnTimeout.asStateFlow()

    private val _activeViewingDocumentTitle = MutableStateFlow<String?>("2023 G.C.E. A/L Combined Mathematics Paper II")
    val activeViewingDocumentTitle: StateFlow<String?> = _activeViewingDocumentTitle.asStateFlow()

    private val _activeViewingDocumentContent = MutableStateFlow(
        """
DEPARTMENT OF EXAMINATIONS, SRI LANKA
GENERAL CERTIFICATE OF EDUCATION (ADVANCED LEVEL) EXAMINATION - 2023
COMBINED MATHEMATICS II (Pure Mathematics & Applied Mathematics)

SECTION A - Pure Mathematics
1. Let f(x) = x^3 - 3x^2 + kx + 12 where k is a real constant. If (x - 2) is a factor of f(x),
   find the value of k. Hence express f(x) as a product of linear factors.
   
2. Using the principle of mathematical induction, prove that for all positive integers n,
   1/(1*3) + 1/(3*5) + 1/(5*7) + ... + 1/((2n-1)(2n+1)) = n / (2n+1).

3. Find the complex numbers z satisfying |z - 2 - 3i| = 2 and arg(z - 1) = pi/4.
   Sketch the loci on an Argand diagram and determine the intersection points.

4. Differentiate with respect to x: y = ln[sqrt((1 + sin 2x) / (1 - sin 2x))].
   Show that dy/dx = 2 sec 2x.

SECTION B - Applied Mathematics
11. A particle P of mass m is projected vertically upwards from ground level with speed u.
    At the same instant, another particle Q of mass 2m is released from rest from height h.
    Taking gravitational acceleration as g, show that they collide at time t = h / u.
    Determine the velocity of each particle immediately after an elastic collision.
        """.trimIndent()
    )
    val activeViewingDocumentContent: StateFlow<String> = _activeViewingDocumentContent.asStateFlow()

    private val _isViewingSchemeNow = MutableStateFlow(false)
    val isViewingSchemeNow: StateFlow<Boolean> = _isViewingSchemeNow.asStateFlow()

    // Real PDF Document for Focus Session Preview
    private val _focusPdfFile = MutableStateFlow<File?>(null)
    val focusPdfFile: StateFlow<File?> = _focusPdfFile.asStateFlow()

    // Draggable On-Screen Focus Tool Widget State
    private val _isFocusWidgetFloating = MutableStateFlow(false)
    val isFocusWidgetFloating: StateFlow<Boolean> = _isFocusWidgetFloating.asStateFlow()

    fun setFocusWidgetFloating(floating: Boolean) {
        _isFocusWidgetFloating.value = floating
        val context = getApplication<Application>()
        if (floating) {
            FloatingFocusService.start(
                context = context,
                remainingSeconds = _focusRemainingSeconds.value,
                isRunning = _isTimerRunning.value,
                title = _activeViewingDocumentTitle.value ?: "Exam Focus Room"
            )
            showSnackbar("🚀 Floating tool active! It will stay on screen even if you minimize to the home screen.")
        } else {
            FloatingFocusService.stop(context)
        }
    }

    private val _ambientSound = MutableStateFlow("None")
    val ambientSound: StateFlow<String> = _ambientSound.asStateFlow()

    private var timerJob: Job? = null

    // Interactive Quiz State & Filters
    private val _quizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val quizQuestions: StateFlow<List<QuizQuestion>> = _quizQuestions.asStateFlow()

    private val _currentQuizIndex = MutableStateFlow(0)
    val currentQuizIndex: StateFlow<Int> = _currentQuizIndex.asStateFlow()

    private val _selectedQuizOption = MutableStateFlow<Int?>(null)
    val selectedQuizOption: StateFlow<Int?> = _selectedQuizOption.asStateFlow()

    private val _isAnswerSubmitted = MutableStateFlow(false)
    val isAnswerSubmitted: StateFlow<Boolean> = _isAnswerSubmitted.asStateFlow()

    private val _quizReviews = MutableStateFlow<List<QuestionReview>>(emptyList())

    private val _quizScoreReport = MutableStateFlow<QuizScoreReport?>(null)
    val quizScoreReport: StateFlow<QuizScoreReport?> = _quizScoreReport.asStateFlow()

    private val _quizSubjectFilter = MutableStateFlow("All")
    val quizSubjectFilter: StateFlow<String> = _quizSubjectFilter.asStateFlow()

    private val _quizUnitFilter = MutableStateFlow("All Units")
    val quizUnitFilter: StateFlow<String> = _quizUnitFilter.asStateFlow()

    private val _quizTypeFilter = MutableStateFlow("All Types")
    val quizTypeFilter: StateFlow<String> = _quizTypeFilter.asStateFlow()

    private val _quizQuestionCount = MutableStateFlow(5)
    val quizQuestionCount: StateFlow<Int> = _quizQuestionCount.asStateFlow()

    private val _quizTimeLimitMinutes = MutableStateFlow(5)
    val quizTimeLimitMinutes: StateFlow<Int> = _quizTimeLimitMinutes.asStateFlow()

    private val _quizRemainingSeconds = MutableStateFlow(5 * 60)
    val quizRemainingSeconds: StateFlow<Int> = _quizRemainingSeconds.asStateFlow()

    private val _isQuizTimerRunning = MutableStateFlow(false)
    val isQuizTimerRunning: StateFlow<Boolean> = _isQuizTimerRunning.asStateFlow()

    private var quizTimerJob: Job? = null

    private val _isGeneratingQuiz = MutableStateFlow(false)
    val isGeneratingQuiz: StateFlow<Boolean> = _isGeneratingQuiz.asStateFlow()

    // Trigger event to notify any open screen or dialog to close when focus or quiz timer expires
    private val _fileCloseTrigger = MutableStateFlow(0)
    val fileCloseTrigger: StateFlow<Int> = _fileCloseTrigger.asStateFlow()

    // Gemini AI Online Chat & Configuration
    val geminiApiKey: StateFlow<String> = repository.geminiApiKey
    val selectedGeminiModel: StateFlow<String> = repository.selectedGeminiModel

    private val _aiChatHistory = MutableStateFlow<List<Pair<String, String>>>(
        listOf(
            "model" to "👋 Welcome to your A/L Gemini AI Tutor!\n\nAsk any question from Combined Mathematics, Physics, Chemistry, Biology, or ICT, and I will provide step-by-step mathematical reasoning, chemical mechanisms, or marking scheme rubrics."
        )
    )
    val aiChatHistory: StateFlow<List<Pair<String, String>>> = _aiChatHistory.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    init {
        loadQuizQuestions("All")
    }

    fun finishSplash() {
        _isSplashActive.value = false
    }

    fun navigateTo(screen: StudyScreen) {
        _currentScreen.value = screen
    }

    fun setConnectionDetailsVisible(visible: Boolean) {
        _showConnectionDetails.value = visible
    }

    fun setSettingsVisible(visible: Boolean) {
        _showSettings.value = visible
    }

    fun showSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // --- Authentication ---
    fun signIn(email: String, pass: String) {
        val result = repository.signIn(email, pass)
        result.onSuccess {
            showSnackbar("Welcome back, ${it.name}! Session cached.")
        }.onFailure {
            showSnackbar(it.message ?: "Sign in failed")
        }
    }

    fun signUp(name: String, email: String, pass: String, stream: String) {
        val result = repository.signUp(name, email, pass, stream)
        result.onSuccess {
            showSnackbar("Account created and saved for ${it.name}!")
        }.onFailure {
            showSnackbar(it.message ?: "Sign up failed")
        }
    }

    fun signInWithGoogle() {
        val user = repository.signInWithGoogle()
        showSnackbar("Signed in with Google as ${user.email} (Credentials preserved)")
    }

    fun updateProfile(name: String, photoUri: String?, stream: String) {
        repository.updateProfile(name, photoUri, stream)
        showSnackbar("Profile updated successfully!")
    }

    fun sendVerificationEmail() {
        val msg = repository.sendVerificationEmail()
        showSnackbar(msg)
    }

    fun requestPasswordRecovery(email: String) {
        val msg = repository.requestPasswordRecovery(email)
        showSnackbar(msg)
    }

    fun signOut() {
        repository.signOut()
        showSnackbar("Signed out. Local credentials cleared.")
    }

    // --- Real File Upload from Device Storage ---
    fun uploadRealFile(uri: Uri, subject: String, fileType: String, isScheme: Boolean) {
        viewModelScope.launch {
            val result = repository.uploadRealFile(uri, subject, fileType, isScheme)
            result.onSuccess {
                showSnackbar("Successfully uploaded \"${it.fileName}\" from device storage to cloud!")
            }.onFailure {
                showSnackbar(it.message ?: "Upload failed")
            }
        }
    }

    fun deleteCloudFile(file: CloudFile) {
        viewModelScope.launch {
            val result = repository.deleteCloudFile(file)
            result.onSuccess {
                showSnackbar("Document \"${file.fileName}\" removed from cloud storage.")
            }.onFailure {
                showSnackbar(it.message ?: "Delete failed")
            }
        }
    }

    fun openUploadedFileInFocus(file: CloudFile) {
        _focusSelectedPaper.value = null
        _focusCompanionScheme.value = null
        _activeViewingDocumentTitle.value = file.fileName
        _activeViewingDocumentContent.value = file.contentText.ifBlank { "Document loaded: ${file.fileName}\nSubject: ${file.subject}\nType: ${file.fileType}" }
        _isViewingSchemeNow.value = file.isScheme
        viewModelScope.launch {
            val pdf = repository.getPdfFileForCloudFile(file)
            _focusPdfFile.value = pdf
        }
        _currentScreen.value = StudyScreen.FOCUS_SESSION
        showSnackbar("Opened \"${file.fileName}\" in Focus Room with full PDF preview.")
    }

    // --- Study Log / Chat ---
    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(text = text)
        }
    }

    fun sharePaperInChat(paper: PastPaper) {
        viewModelScope.launch {
            repository.sendMessage(
                text = "Past paper logged: ${paper.title} (${paper.year} ${paper.paperType})",
                attachmentType = "PAPER",
                attachmentTitle = paper.title,
                attachmentPath = paper.id
            )
            showSnackbar("Logged \"${paper.title}\" to Study Sync log!")
            _currentScreen.value = StudyScreen.CHAT
        }
    }

    // --- Past Papers ---
    fun setPaperSubjectFilter(subject: String) {
        _selectedSubjectFilter.value = subject
    }

    fun setPaperSearchQuery(query: String) {
        _paperSearchQuery.value = query
    }

    fun autoFetchPapers() {
        viewModelScope.launch {
            _isFetchingPapers.value = true
            val count = repository.autoFetchLatestPapers()
            _isFetchingPapers.value = false
            showSnackbar("Auto-fetch completed! Retrieved $count authentic examination papers.")
        }
    }

    fun toggleCachePaper(paper: PastPaper) {
        viewModelScope.launch {
            repository.togglePaperCache(paper.id, paper.isCached)
            showSnackbar(if (paper.isCached) "Cached offline" else "Removed from offline cache")
        }
    }

    // --- Embedded Native PDF Viewer Tool Actions ---
    fun openPdfViewerForPaper(paper: PastPaper) {
        viewModelScope.launch {
            _isDownloadingFromWeb.value = true
            _downloadProgressStatus.value = "Preparing PDF for ${paper.title}..."
            val file = repository.getPdfFileForPaper(paper)
            _activePdfFile.value = file
            _activePdfTitle.value = paper.title
            _isPdfViewerOpen.value = true
            _isDownloadingFromWeb.value = false
        }
    }

    fun openPdfViewerForCloudFile(cloudFile: CloudFile) {
        viewModelScope.launch {
            _isDownloadingFromWeb.value = true
            _downloadProgressStatus.value = "Opening ${cloudFile.fileName} in PDF Viewer..."
            val file = repository.getPdfFileForCloudFile(cloudFile)
            _activePdfFile.value = file
            _activePdfTitle.value = cloudFile.fileName
            _isPdfViewerOpen.value = true
            _isDownloadingFromWeb.value = false
        }
    }

    fun openPdfViewerForFile(file: File, title: String) {
        _activePdfFile.value = file
        _activePdfTitle.value = title
        _isPdfViewerOpen.value = true
    }

    fun closePdfViewer() {
        _isPdfViewerOpen.value = false
        _activePdfFile.value = null
    }

    fun downloadFromWebUrl(url: String) {
        viewModelScope.launch {
            _isDownloadingFromWeb.value = true
            _downloadProgressStatus.value = "Downloading paper from web portal..."
            val result = repository.downloadPaperFromUrl(url)
            _isDownloadingFromWeb.value = false
            result.onSuccess { (paper, file) ->
                _activePdfFile.value = file
                _activePdfTitle.value = paper.title
                // AUTOMATICALLY open the embedded PDF viewer tool once completed!
                _isPdfViewerOpen.value = true
                showSnackbar("📥 Download complete! Auto-opened \"${paper.title}\" in PDF Viewer.")
            }.onFailure {
                showSnackbar("Download error: ${it.localizedMessage ?: "Failed to download"}")
            }
        }
    }

    fun downloadSamplePaper(sampleId: String) {
        viewModelScope.launch {
            _isDownloadingFromWeb.value = true
            _downloadProgressStatus.value = "Downloading official A/L exam PDF..."
            val (paper, file) = repository.downloadSampleAlevelPaper(sampleId)
            _isDownloadingFromWeb.value = false
            _activePdfFile.value = file
            _activePdfTitle.value = paper.title
            // AUTOMATICALLY open the embedded PDF viewer tool once completed!
            _isPdfViewerOpen.value = true
            showSnackbar("📥 Download complete! Auto-opened \"${paper.title}\" in PDF Viewer.")
        }
    }

    // --- Focus Session Engine with Zoomable Reader ---
    fun configureFocusSession(
        durationMinutes: Int,
        paper: PastPaper?,
        companionScheme: CloudFile?,
        actionOnTimeout: String,
        sound: String
    ) {
        _focusTotalSeconds.value = durationMinutes * 60
        _focusRemainingSeconds.value = durationMinutes * 60
        _focusSelectedPaper.value = paper
        _focusCompanionScheme.value = companionScheme
        _focusActionOnTimeout.value = actionOnTimeout
        _ambientSound.value = sound
        _isViewingSchemeNow.value = false

        if (paper != null) {
            _activeViewingDocumentTitle.value = paper.title
            _activeViewingDocumentContent.value = paper.officialExamContent.ifBlank {
                "Examination Paper: ${paper.title}\nSubject: ${paper.subject}\nYear: ${paper.year}"
            }
            viewModelScope.launch {
                val file = repository.getPdfFileForPaper(paper)
                _focusPdfFile.value = file
            }
        } else if (companionScheme != null) {
            _activeViewingDocumentTitle.value = companionScheme.fileName
            _activeViewingDocumentContent.value = companionScheme.contentText
            viewModelScope.launch {
                val file = repository.getPdfFileForCloudFile(companionScheme)
                _focusPdfFile.value = file
            }
        }
    }

    fun startFocusTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true
        val context = getApplication<Application>()
        if (_isFocusWidgetFloating.value) {
            FloatingFocusService.update(context, _focusRemainingSeconds.value, true)
        }
        timerJob = viewModelScope.launch {
            while (_isTimerRunning.value && _focusRemainingSeconds.value > 0) {
                delay(1000)
                _focusRemainingSeconds.value -= 1
                if (_isFocusWidgetFloating.value) {
                    FloatingFocusService.update(context, _focusRemainingSeconds.value, true)
                }
            }
            if (_focusRemainingSeconds.value <= 0) {
                handleFocusTimeout()
            }
        }
    }

    fun pauseFocusTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        if (_isFocusWidgetFloating.value) {
            FloatingFocusService.update(getApplication(), _focusRemainingSeconds.value, false)
        }
    }

    fun resetFocusTimer() {
        pauseFocusTimer()
        _focusRemainingSeconds.value = _focusTotalSeconds.value
        _isViewingSchemeNow.value = false
        if (_isFocusWidgetFloating.value) {
            FloatingFocusService.update(getApplication(), _focusTotalSeconds.value, false)
        }
        _focusSelectedPaper.value?.let { p ->
            _activeViewingDocumentTitle.value = p.title
            _activeViewingDocumentContent.value = p.officialExamContent
        }
    }

    private fun handleFocusTimeout() {
        _isTimerRunning.value = false
        // Close whatever file or PDF is currently open
        closePdfViewer()
        _activeViewingDocumentTitle.value = null
        _activeViewingDocumentContent.value = ""
        _focusPdfFile.value = null
        _isViewingSchemeNow.value = false
        _fileCloseTrigger.value += 1
        FloatingFocusService.stop(getApplication())
        navigateTo(StudyScreen.FOCUS_SESSION)
        showSnackbar("⏰ Focus Session Ended! Time is 0:00. Automatically opening app window.")
    }

    // --- Enhanced Interactive Quiz Section ---
    fun startQuizTimer(minutes: Int) {
        quizTimerJob?.cancel()
        if (minutes <= 0) {
            _isQuizTimerRunning.value = false
            _quizRemainingSeconds.value = 0
            return
        }
        _quizTimeLimitMinutes.value = minutes
        _quizRemainingSeconds.value = minutes * 60
        _isQuizTimerRunning.value = true
        quizTimerJob = viewModelScope.launch {
            while (_isQuizTimerRunning.value && _quizRemainingSeconds.value > 0) {
                delay(1000)
                _quizRemainingSeconds.value -= 1
            }
            if (_quizRemainingSeconds.value <= 0 && _quizScoreReport.value == null) {
                _isQuizTimerRunning.value = false
                finishQuizDueToTimeOut()
            }
        }
    }

    fun stopQuizTimer() {
        _isQuizTimerRunning.value = false
        quizTimerJob?.cancel()
    }

    private fun finishQuizDueToTimeOut() {
        // Automatically close whatever file or PDF is open when time expires
        closePdfViewer()
        _activeViewingDocumentTitle.value = null
        _activeViewingDocumentContent.value = ""
        _focusPdfFile.value = null
        _isViewingSchemeNow.value = false
        _fileCloseTrigger.value += 1

        val currentQ = _quizQuestions.value.getOrNull(_currentQuizIndex.value)
        if (currentQ != null && !_isAnswerSubmitted.value) {
            val selected = _selectedQuizOption.value ?: -1
            val review = QuestionReview(
                question = currentQ,
                selectedIndex = selected,
                isCorrect = selected == currentQ.correctIndex
            )
            _quizReviews.value = _quizReviews.value + review
        }
        val elapsed = (_quizTimeLimitMinutes.value * 60) - _quizRemainingSeconds.value
        val report = repository.evaluateQuiz(_quizReviews.value, elapsed.coerceAtLeast(1))
        _quizScoreReport.value = report
        showSnackbar("⏰ Quiz Time Limit Expired! Answers submitted and open documents closed.")
    }

    fun loadQuizQuestions(
        subject: String = _quizSubjectFilter.value,
        unit: String = _quizUnitFilter.value,
        questionType: String = _quizTypeFilter.value,
        count: Int = _quizQuestionCount.value,
        timeLimitMinutes: Int = _quizTimeLimitMinutes.value
    ) {
        _quizSubjectFilter.value = subject
        _quizUnitFilter.value = unit
        _quizTypeFilter.value = questionType
        _quizQuestionCount.value = count
        _quizTimeLimitMinutes.value = timeLimitMinutes

        val questions = repository.getAlevelQuestions(subject, unit, questionType, count)
        _quizQuestions.value = questions
        _currentQuizIndex.value = 0
        _selectedQuizOption.value = null
        _isAnswerSubmitted.value = false
        _quizReviews.value = emptyList()
        _quizScoreReport.value = null

        startQuizTimer(timeLimitMinutes)
    }

    fun generateCustomQuiz(params: com.example.data.model.QuizGenerationParams) {
        viewModelScope.launch {
            _isGeneratingQuiz.value = true
            _quizSubjectFilter.value = params.subject
            _quizUnitFilter.value = params.unit
            _quizTypeFilter.value = params.questionType
            _quizQuestionCount.value = params.questionCount
            _quizTimeLimitMinutes.value = params.timeLimitMinutes

            val questions = repository.generateNewQuizFromInternet(
                subject = params.subject,
                unit = params.unit,
                questionType = params.questionType,
                count = params.questionCount
            )
            _quizQuestions.value = questions
            _currentQuizIndex.value = 0
            _selectedQuizOption.value = null
            _isAnswerSubmitted.value = false
            _quizReviews.value = emptyList()
            _quizScoreReport.value = null
            _isGeneratingQuiz.value = false

            startQuizTimer(params.timeLimitMinutes)
            showSnackbar("🎯 Generated ${questions.size} custom ${params.subject} (${params.unit}) questions! Timer: ${if (params.timeLimitMinutes > 0) "${params.timeLimitMinutes} min" else "Untimed"}")
        }
    }

    fun generateQuizFromBank(params: com.example.data.model.QuizGenerationParams) {
        _quizSubjectFilter.value = params.subject
        _quizUnitFilter.value = params.unit
        _quizTypeFilter.value = params.questionType
        _quizQuestionCount.value = params.questionCount
        _quizTimeLimitMinutes.value = params.timeLimitMinutes

        val questions = repository.getAlevelQuestions(
            subject = params.subject,
            unit = params.unit,
            questionType = params.questionType,
            count = params.questionCount
        )
        _quizQuestions.value = questions
        _currentQuizIndex.value = 0
        _selectedQuizOption.value = null
        _isAnswerSubmitted.value = false
        _quizReviews.value = emptyList()
        _quizScoreReport.value = null

        startQuizTimer(params.timeLimitMinutes)
        showSnackbar("📚 Prepared ${questions.size} A/L questions for ${params.subject} • ${params.unit}")
    }

    fun selectQuizOption(index: Int) {
        if (_isAnswerSubmitted.value) return
        _selectedQuizOption.value = index
    }

    fun submitCurrentQuizAnswer() {
        val selected = _selectedQuizOption.value ?: return
        val currentQ = _quizQuestions.value.getOrNull(_currentQuizIndex.value) ?: return

        _isAnswerSubmitted.value = true
        val isCorrect = selected == currentQ.correctIndex

        val review = QuestionReview(
            question = currentQ,
            selectedIndex = selected,
            isCorrect = isCorrect
        )
        _quizReviews.value = _quizReviews.value + review
    }

    fun nextQuizQuestion() {
        if (_currentQuizIndex.value < _quizQuestions.value.size - 1) {
            _currentQuizIndex.value += 1
            _selectedQuizOption.value = null
            _isAnswerSubmitted.value = false
        } else {
            stopQuizTimer()
            val elapsed = if (_quizTimeLimitMinutes.value > 0) {
                ((_quizTimeLimitMinutes.value * 60) - _quizRemainingSeconds.value).coerceAtLeast(1)
            } else 180
            val report = repository.evaluateQuiz(_quizReviews.value, elapsed)
            _quizScoreReport.value = report
        }
    }

    fun restartQuiz() {
        loadQuizQuestions(
            subject = _quizSubjectFilter.value,
            unit = _quizUnitFilter.value,
            questionType = _quizTypeFilter.value,
            count = _quizQuestionCount.value,
            timeLimitMinutes = _quizTimeLimitMinutes.value
        )
    }

    fun generateNewQuizFromInternet(subject: String? = null) {
        viewModelScope.launch {
            _isGeneratingQuiz.value = true
            val targetSubject = subject ?: _quizSubjectFilter.value
            _quizSubjectFilter.value = targetSubject
            val questions = repository.generateNewQuizFromInternet(
                subject = targetSubject,
                unit = _quizUnitFilter.value,
                questionType = _quizTypeFilter.value,
                count = _quizQuestionCount.value
            )
            _quizQuestions.value = questions
            _currentQuizIndex.value = 0
            _selectedQuizOption.value = null
            _isAnswerSubmitted.value = false
            _quizReviews.value = emptyList()
            _quizScoreReport.value = null
            _isGeneratingQuiz.value = false
            startQuizTimer(_quizTimeLimitMinutes.value)
            showSnackbar("🌐 Generated ${questions.size} fresh A/L questions from internet for $targetSubject!")
        }
    }

    fun deletePastPaper(paper: PastPaper) {
        viewModelScope.launch {
            repository.deletePaper(paper)
            if (_activePdfFile.value?.absolutePath == paper.localPdfPath) {
                closePdfViewer()
            }
            showSnackbar("🗑️ Removed \"${paper.title}\" from device.")
        }
    }

    // --- Gemini AI Tutor & Multi-Session Chat History ---
    val aiChatSessions: StateFlow<List<AiChatSession>> = repository.aiChatSessions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val activeAiChatSessionId: StateFlow<String> = repository.activeChatSessionId

    fun createNewAiChatSession(title: String = "New Discussion") {
        viewModelScope.launch {
            val newId = repository.createNewAiChatSession(title)
            _aiChatHistory.value = listOf(
                "model" to "👋 Welcome to your new A/L Gemini AI Tutor session!\n\nAsk any question or upload images/documents/folders, and I'll solve it step-by-step with LaTeX equations."
            )
            showSnackbar("Started new AI Tutor discussion.")
        }
    }

    fun selectAiChatSession(sessionId: String) {
        repository.selectAiChatSession(sessionId)
        viewModelScope.launch {
            repository.getMessagesForSession(sessionId).collect { msgs ->
                if (msgs.isNotEmpty()) {
                    _aiChatHistory.value = msgs.map { it.role to it.content }
                } else {
                    _aiChatHistory.value = listOf(
                        "model" to "👋 Resumed discussion.\n\nAsk a question or continue your study session."
                    )
                }
            }
        }
    }

    fun deleteAiChatSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteAiChatSession(sessionId)
            _aiChatHistory.value = listOf(
                "model" to "👋 Welcome! Start a new chat or select from your history."
            )
            showSnackbar("Chat session deleted.")
        }
    }

    fun setGeminiApiKey(key: String) {
        repository.setGeminiApiKey(key)
        showSnackbar(if (key.isNotBlank()) "Gemini API key saved securely!" else "API Key cleared.")
    }

    fun setGeminiModel(model: String) {
        repository.setSelectedGeminiModel(model)
        showSnackbar("Switched active AI model to $model")
    }

    fun clearAiChat() {
        val currentSession = repository.activeChatSessionId.value
        if (currentSession.isNotBlank()) {
            deleteAiChatSession(currentSession)
        } else {
            _aiChatHistory.value = listOf(
                "model" to "👋 Conversation cleared.\n\nAsk any question from Combined Mathematics, Physics, Chemistry, Biology, or ICT, and I will assist you using ${repository.selectedGeminiModel.value}."
            )
            showSnackbar("AI Tutor conversation cleared.")
        }
    }

    fun askGemini(prompt: String, attachments: List<AiChatAttachment> = emptyList()) {
        if (prompt.isBlank() && attachments.isEmpty()) return
        if (_isAiLoading.value) return

        val currentHistory = _aiChatHistory.value
        val displayPrompt = if (attachments.isNotEmpty()) {
            val attText = attachments.joinToString(", ") { it.name }
            if (prompt.isNotBlank()) "$prompt\n📎 [$attText]" else "📎 Attached: $attText"
        } else prompt

        _aiChatHistory.value = currentHistory + ("user" to displayPrompt)
        _isAiLoading.value = true

        viewModelScope.launch {
            var currentSessionId = repository.activeChatSessionId.value
            if (currentSessionId.isBlank()) {
                val titlePreview = prompt.take(30).ifBlank { "A/L Discussion" }
                currentSessionId = repository.createNewAiChatSession(titlePreview)
            }

            repository.saveAiChatMessage(
                sessionId = currentSessionId,
                role = "user",
                content = displayPrompt,
                attachmentNames = attachments.map { it.name },
                attachmentType = if (attachments.any { it.mimeType.startsWith("image") }) "IMAGE" else "DOCUMENT"
            )

            val response = repository.askGeminiTutor(
                prompt = prompt.ifBlank { "Please analyze the attached document or image thoroughly according to the Sri Lankan A/L curriculum." },
                model = repository.selectedGeminiModel.value,
                apiKey = repository.geminiApiKey.value,
                conversationHistory = currentHistory,
                attachments = attachments
            )

            _aiChatHistory.value = _aiChatHistory.value + ("model" to response)
            _isAiLoading.value = false

            repository.saveAiChatMessage(
                sessionId = currentSessionId,
                role = "model",
                content = response
            )
        }
    }

    // --- Settings & Telemetry ---
    fun toggleOfflineMode() {
        repository.toggleOfflineMode()
        showSnackbar(if (repository.isOfflineMode.value) "Offline Mode Enabled" else "Online Cloud Sync Active")
    }

    fun setAccentTheme(theme: String) {
        repository.setAccentTheme(theme)
        showSnackbar("Theme applied: $theme")
    }

    fun syncMultiDevice() {
        val res = repository.triggerMultiDeviceSync()
        showSnackbar(res)
    }

    fun clearStorageCache() {
        val freed = repository.clearLocalCache()
        showSnackbar("Cleaned $freed MB of cached files and temporary data.")
    }
}
