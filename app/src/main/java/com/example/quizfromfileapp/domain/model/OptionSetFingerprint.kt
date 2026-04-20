package com.example.quizfromfileapp.domain.model

/**
 * Fingerprint cho option set — dùng để deduplicate giữa các câu hỏi.
 *
 * Hai câu hỏi được coi là trùng option set nếu:
 * - 4 options giống nhau (sau khi sort/normalize)
 * - Hoặc 3/4 options giống nhau và thứ tự khác
 */
object OptionSetFingerprint {

    private const val OVERLAP_THRESHOLD = 0.70

    /**
     * Kiểm tra hai option sets có trùng nhau không.
     *
     * @param setA danh sách 4 options của câu 1
     * @param setB danh sách 4 options của câu 2
     * @return true nếu trùng option set
     */
    fun isDuplicate(setA: List<String>, setB: List<String>): Boolean {
        if (setA.size < 4 || setB.size < 4) return false
        val fpA = fingerprint(setA)
        val fpB = fingerprint(setB)
        return fpA == fpB
    }

    /**
     * Kiểm tra option set mới có trùng với bất kỳ set nào trong danh sách không.
     */
    fun isDuplicateOfAny(
        newOptions: List<String>,
        existingOptionSets: List<List<String>>
    ): Boolean {
        return existingOptionSets.any { isDuplicate(newOptions, it) }
    }

    /**
     * Tính overlap ratio giữa hai option sets.
     * @return giá trị 0.0 – 1.0
     */
    fun overlap(setA: List<String>, setB: List<String>): Double {
        if (setA.size < 4 || setB.size < 4) return 0.0
        val fpA = setA.map { optionFingerprint(it) }.toSet()
        val fpB = setB.map { optionFingerprint(it) }.toSet()
        val intersection = fpA.intersect(fpB).size
        val total = maxOf(fpA.size, fpB.size)
        return intersection.toDouble() / total
    }

    /**
     * Kiểm tra option set mới có gần-trùng (overlap > THRESHOLD) với existing không.
     */
    fun isNearDuplicate(
        newOptions: List<String>,
        existingOptionSets: List<List<String>>
    ): Boolean {
        return existingOptionSets.any { overlap(newOptions, it) > OVERLAP_THRESHOLD }
    }

    /**
     * Tạo fingerprint ổn định cho option set.
     * Sắp xếp options để tạo fingerprint không phụ thuộc thứ tự.
     */
    fun fingerprint(options: List<String>): String {
        if (options.size < 4) return ""
        return options
            .map { optionFingerprint(it) }
            .sorted()
            .joinToString("|")
    }

    /**
     * Tạo fingerprint cho một option đơn lẻ.
     */
    fun optionFingerprint(option: String): String {
        return option
            .lowercase()
            .replace(Regex("""[.!?,\(\)\[\]{}'"]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(60)
    }

    /**
     * Kiểm tra 4 options có cùng độ dài quá (uniform) không.
     * Nếu tất cả quá giống nhau về độ dài → có thể là generic.
     */
    fun isSuspiciouslyUniform(options: List<String>): Boolean {
        if (options.size < 4) return false
        val lengths = options.map { it.length }
        val avg = lengths.average()
        val variance = lengths.map { (it - avg) * (it - avg) }.average()
        val stdDev = kotlin.math.sqrt(variance)
        // Nếu độ lệch chuẩn < 5 và trung bình < 80 → có thể là template
        return stdDev < 5 && avg < 80
    }
}
