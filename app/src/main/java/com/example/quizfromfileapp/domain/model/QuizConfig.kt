package com.example.quizfromfileapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class QuizConfig(
    val questionCount: Int = 10,
    val difficulty: String = "Trung bình",
    val questionType: String = "Trắc nghiệm 4 đáp án",
    val generationMode: QuizGenerationMode = QuizGenerationMode.DEFAULT
) {
    companion object {
        val DIFFICULTY_OPTIONS = listOf("Dễ", "Trung bình", "Khó")
        val QUESTION_TYPE_OPTIONS = listOf(
            "Trắc nghiệm 4 đáp án",
            "Đúng / Sai",
            "Điền khuyết"
        )
        const val MIN_QUESTIONS = 5
        const val MAX_QUESTIONS = 20
    }
}
