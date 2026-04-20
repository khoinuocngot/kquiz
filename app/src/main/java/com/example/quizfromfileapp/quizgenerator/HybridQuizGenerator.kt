package com.example.quizfromfileapp.quizgenerator

import android.content.Context
import android.util.Log
import com.example.quizfromfileapp.domain.model.ContentQualityFilter
import com.example.quizfromfileapp.domain.model.ContentSegment
import com.example.quizfromfileapp.domain.model.DeclarativeSentenceExtractor
import com.example.quizfromfileapp.domain.model.ExtractedContent
import com.example.quizfromfileapp.domain.model.HeadingCaptionFilter
import com.example.quizfromfileapp.domain.model.InstructionSentenceFilter
import com.example.quizfromfileapp.domain.model.LanguageDetector
import com.example.quizfromfileapp.domain.model.QuizConfig
import com.example.quizfromfileapp.domain.model.QuizGenerationMode
import com.example.quizfromfileapp.domain.model.QuizSession
import com.example.quizfromfileapp.domain.model.QuizUniquenessValidator

/**
 * Bộ sinh quiz lai — chọn mode dựa trên config.
 *
 * Luồng:
 * - RULE_BASED → SmartQuizGenerator trực tiếp
 * - LLM_ASSISTED → OnDeviceLlmQuizGenerator → fallback SmartQuizGenerator nếu fail
 *
 * MỖI câu hỏi đều truy vết được về trang nguồn (sourcePageStart/sourcePageEnd).
 *
 * Tích hợp LLM state callback để UI phản hồi trạng thái generation.
 */
class HybridQuizGenerator(
    private val context: Context,
    private val llmStateCallback: LlmStateCallback? = null
) : QuizGenerator {

    private val ruleBasedGenerator = SmartQuizGenerator()
    private val llmGenerator: LocalLlmQuizGenerator = OnDeviceLlmQuizGenerator(context, llmStateCallback)
    private val sessionValidator = QuizUniquenessValidator()

    private companion object {
        const val TAG = "HybridQuizGenerator"
        const val MAX_LLM_BATCH = 10
    }

    override suspend fun generate(content: ExtractedContent, config: QuizConfig): QuizSession {
        sessionValidator.reset()

        return when (config.generationMode) {
            QuizGenerationMode.RULE_BASED -> {
                Log.d(TAG, "generate: RULE_BASED → SmartQuizGenerator")
                ruleBasedGenerator.generate(content, config)
            }

            QuizGenerationMode.LLM_ASSISTED -> {
                Log.d(TAG, "generate: LLM_ASSISTED → thử OnDeviceLlmQuizGenerator")
                generateWithLlmFallback(content, config)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // LLM-assisted generation với fallback
    // ─────────────────────────────────────────────────────────────

    /**
     * Sinh bằng LLM, fallback về rule-based nếu:
     * - LLM không available
     * - LLM throw exception
     * - LLM return rỗng
     * - LLM output không parse được
     *
     * Cập nhật LLM state qua callback để UI phản hồi.
     */
    private suspend fun generateWithLlmFallback(
        content: ExtractedContent,
        config: QuizConfig
    ): QuizSession {
        // Bước 1: Kiểm tra LLM availability
        llmStateCallback?.onLlmStateChanged(LlmGenerationUiState.CheckingAvailability)
        Log.d(TAG, "generateWithLlmFallback: đang kiểm tra availability...")

        val available = try {
            llmGenerator.isAvailable()
        } catch (e: Exception) {
            Log.w(TAG, "isAvailable lỗi: ${e.message}")
            false
        }

        if (!available) {
            Log.w(TAG, "generateWithLlmFallback: LLM không khả dụng → fallback RULE_BASED")
            llmStateCallback?.onLlmStateChanged(
                LlmGenerationUiState.FallbackTriggered("Local model chưa sẵn sàng")
            )
            return ruleBasedGenerator.generate(content, config)
        }

        // Bước 2: Lấy quality segments
        val rawSegments = content.cleanedContent?.segments ?: emptyList()
        val qualitySegments = rawSegments
            .filter { ContentQualityFilter.isQualitySegment(it.text) }
            .filterNot { HeadingCaptionFilter.isHeadingOrCaption(it.text) }
            .filterNot { InstructionSentenceFilter.isInstructional(it.text) }
            .filter { DeclarativeSentenceExtractor.extractDeclarativeSentences(it.text).isNotEmpty() }

        if (qualitySegments.isEmpty()) {
            Log.w(TAG, "generateWithLlmFallback: không có quality segments → fallback RULE_BASED")
            llmStateCallback?.onLlmStateChanged(
                LlmGenerationUiState.FallbackTriggered("Không đủ nội dung chất lượng cho LLM")
            )
            return ruleBasedGenerator.generate(content, config)
        }

        // Bước 3: Xác định ngôn ngữ
        val language = LanguageDetector.isEnglish(
            content.pdfExtractionResult?.fullMergedTextBlockCleaned
                ?: content.cleanedText
        ).let { if (it) "en" else "vi" }

        // Bước 4: Gọi LLM với state callback
        llmStateCallback?.onLlmStateChanged(
            LlmGenerationUiState.Generating(
                message = "Đang sinh câu hỏi bằng local AI..."
            )
        )

        val llmQuestions = try {
            llmGenerator.generateQuestions(qualitySegments, config.questionCount, language)
        } catch (e: LlmNotAvailableException) {
            Log.w(TAG, "generateWithLlmFallback: LLM not available: ${e.message}")
            llmStateCallback?.onLlmStateChanged(
                LlmGenerationUiState.FallbackTriggered("Local model: ${e.message}")
            )
            return ruleBasedGenerator.generate(content, config)
        } catch (e: LlmTimeoutException) {
            Log.w(TAG, "generateWithLlmFallback: LLM timeout: ${e.message}")
            llmStateCallback?.onLlmStateChanged(
                LlmGenerationUiState.FallbackTriggered("Local AI timeout")
            )
            return ruleBasedGenerator.generate(content, config)
        } catch (e: LlmInferenceException) {
            Log.e(TAG, "generateWithLlmFallback: LLM inference error: ${e.message}")
            llmStateCallback?.onLlmStateChanged(
                LlmGenerationUiState.FallbackTriggered("Local AI lỗi: ${e.message}")
            )
            return ruleBasedGenerator.generate(content, config)
        } catch (e: Exception) {
            Log.e(TAG, "generateWithLlmFallback: LLM exception: ${e.message}")
            llmStateCallback?.onLlmStateChanged(
                LlmGenerationUiState.FallbackTriggered("Lỗi: ${e.message}")
            )
            return ruleBasedGenerator.generate(content, config)
        }

        // Bước 5: Validate LLM output
        if (llmQuestions.isEmpty()) {
            Log.w(TAG, "generateWithLlmFallback: LLM return rỗng → fallback RULE_BASED")
            llmStateCallback?.onLlmStateChanged(
                LlmGenerationUiState.FallbackTriggered("Local AI không tạo được câu hỏi")
            )
            return ruleBasedGenerator.generate(content, config)
        }

        // Bước 6: Deduplicate với session validator
        val deduplicated = deduplicateQuestions(llmQuestions, config.questionCount)

        Log.d(TAG, "generateWithLlmFallback: LLM thành công, ${deduplicated.size} câu hỏi (sau dedup)")

        val actualCount = deduplicated.size
        val requestedCount = config.questionCount

        // Cập nhật state: thành công hoặc fallback
        if (actualCount < requestedCount) {
            // LLM không đủ → bổ sung bằng rule-based
            llmStateCallback?.onLlmStateChanged(
                LlmGenerationUiState.LlmSuccessPartial(
                    llmCount = actualCount,
                    targetCount = requestedCount,
                    message = "Local AI tạo được $actualCount/$requestedCount câu"
                )
            )
        } else {
            llmStateCallback?.onLlmStateChanged(
                LlmGenerationUiState.LlmSuccess(
                    questionCount = actualCount,
                    message = "Tạo bằng local AI thành công"
                )
            )
        }

        val warning = if (actualCount < requestedCount) {
            if (language == "en") {
                "LLM generated $actualCount questions. Rule-based filled ${
                    requestedCount - actualCount
                } remaining."
            } else {
                "Local AI tạo được $actualCount câu. Rule-based bổ sung ${
                    requestedCount - actualCount
                } câu còn lại."
            }
        } else null

        return QuizSession(
            fileName = content.fileName,
            questionCount = actualCount,
            difficulty = config.difficulty,
            questionType = "Trắc nghiệm 4 đáp án",
            questions = deduplicated,
            generationWarning = warning
        )
    }

    /**
     * Loại bỏ câu hỏi trùng lặp dùng sessionValidator.
     */
    private fun deduplicateQuestions(
        questions: List<com.example.quizfromfileapp.domain.model.QuizQuestion>,
        targetCount: Int
    ): List<com.example.quizfromfileapp.domain.model.QuizQuestion> {
        val result = mutableListOf<com.example.quizfromfileapp.domain.model.QuizQuestion>()

        for (question in questions) {
            if (result.size >= targetCount) break

            val reason = sessionValidator.validate(
                segment = question.sourceSnippet,
                question = question.question,
                options = question.options
            )

            if (reason == null) {
                sessionValidator.markUsed(question.sourceSnippet, question.question, question.options)
                result.add(question)
            }
        }

        return result
    }

    /**
     * Giải phóng tài nguyên khi không cần nữa.
     */
    fun close() {
        (llmGenerator as? OnDeviceLlmQuizGenerator)?.close()
    }
}

/**
 * Callback interface để HybridQuizGenerator cập nhật LLM state cho ViewModel.
 *
 * Các trạng thái:
 * - Idle: chưa bắt đầu
 * - CheckingAvailability: đang kiểm tra model
 * - Generating: đang inference
 * - LlmSuccess: LLM thành công hoàn toàn
 * - LlmSuccessPartial: LLM thành công nhưng ít hơn target
 * - FallbackTriggered: đã fallback sang rule-based
 */
sealed class LlmGenerationUiState {
    data object Idle : LlmGenerationUiState()
    data object CheckingAvailability : LlmGenerationUiState()
    data class Generating(val message: String) : LlmGenerationUiState()
    data class LlmSuccess(val questionCount: Int, val message: String) : LlmGenerationUiState()
    data class LlmSuccessPartial(
        val llmCount: Int,
        val targetCount: Int,
        val message: String
    ) : LlmGenerationUiState()
    data class FallbackTriggered(val reason: String) : LlmGenerationUiState()
}

/**
 * Callback để HybridQuizGenerator notify ViewModel.
 */
interface LlmStateCallback {
    fun onLlmStateChanged(state: LlmGenerationUiState)
}
