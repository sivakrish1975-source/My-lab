package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CloudFile
import com.example.data.model.QuestionReview
import com.example.data.repository.StudyRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var repository: StudyRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        repository = StudyRepository(context)
    }

    @Test
    fun `read app name string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("AL Collab", appName)
    }

    @Test
    fun `test user sign in and persistent caching`() {
        val failResult = repository.signIn("", "123")
        assertTrue(failResult.isFailure)

        val successResult = repository.signIn("student@gmail.com", "pass12345")
        assertTrue(successResult.isSuccess)
        assertNotNull(repository.currentUser.value)
        assertEquals("Student", repository.currentUser.value?.name)
    }

    @Test
    fun `test google sign in option`() {
        val user = repository.signInWithGoogle("sivakrish1975@gmail.com")
        assertEquals("sivakrish1975@gmail.com", user.email)
        assertEquals("GOOGLE", user.authProvider)
        assertEquals(user, repository.currentUser.value)
    }

    @Test
    fun `test 5 option quiz evaluation logic`() {
        val questions = repository.getAlevelQuestions("Physics")
        assertTrue(questions.isNotEmpty())
        assertEquals(5, questions.first().options.size)

        val reviews = listOf(
            QuestionReview(questions.first(), selectedIndex = questions.first().correctIndex, isCorrect = true)
        )
        val report = repository.evaluateQuiz(reviews, 30)
        assertEquals(1, report.score)
        assertEquals(100f, report.accuracy)
    }

    @Test
    fun `test role based delete permission`() = runBlocking {
        val user = repository.signIn("uploader@gmail.com", "pass12345").getOrNull()!!
        val fileOwnedByUploader = CloudFile(
            id = "test_f1",
            fileName = "my_doc.pdf",
            subject = "Physics",
            fileType = "Question Paper",
            fileSizeKb = 2000,
            uploaderId = user.id,
            uploaderName = user.name
        )
        // Owner can delete
        val deleteRes = repository.deleteCloudFile(fileOwnedByUploader)
        assertTrue(deleteRes.isSuccess)
    }
}
