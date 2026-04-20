package com.example.quizfromfileapp.quizgenerator

/**
 * Cấu hình cho quá trình sinh quiz bằng local LLM.
 *
 * Tất cả giá trị có thể điều chỉnh được.
 * Nếu model local chưa sẵn sàng, generator sẽ fallback về rule-based.
 */
object LlmGenerationConfig {

    /** Số segment tối đa mỗi batch gửi cho LLM — tránh quá tải context */
    const val MAX_SEGMENTS_PER_BATCH = 8

    /** Số ký tự tối đa mỗi segment gửi cho LLM */
    const val MAX_CHARS_PER_SEGMENT = 200

    /** Số câu hỏi tối đa LLM sinh trong một batch */
    const val MAX_OUTPUT_QUESTIONS = 8

    /** Timeout cho một lần inference (milliseconds) */
    const val GENERATION_TIMEOUT_MS = 90_000L // 90 giây

    /** Số ký tự tối đa cho output JSON — tránh LLM generate quá dài */
    const val MAX_OUTPUT_CHARS = 12_000

    /**
     * Đường dẫn tương đối của model file trong assets.
     * Nếu model chưa có trong assets, LLM sẽ không khả dụng và fallback.
     *
     * Ví dụ:
     * - "llm/quiz_model.bin"
     * - "gemma-2b-it.bin"
     *
     * Đặt null nếu dùng MlKit LLM API (không cần file riêng).
     */
    val MODEL_ASSET_PATH: String? = null

    /**
     * Tên model dùng để hiển thị trong UI.
     * Ví dụ: "Gemma 2B", "Phi-3 Mini", "MlKit LLM"
     */
    const val MODEL_DISPLAY_NAME = "MlKit LLM (Local)"

    /**
     * Kích thước tối thiểu (bytes) để xem là model file hợp lệ.
     * Nếu file nhỏ hơn, coi như chưa có model.
     * Đặt 0 để bỏ qua kiểm tra.
     */
    const val MIN_MODEL_FILE_SIZE_BYTES = 0L

    /**
     * Số batch tối đa chạy liên tiếp nếu cần sinh nhiều câu hỏi hơn.
     * VD: targetCount=15, MAX_OUTPUT_QUESTIONS=8 → cần 2 batch.
     */
    const val MAX_BATCHES = 3

    /**
     * Có bật debug log cho LLM generation không.
     * Đặt false khi release.
     */
    const val DEBUG_LOGGING = true
}
