package com.example.quizfromfileapp.domain.usecase

import android.content.Context
import com.example.quizfromfileapp.domain.model.ExtractedContent
import com.example.quizfromfileapp.domain.model.QuizConfig
import com.example.quizfromfileapp.domain.model.QuizSession
import com.example.quizfromfileapp.quizgenerator.HybridQuizGenerator
import com.example.quizfromfileapp.quizgenerator.LlmStateCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * UseCase sinh quiz.
 *
 * Chọn generator theo config.generationMode:
 * - RULE_BASED → HybridQuizGenerator → SmartQuizGenerator (rule-based)
 * - LLM_ASSISTED → HybridQuizGenerator → OnDeviceLlmQuizGenerator → fallback SmartQuizGenerator
 *
 * HybridQuizGenerator tự động xử lý fallback nếu LLM fail.
 *
 * @param context  Android context (để kiểm tra model availability)
 * @param callback Optional callback để nhận LLM state updates
 */
class GenerateQuizUseCase(
    private val context: Context,
    private val callback: LlmStateCallback? = null
) {
    private val generator = HybridQuizGenerator(context, callback)

    suspend fun generate(content: ExtractedContent, config: QuizConfig): QuizSession {
        return withContext(Dispatchers.Default) {
            generator.generate(content, config)
        }
    }

    /**
     * Giải phóng tài nguyên generator (đặc biệt là LLM model).
     */
    fun close() {
        generator.close()
    }
}
