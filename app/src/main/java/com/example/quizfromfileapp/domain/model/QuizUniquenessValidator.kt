package com.example.quizfromfileapp.domain.model

/**
 * Validator deduplication toàn bộ quiz session.
 *
 * Theo dõi trong một session:
 * - usedSourceSegments: segment đã dùng rồi (tránh dùng 2 lần)
 * - usedQuestionTexts: question fingerprint đã dùng
 * - usedOptionSets: option set fingerprints đã dùng
 *
 * Trước khi thêm câu hỏi vào quiz, phải kiểm tra TẤT CẢ
 * để đảm bảo không trùng lặp.
 */
class QuizUniquenessValidator {

    private val usedSourceSegmentTexts = mutableSetOf<String>()
    private val usedQuestionFingerprints = mutableSetOf<String>()
    private val usedOptionSetFingerprints = mutableSetOf<String>()
    private val usedOptionSetList = mutableListOf<List<String>>()

    /**
     * Reset validator cho một quiz session mới.
     */
    fun reset() {
        usedSourceSegmentTexts.clear()
        usedQuestionFingerprints.clear()
        usedOptionSetFingerprints.clear()
        usedOptionSetList.clear()
    }

    /**
     * Kiểm tra segment đã được dùng làm nguồn câu hỏi chưa.
     */
    fun isSegmentUsed(segment: String): Boolean {
        // Kiểm tra trùng gần (với similarity cao)
        return usedSourceSegmentTexts.any { QuestionFingerprint.isDuplicate(it, segment) }
    }

    /**
     * Kiểm tra câu hỏi đã tồn tại trong session chưa.
     */
    fun isQuestionDuplicate(question: String): Boolean {
        val fp = QuestionFingerprint.fingerprint(question)
        return usedQuestionFingerprints.contains(fp) ||
                usedQuestionFingerprints.any {
                    QuestionFingerprint.isDuplicate(question, it)
                }
    }

    /**
     * Kiểm tra option set đã tồn tại trong session chưa.
     */
    fun isOptionSetDuplicate(options: List<String>): Boolean {
        if (options.size < 4) return true
        val fp = OptionSetFingerprint.fingerprint(options)
        return usedOptionSetFingerprints.contains(fp) ||
                OptionSetFingerprint.isDuplicateOfAny(options, usedOptionSetList)
    }

    /**
     * Kiểm tra option set có gần-trùng (overlap cao) không.
     */
    fun isOptionSetNearDuplicate(options: List<String>): Boolean {
        return OptionSetFingerprint.isNearDuplicate(options, usedOptionSetList)
    }

    /**
     * Kiểm tra option set có suspicious uniform (có thể là generic) không.
     */
    fun isOptionSetSuspicious(options: List<String>): Boolean {
        return OptionSetFingerprint.isSuspiciouslyUniform(options)
    }

    /**
     * Kiểm tra đầy đủ trước khi thêm câu hỏi.
     *
     * @return null nếu hợp lệ, String (lý do) nếu reject
     */
    fun validate(
        segment: String,
        question: String,
        options: List<String>
    ): String? {
        // 1. Check segment đã dùng
        if (isSegmentUsed(segment)) {
            return "Segment nguồn đã được dùng (trùng nội dung)"
        }

        // 2. Check question trùng
        if (isQuestionDuplicate(question)) {
            return "Câu hỏi trùng với câu trước đó"
        }

        // 3. Check question trong blacklist
        if (GenericFallbackBlacklist.isForbiddenQuestion(question)) {
            return "Câu hỏi nằm trong fallback blacklist"
        }

        // 4. Check options trong blacklist
        if (GenericFallbackBlacklist.hasForbiddenOptions(options)) {
            return "Options chứa từ trong fallback blacklist"
        }

        // 5. Check option set trùng
        if (isOptionSetDuplicate(options)) {
            return "Option set trùng với câu trước đó"
        }

        // 6. Check option set gần-trùng
        if (isOptionSetNearDuplicate(options)) {
            return "Option set gần-trùng với câu trước đó"
        }

        // 7. Check suspicious uniform
        if (isOptionSetSuspicious(options)) {
            return "Option set quá đều nhau (có thể là generic)"
        }

        return null // Hợp lệ
    }

    /**
     * Đánh dấu segment/question/options đã dùng.
     * Chỉ gọi SAU KHI validate thành công.
     */
    fun markUsed(
        segment: String,
        question: String,
        options: List<String>
    ) {
        usedSourceSegmentTexts.add(segment)
        usedQuestionFingerprints.add(QuestionFingerprint.fingerprint(question))
        usedOptionSetFingerprints.add(OptionSetFingerprint.fingerprint(options))
        usedOptionSetList.add(options)
    }

    /**
     * Số câu hỏi hiện tại trong session.
     */
    fun questionCount(): Int = usedQuestionFingerprints.size
}
