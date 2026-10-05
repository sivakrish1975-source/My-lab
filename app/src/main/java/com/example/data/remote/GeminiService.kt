package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AiChatAttachment
import com.example.data.model.QuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Resolves the effective API key from user-provided key or BuildConfig
     */
    fun resolveApiKey(customApiKey: String?): String {
        if (!customApiKey.isNullOrBlank()) {
            return customApiKey.trim()
        }
        val buildKey = BuildConfig.GEMINI_API_KEY
        return if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey.trim() else ""
    }

    suspend fun askTutor(
        userQuestion: String,
        model: String = "gemini-3.5-flash",
        customApiKey: String? = null,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        attachments: List<AiChatAttachment> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(customApiKey)
        if (apiKey.isBlank()) {
            return@withContext "🔑 **Gemini API Key Required**\n\n" +
                    "To receive live, personalized responses from **$model**, tap the **⚙️ Model & Key** settings in the top bar to paste your Gemini API key (or configure in Secrets panel).\n\n" +
                    "---\n" +
                    getOfflineSmartResponse(userQuestion)
        }

        try {
            val cleanModel = model.trim().ifBlank { "gemini-3.5-flash" }
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"

            val contentsArray = JSONArray()

            // Pass prior conversation turns for full session continuity (up to 20 turns)
            for ((role, text) in conversationHistory.takeLast(20)) {
                if (text.isNotBlank() && !text.startsWith("🔑 **Gemini API Key Required**")) {
                    val turnObj = JSONObject()
                    turnObj.put("role", if (role == "user") "user" else "model")
                    val parts = JSONArray()
                    val part = JSONObject()
                    part.put("text", text)
                    parts.put(part)
                    turnObj.put("parts", parts)
                    contentsArray.put(turnObj)
                }
            }

            // Current question with multimodal attachments
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()

            // Attachments (Images, PDFs, Documents)
            for (att in attachments) {
                if (!att.base64Data.isNullOrBlank()) {
                    val inlineDataObj = JSONObject()
                    val dataObj = JSONObject()
                    dataObj.put("mimeType", att.mimeType)
                    dataObj.put("data", att.base64Data)
                    inlineDataObj.put("inlineData", dataObj)
                    currentParts.put(inlineDataObj)
                } else if (!att.textSnippet.isNullOrBlank()) {
                    val textPart = JSONObject()
                    textPart.put("text", "📄 [Attached Document: ${att.name}]\n${att.textSnippet}\n---")
                    currentParts.put(textPart)
                }
            }

            // Main text prompt
            val currentPart = JSONObject()
            currentPart.put("text", userQuestion)
            currentParts.put(currentPart)
            currentTurn.put("parts", currentParts)
            contentsArray.put(currentTurn)

            // System instruction tailored for Sri Lankan A/L curriculum with LaTeX equations
            val systemInstruction = JSONObject()
            val sysParts = JSONArray()
            val sysPart = JSONObject()
            sysPart.put(
                "text",
                "You are an expert Sri Lankan G.C.E. A/L (Advanced Level) tutor specializing in Combined Mathematics, Physics, Chemistry, Biology, and ICT. " +
                        "Provide comprehensive, clear step-by-step solutions, derivations, marking scheme criteria (Method M and Accuracy A marks), formulas, and mnemonics. " +
                        "Format your response with clear markdown headings, bullet points, and LaTeX math notations ($...$ for inline formulas, and $$...$$ for standalone equations) " +
                        "such as fractions \\frac{a}{b}, roots \\sqrt{x}, Greek symbols (\\alpha, \\beta, \\theta, \\lambda), integrals, and summations."
            )
            sysParts.put(sysPart)
            systemInstruction.put("parts", sysParts)

            val requestJson = JSONObject()
            requestJson.put("contents", contentsArray)
            requestJson.put("systemInstruction", systemInstruction)

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            requestJson.put("generationConfig", genConfig)

            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorDetail = try {
                    val errJson = JSONObject(responseBody).optJSONObject("error")
                    errJson?.optString("message") ?: "HTTP status ${response.code}"
                } catch (e: Exception) {
                    "HTTP status ${response.code}"
                }
                Log.w("GeminiService", "API call failed with code ${response.code}: $responseBody")
                return@withContext "⚠️ **Gemini API Error (${response.code})**\n\n" +
                        "$errorDetail\n\n" +
                        "• **Model:** `$cleanModel`\n" +
                        "• **Troubleshooting:** Verify your API key in **⚙️ Model & Key** settings, or try selecting `gemini-3.5-flash` or `gemini-3.1-flash-lite-preview`."
            }

            val jsonRes = JSONObject(responseBody)
            val candidates = jsonRes.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val first = candidates.getJSONObject(0)
                val content = first.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val reply = parts.getJSONObject(0).optString("text", "")
                    if (reply.isNotBlank()) {
                        return@withContext reply
                    }
                }
            }

            "⚠️ Received an empty response from model `$cleanModel`. Please rephrase or try another question."
        } catch (e: Exception) {
            Log.e("GeminiService", "Exception calling Gemini API", e)
            "⚠️ **Network / Connection Error**\n\n" +
                    "Could not reach Google Gemini API (${e.localizedMessage ?: "Unknown network error"}).\n\n" +
                    "Please check your internet connection or verify your API key settings."
        }
    }

    /**
     * Generates 5 fresh A/L multiple choice quiz questions using Gemini API from internet
     */
    suspend fun generateQuizQuestions(
        subject: String,
        unit: String = "All Units",
        questionType: String = "All Types",
        count: Int = 5,
        model: String = "gemini-3.5-flash",
        customApiKey: String? = null
    ): List<QuizQuestion>? = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(customApiKey)
        if (apiKey.isBlank()) return@withContext null

        try {
            val cleanModel = model.trim().ifBlank { "gemini-3.5-flash" }
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"
            val validCount = count.coerceIn(3, 25)

            val unitClause = if (unit != "All Units" && unit.isNotBlank()) "specifically covering curriculum unit: '$unit'" else "covering a balanced mix of syllabus units"
            val typeClause = if (questionType != "All Types" && questionType.isNotBlank()) "with format style: '$questionType'" else "with standard 5-option MCQ format"

            val prompt = """
                Generate exactly $validCount authentic, challenging Sri Lankan G.C.E. Advanced Level (A/L) examination questions for subject '$subject' $unitClause $typeClause.
                Each question MUST have exactly 5 distinct options ("Option 1: ...", "Option 2: ...", "Option 3: ...", "Option 4: ...", "Option 5: ...").
                Include correctIndex (0 to 4), topic name (specific unit), detailed explanation citing Sri Lankan exam marking scheme principles, and difficulty ("A/L Standard").

                Return ONLY a valid JSON array of objects with this exact structure:
                [
                  {
                    "subject": "$subject",
                    "topic": "Unit / Topic Name",
                    "questionText": "Question description with exact parameters...",
                    "options": [
                      "Option 1: ...",
                      "Option 2: ...",
                      "Option 3: ...",
                      "Option 4: ...",
                      "Option 5: ..."
                    ],
                    "correctIndex": 0,
                    "explanation": "Detailed explanation of correct answer and why other options are incorrect",
                    "difficulty": "A/L Standard"
                  }
                ]
                Do not include markdown backticks or any explanatory text outside the JSON array.
            """.trimIndent()

            val contentsArray = JSONArray()
            val turn = JSONObject()
            turn.put("role", "user")
            val parts = JSONArray()
            val part = JSONObject()
            part.put("text", prompt)
            parts.put(part)
            turn.put("parts", parts)
            contentsArray.put(turn)

            val requestJson = JSONObject()
            requestJson.put("contents", contentsArray)

            val genConfig = JSONObject()
            genConfig.put("responseMimeType", "application/json")
            genConfig.put("temperature", 0.7)
            requestJson.put("generationConfig", genConfig)

            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null

            if (!response.isSuccessful) {
                Log.w("GeminiService", "Quiz generation failed with HTTP ${response.code}: $responseBody")
                return@withContext null
            }

            val jsonRes = JSONObject(responseBody)
            val candidates = jsonRes.optJSONArray("candidates") ?: return@withContext null
            if (candidates.length() == 0) return@withContext null
            val rawText = candidates.getJSONObject(0)
                .optJSONObject("content")
                ?.optJSONArray("parts")
                ?.getJSONObject(0)
                ?.optString("text") ?: return@withContext null

            val cleanedJson = rawText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonArray = JSONArray(cleanedJson)
            val questions = mutableListOf<QuizQuestion>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val optArray = obj.getJSONArray("options")
                val optionsList = mutableListOf<String>()
                for (j in 0 until optArray.length()) {
                    optionsList.add(optArray.getString(j))
                }

                val q = QuizQuestion(
                    id = "net_q_${UUID.randomUUID().toString().take(8)}",
                    subject = obj.optString("subject", subject.ifBlank { "Physical Science" }),
                    topic = obj.optString("topic", "A/L Examination Unit"),
                    questionText = obj.getString("questionText"),
                    drawableResName = null,
                    options = optionsList,
                    correctIndex = obj.getInt("correctIndex").coerceIn(0, (optionsList.size - 1).coerceAtLeast(0)),
                    explanation = obj.optString("explanation", "Standard Sri Lanka A/L marking scheme rule applied."),
                    difficulty = obj.optString("difficulty", "A/L Standard")
                )
                questions.add(q)
            }

            if (questions.isNotEmpty()) questions else null
        } catch (e: Exception) {
            Log.e("GeminiService", "Failed to parse generated quiz from internet", e)
            null
        }
    }

    private fun getOfflineSmartResponse(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("circuit") || q.contains("resistor") || q.contains("current") || q.contains("voltage") || q.contains("physics") -> {
                "💡 **Physics Circuit Analysis (A/L Unit 5 - Current Electricity)**\n\n" +
                        "1. **Kirchhoff's Current Law (KCL):** Σ I = 0 at any node. Total incoming charge equals total outgoing charge.\n" +
                        "2. **Kirchhoff's Voltage Law (KVL):** Σ E = Σ IR around any closed mesh loop.\n" +
                        "3. **Equivalent Resistance Formulations:**\n" +
                        "   - Series: R_eq = R1 + R2 + ... + Rn\n" +
                        "   - Parallel: 1/R_eq = 1/R1 + 1/R2 + ...\n\n" +
                        "📌 *Marking Scheme Tip:* Always indicate defined loop directions and polarity conventions clearly to obtain full method marks (M1 + A1) in Structured Essay questions."
            }
            q.contains("organic") || q.contains("reaction") || q.contains("mechanism") || q.contains("benzene") || q.contains("chemistry") -> {
                "🧪 **A/L Chemistry - Organic Conversion Strategy**\n\n" +
                        "• **Electrophilic Addition:** Alkene + Br2/CCl4 (Decolorization test for unsaturation).\n" +
                        "• **Nucleophilic Substitution:** Alkyl Halide + alcoholic KCN -> Nitrile (Carbon chain extension).\n" +
                        "• **Electrophilic Aromatic Substitution:** Benzene + conc. HNO3 / conc. H2SO4 at 50°C-55°C -> Nitrobenzene.\n" +
                        "• **Grignard Synthesis:** R-MgX + Formaldehyde -> 1° alcohol.\n\n" +
                        "🔑 *Scheme Guidance:* Step-wise reagent specification with exact conditions (temperature, catalyst, phase) is strictly evaluated."
            }
            q.contains("differentiation") || q.contains("integration") || q.contains("calculus") || q.contains("math") || q.contains("vector") -> {
                "📐 **Combined Mathematics (Pure & Applied Mathematics)**\n\n" +
                        "**Standard Calculus Forms:**\n" +
                        "• d/dx [sin(ax+b)] = a * cos(ax+b)\n" +
                        "• ∫ [f'(x) / f(x)] dx = ln|f(x)| + Constant\n" +
                        "• **Integration by Parts:** ∫ u (dv/dx) dx = u*v - ∫ v (du/dx) dx\n\n" +
                        "🎯 *Exam Strategy:* Check boundary conditions and never omit the integration constant C in indefinite integrals to preserve the accuracy mark."
            }
            q.contains("cell") || q.contains("mitosis") || q.contains("dna") || q.contains("biology") || q.contains("photosynthesis") -> {
                "🔬 **A/L Biology - Cellular Structure & Physiological Processes**\n\n" +
                        "• **Mitochondria:** Double-membrane organelle responsible for ATP synthesis via oxidative phosphorylation.\n" +
                        "• **Chloroplast:** Site of photosynthesis (Light reactions in thylakoids; Calvin cycle in stroma).\n" +
                        "• **Ribosomes (80S / 70S):** Sites of mRNA translation into polypeptide chains.\n\n" +
                        "📝 *Note:* In structural essays, neatly labeled diagrams carry up to 4 marks out of 15!"
            }
            q.contains("network") || q.contains("subnet") || q.contains("python") || q.contains("database") || q.contains("ict") -> {
                "💻 **A/L ICT - Systems & Networking**\n\n" +
                        "• **IPv4 Subnetting:** Total hosts = 2^(32 - prefix), Usable hosts = 2^(32 - prefix) - 2 (excluding network and broadcast addresses).\n" +
                        "• **Database Normalization:** 1NF (atomic values), 2NF (no partial dependency on composite key), 3NF (no transitive dependency).\n" +
                        "• **Python Data Structures:** Lists (mutable), Tuples (immutable), Dictionaries (key-value hash maps)."
            }
            else -> {
                "🎓 **Sri Lanka A/L Study Assistant:**\n\n" +
                        "Analyzing topic: *\"$query\"*\n\n" +
                        "• **Step 1 - Core Principle:** Identify the governing physical law, theorem, or chemical equilibrium equation from the NIE syllabus.\n" +
                        "• **Step 2 - Model Formulation:** Write down initial state parameters, system constraints, and applicable formulas.\n" +
                        "• **Step 3 - Marking Scheme Rubric:** Step-by-step intermediate working yields maximum method marks even if final arithmetic contains an error.\n\n" +
                        "💡 *Tip:* Enter your Gemini API key in **⚙️ Model & Key** above to enable dynamic, unconstrained reasoning powered by models like **Gemini 3.5 Flash** or **Gemini 3.1 Flash Lite**!"
            }
        }
    }
}
