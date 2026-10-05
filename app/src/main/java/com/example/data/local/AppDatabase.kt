package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val senderName: String,
    val senderPhotoUri: String?,
    val text: String,
    val timestamp: Long,
    val attachmentType: String?,
    val attachmentTitle: String?,
    val attachmentPath: String?,
    val isSyncedToCloud: Boolean
)

@Entity(tableName = "past_papers")
data class PastPaperEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    val year: Int,
    val paperType: String,
    val medium: String,
    val fileSizeMb: Double,
    val isCached: Boolean,
    val companionSchemeId: String?,
    val questionsCount: Int,
    val officialExamContent: String,
    val markingSchemeContent: String,
    val localPdfPath: String? = null,
    val sourceUrl: String? = null
)

@Entity(tableName = "cloud_files")
data class CloudFileEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val subject: String,
    val fileType: String,
    val fileSizeKb: Long,
    val uploaderId: String,
    val uploaderName: String,
    val uploadedAt: Long,
    val isScheme: Boolean,
    val localFilePath: String?,
    val contentText: String
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChat()
}

@Dao
interface PastPaperDao {
    @Query("SELECT * FROM past_papers ORDER BY year DESC, title ASC")
    fun getAllPapers(): Flow<List<PastPaperEntity>>

    @Query("SELECT * FROM past_papers WHERE subject = :subject ORDER BY year DESC")
    fun getPapersBySubject(subject: String): Flow<List<PastPaperEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaper(paper: PastPaperEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPapers(papers: List<PastPaperEntity>)

    @Query("UPDATE past_papers SET isCached = :cached WHERE id = :id")
    suspend fun updateCachedStatus(id: String, cached: Boolean)

    @Query("UPDATE past_papers SET localPdfPath = :path, isCached = 1 WHERE id = :id")
    suspend fun updateLocalPdfPath(id: String, path: String)

    @Query("DELETE FROM past_papers WHERE id = :id")
    suspend fun deletePaper(id: String)
}

@Dao
interface CloudFileDao {
    @Query("SELECT * FROM cloud_files ORDER BY uploadedAt DESC")
    fun getAllFiles(): Flow<List<CloudFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: CloudFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<CloudFileEntity>)

    @Query("DELETE FROM cloud_files WHERE id = :fileId")
    suspend fun deleteFile(fileId: String)
}

@Entity(tableName = "ai_chat_sessions")
data class AiChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int,
    val previewText: String
)

@Entity(tableName = "ai_chat_messages")
data class AiChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val attachmentNames: String, // comma separated
    val attachmentType: String?
)

@Dao
interface AiChatDao {
    @Query("SELECT * FROM ai_chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<AiChatSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AiChatSessionEntity)

    @Query("DELETE FROM ai_chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("SELECT * FROM ai_chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<AiChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiChatMessageEntity)

    @Query("DELETE FROM ai_chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)
}

@Database(
    entities = [
        ChatMessageEntity::class,
        PastPaperEntity::class,
        CloudFileEntity::class,
        AiChatSessionEntity::class,
        AiChatMessageEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun pastPaperDao(): PastPaperDao
    abstract fun cloudFileDao(): CloudFileDao
    abstract fun aiChatDao(): AiChatDao
}
