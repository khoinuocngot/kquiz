package com.example.quizfromfileapp.quizgenerator

/**
 * Các exception dùng trong LLM generation pipeline.
 * Tất cả đều được catch ở OnDeviceLlmQuizGenerator → fallback rule-based.
 */

/** Model LLM không khả dụng — throw khi init thất bại hoặc class không tồn tại */
class LlmNotAvailableException(message: String, cause: Throwable? = null)
    : Exception(message, cause)

/** LLM inference timeout */
class LlmTimeoutException(message: String) : Exception(message)

/** LLM inference lỗi khi đang chạy */
class LlmInferenceException(message: String, cause: Throwable? = null)
    : Exception(message, cause)
