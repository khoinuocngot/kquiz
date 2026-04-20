package com.example.quizfromfileapp.quizgenerator

import android.content.Context
import android.util.Log
import com.example.quizfromfileapp.domain.model.ContentSegment
import com.example.quizfromfileapp.domain.model.QuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "LocalLlmManager"

class LocalLlmManager(private val context: Context) {

    @Volatile
    private var modelState: ModelState = ModelState.NOT_INITIALIZED

    private var mlKitLlmInference: Any? = null

    // ─────────────────────────────────────────────────────────────
    // Model Initialization
    // ─────────────────────────────────────────────────────────────

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        when (modelState) {
            ModelState.READY -> {
                logd("initialize: model đã sẵn sàng, bỏ qua")
                return@withContext true
            }
            ModelState.INITIALIZING -> {
                logd("initialize: đang khởi tạo bởi thread khác, chờ...")
                waitForReady()
                return@withContext modelState == ModelState.READY
            }
            ModelState.NOT_INITIALIZED,
            ModelState.FAILED -> {
                modelState = ModelState.INITIALIZING
            }
        }

        try {
            initializeMlKitLlm()
            modelState = ModelState.READY
            logd("initialize: ML Kit LLM khởi tạo thành công")
            true
        } catch (e: Exception) {
            modelState = ModelState.FAILED
            loge("initialize thất bại: ${e.message}")
            throw LlmNotAvailableException("Không khởi tạo được LLM: ${e.message}", e)
        }
    }

    private fun initializeMlKitLlm() {
        if (!LlmAvailabilityChecker.isMlKitLlmApiAvailableSync()) {
            throw LlmNotAvailableException(
                "ML Kit LLM API class không tìm thấy. " +
                "Hãy thêm dependency:\n" +
                "implementation 'com.google.mlkit:language-model:16.0.0-beta1'\n" +
                "Hoặc dùng file-based model và đặt MODEL_ASSET_PATH trong LlmGenerationConfig."
            )
        }

        mlKitLlmInference = createLlmInferenceInstance()
        logd("ML Kit LlmInference instance đã tạo")
    }

    private fun createLlmInferenceInstance(): Any {
        return try {
            val llmClass = Class.forName("com.google.mlkit.nl.inference.LlmInference")
            // Thử getInstance() không tham số trước
            try {
                val getInstance = llmClass.getMethod("getInstance")
                getInstance.invoke(null)
            } catch (_: Exception) {
                // Thử với Options
                val options = buildLlmOptions()
                val getInstanceWithOptions = llmClass.getMethod("getInstance", Class.forName("com.google.mlkit.nl.inference.LlmInference\$Options"))
                getInstanceWithOptions.invoke(null, options)
            }
        } catch (e: ClassNotFoundException) {
            throw LlmNotAvailableException(
                "ML Kit LLM Inference class không tìm thấy. " +
                "Cần thêm dependency 'com.google.mlkit:language-model:16.0.0-beta1'",
                e
            )
        } catch (e: Exception) {
            throw LlmNotAvailableException("Không tạo được LlmInference instance: ${e.message}", e)
        }
    }

    /**
     * Build LlmInference.Options bằng reflection.
     * Nếu ML Kit class không có, throw.
     */
    private fun buildLlmOptions(): Any {
        return try {
            val optionsBuilderClass = Class.forName("com.google.mlkit.nl.inference.LlmInference\$Options\$Builder")
            val optionsClass = Class.forName("com.google.mlkit.nl.inference.LlmInference\$Options")

            val builder = optionsBuilderClass.getMethod("builder").invoke(null)
            optionsBuilderClass.getMethod("setMaxTokens", Int::class.java).invoke(builder, 2048)
            optionsBuilderClass.getMethod("setTopK", Int::class.java).invoke(builder, 40)
            optionsBuilderClass.getMethod("setTopP", Float::class.java).invoke(builder, 0.95f)
            optionsBuilderClass.getMethod("setTemperature", Float::class.java).invoke(builder, 0.7f)

            optionsBuilderClass.getMethod("build").invoke(builder)
        } catch (_: Exception) {
            // Nếu Options không hoạt động, throw để fallback
            throw LlmNotAvailableException("ML Kit LLM Options không hỗ trợ trên thiết bị này")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Inference
    // ─────────────────────────────────────────────────────────────

    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        if (modelState != ModelState.READY) {
            val initSuccess = initialize()
            if (!initSuccess) {
                throw LlmNotAvailableException("Model chưa được khởi tạo và initialization thất bại")
            }
        }

        logd("Bắt đầu inference (prompt length: ${prompt.length} chars)")

        val result = withTimeoutOrNull(LlmGenerationConfig.GENERATION_TIMEOUT_MS) {
            executeInference(prompt)
        }

        if (result == null) {
            loge("Inference timeout sau ${LlmGenerationConfig.GENERATION_TIMEOUT_MS}ms")
            throw LlmTimeoutException("LLM inference timeout sau ${LlmGenerationConfig.GENERATION_TIMEOUT_MS / 1000}s")
        }

        logd("Inference hoàn thành (response length: ${result.length} chars)")
        result
    }

    private suspend fun executeInference(prompt: String): String {
        val inference = mlKitLlmInference
            ?: throw LlmNotAvailableException("ML Kit LLM Inference chưa được khởi tạo")

        return suspendCancellableCoroutine { continuation ->
            try {
                val llmClass = inference.javaClass

                // Thử generateAsync với Consumer
                val generateMethod = llmClass.getMethod(
                    "generateAsync",
                    String::class.java,
                    java.util.function.Consumer::class.java
                )

                @Suppress("UNCHECKED_CAST")
                val future = generateMethod.invoke(
                    inference,
                    prompt,
                    java.util.function.Consumer<Any> { response ->
                        if (continuation.isActive) {
                            val responseText = extractResponseText(response)
                            continuation.resume(responseText)
                        }
                    }
                ) as? java.util.concurrent.Future<*>

                continuation.invokeOnCancellation {
                    logd("Inference cancelled")
                    try {
                        future?.cancel(true)
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        LlmInferenceException("Inference lỗi: ${e.message}", e)
                    )
                }
            }
        }
    }

    private fun extractResponseText(response: Any): String {
        return try {
            when (response) {
                is String -> response
                else -> {
                    val textMethod = response.javaClass.getMethod("getText")
                    textMethod.invoke(response) as? String ?: response.toString()
                }
            }
        } catch (_: Exception) {
            response.toString()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Generation Pipeline
    // ─────────────────────────────────────────────────────────────

    suspend fun generateQuizQuestions(
        segments: List<ContentSegment>,
        targetCount: Int,
        language: String
    ): List<QuizQuestion> {
        if (segments.isEmpty()) {
            logd("generateQuizQuestions: segments rỗng → return empty")
            return emptyList()
        }

        logd("generateQuizQuestions: ${segments.size} segments, target=$targetCount, lang=$language")

        val prompt = LlmPromptBuilder.buildPrompt(
            segments = segments,
            targetCount = targetCount.coerceAtMost(LlmGenerationConfig.MAX_OUTPUT_QUESTIONS),
            language = language
        )

        if (prompt.isBlank()) {
            logd("Prompt rỗng sau khi build → return empty")
            return emptyList()
        }

        val rawResponse = generateResponse(prompt)
        val questions = LlmResponseParser.parse(rawResponse)

        logd("generateQuizQuestions: parsed ${questions.size} câu hỏi")
        return questions
    }

    fun isReady(): Boolean = modelState == ModelState.READY

    fun close() {
        logd("close: giải phóng model resources")
        try {
            mlKitLlmInference?.let { inference ->
                val closeMethod = inference.javaClass.getMethod("close")
                closeMethod.invoke(inference)
            }
        } catch (_: Exception) {}
        finally {
            mlKitLlmInference = null
            modelState = ModelState.NOT_INITIALIZED
        }
    }

    private fun waitForReady(timeoutMs: Long = 30_000L) {
        val start = System.currentTimeMillis()
        while (modelState == ModelState.INITIALIZING && System.currentTimeMillis() - start < timeoutMs) {
            Thread.sleep(100)
        }
    }

    private fun logd(message: String) {
        if (LlmGenerationConfig.DEBUG_LOGGING) {
            Log.d(TAG, message)
        }
    }

    private fun loge(message: String) {
        Log.e(TAG, message)
    }

    private enum class ModelState {
        NOT_INITIALIZED,
        INITIALIZING,
        READY,
        FAILED
    }
}
