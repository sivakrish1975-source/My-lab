package com.example.data.model

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val photoUri: String? = null,
    val avatarId: String = "avatar_1",
    val stream: String = "Physical Science",
    val role: String = "STUDENT",
    val isEmailVerified: Boolean = true,
    val authProvider: String = "EMAIL", // "GOOGLE" or "EMAIL"
    val signedInTimestamp: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val senderId: String,
    val senderName: String,
    val senderPhotoUri: String? = null,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachmentType: String? = null, // "PAPER", "FILE", "SCHEME"
    val attachmentTitle: String? = null,
    val attachmentPath: String? = null,
    val isSyncedToCloud: Boolean = true
)

data class PastPaper(
    val id: String,
    val title: String,
    val subject: String,
    val year: Int,
    val paperType: String, // "Paper I (MCQ)", "Paper II (Structured & Essay)", "Marking Scheme"
    val medium: String = "English",
    val fileSizeMb: Double,
    val isCached: Boolean = true,
    val companionSchemeId: String? = null,
    val questionsCount: Int = 50,
    val officialExamContent: String = "",
    val markingSchemeContent: String = "",
    val localPdfPath: String? = null,
    val sourceUrl: String? = null
)

data class CloudFile(
    val id: String,
    val fileName: String,
    val subject: String,
    val fileType: String,
    val fileSizeKb: Long,
    val uploaderId: String,
    val uploaderName: String,
    val uploadedAt: Long = System.currentTimeMillis(),
    val isScheme: Boolean = false,
    val localFilePath: String? = null,
    val contentText: String = ""
)

data class QuizQuestion(
    val id: String,
    val subject: String,
    val topic: String,
    val questionText: String,
    val drawableResName: String? = null,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val difficulty: String = "A/L Standard",
    val questionType: String = "MCQ 5-Option",
    val unitName: String = "General"
)

data class QuizGenerationParams(
    val subject: String = "Physics",
    val unit: String = "All Units",
    val questionType: String = "All Types",
    val questionCount: Int = 5,
    val timeLimitMinutes: Int = 5 // 0 means untimed
)

data class QuizScoreReport(
    val score: Int,
    val total: Int,
    val accuracy: Float,
    val subject: String,
    val timeSeconds: Int,
    val weakTopics: List<String>,
    val strongTopics: List<String>,
    val questionAnswers: List<QuestionReview>
)

data class QuestionReview(
    val question: QuizQuestion,
    val selectedIndex: Int,
    val isCorrect: Boolean
)

data class AiChatSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "New Discussion",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0,
    val previewText: String = ""
)

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sessionId: String,
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachmentNames: List<String> = emptyList(),
    val attachmentUris: List<String> = emptyList(),
    val attachmentType: String? = null // "IMAGE", "DOCUMENT", "FOLDER"
)

data class AiChatAttachment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val mimeType: String,
    val uriString: String = "",
    val sizeBytes: Long = 0,
    val base64Data: String? = null,
    val textSnippet: String? = null
)

