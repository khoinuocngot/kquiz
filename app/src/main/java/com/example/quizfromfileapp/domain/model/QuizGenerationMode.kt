package com.example.quizfromfileapp.domain.model

import kotlinx.serialization.Serializable

/**
 * Chế độ sinh quiz.
 *
 * - RULE_BASED: dùng SmartQuizGenerator (rule-based, không LLM)
 * - LLM_ASSISTED: dùng local on-device LLM để sinh câu hỏi, fallback về rule-based nếu fail
 */
@Serializable
enum class QuizGenerationMode {
    /** Sinh bằng rule-based (SmartQuizGenerator) — không dùng LLM */
    RULE_BASED,

    /** Sinh bằng local on-device LLM, fallback về rule-based nếu fail */
    LLM_ASSISTED;

    /** Tên hiển thị cho UI */
    val displayName: String get() = when (this) {
        RULE_BASED -> "Rule-based"
        LLM_ASSISTED -> "LLM-assisted"
    }

    /** Mô tả cho UI */
    val description: String get() = when (this) {
        RULE_BASED -> "Dùng quy tắc cố định, nhanh, không cần AI"
        LLM_ASSISTED -> "Dùng AI trên thiết bị để sinh câu hỏi tự nhiên hơn"
    }

    companion object {
        val ALL = entries.toList()
        val DEFAULT = RULE_BASED
    }
}
