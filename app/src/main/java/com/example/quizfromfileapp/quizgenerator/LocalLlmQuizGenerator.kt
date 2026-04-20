package com.example.quizfromfileapp.quizgenerator

import com.example.quizfromfileapp.domain.model.ContentSegment
import com.example.quizfromfileapp.domain.model.QuizQuestion

/**
 * Interface cho bộ sinh quiz dùng local on-device LLM.
 *
 * Các implementation cụ thể sẽ gọi LLM API (MlKit, Ollama, Gemma, v.v.)
 * và trả về danh sách QuizQuestion đã parse được.
 */
interface LocalLlmQuizGenerator {

    /**
     * Kiểm tra LLM có sẵn sàng để sử dụng hay không.
     * Implementations nên cache trạng thái readiness.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Sinh câu hỏi từ các segment chất lượng cao.
     *
     * @param segments       Danh sách segment đã lọc, đã sorted theo chất lượng
     * @param targetCount    Số câu hỏi cần sinh (có thể sinh nhiều hơn nếu cần)
     * @param language       Ngôn ngữ của nội dung nguồn ("vi" hoặc "en")
     * @return              Danh sách QuizQuestion đã parse, hoặc rỗng nếu fail
     */
    suspend fun generateQuestions(
        segments: List<ContentSegment>,
        targetCount: Int,
        language: String
    ): List<QuizQuestion>
}
