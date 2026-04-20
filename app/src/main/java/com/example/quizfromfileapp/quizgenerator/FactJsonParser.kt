package com.example.quizfromfileapp.quizgenerator

import android.util.Log

/**
 * Parse JSON response từ fact extraction LLM call.
 *
 * Input: JSON string từ LLM
 * Output: List<FactItem>
 *
 * Expected schema:
 * {"facts":[{"concept":"...","factStatement":"...","sourceSnippet":"..."}]}
 */
object FactJsonParser {

    private const val TAG = "FactJsonParser"

    /**
     * Parse fact extraction JSON response.
     *
     * @param rawResponse Raw JSON string từ LLM
     * @return Danh sách FactItem đã parse
     */
    fun parse(rawResponse: String): List<FactItem> {
        if (rawResponse.isBlank()) {
            logd("parse: response rỗng")
            return emptyList()
        }

        val cleanJson = LlmJsonExtractor.extractCleanJson(rawResponse)
        if (cleanJson == null) {
            loge("extractCleanJson failed")
            return emptyList()
        }

        return try {
            parseJsonStructure(cleanJson)
        } catch (e: Exception) {
            loge("parseJsonStructure lỗi: ${e.message}")
            emptyList()
        }
    }

    private fun parseJsonStructure(jsonString: String): List<FactItem> {
        return try {
            val json = org.json.JSONObject(jsonString)
            val factsArray = json.optJSONArray("facts")

            if (factsArray == null || factsArray.length() == 0) {
                // Thử parse dưới dạng array trực tiếp
                val directArray = try {
                    org.json.JSONArray(jsonString)
                } catch (_: Exception) { null }

                if (directArray != null) {
                    return parseDirectArray(directArray)
                }

                logw("Không tìm thấy 'facts' array trong JSON")
                return emptyList()
            }

            val result = mutableListOf<FactItem>()
            for (i in 0 until factsArray.length()) {
                val factObj = factsArray.getJSONObject(i)
                val fact = parseFactObject(factObj, i)
                if (fact != null) {
                    result.add(fact)
                }
            }

            logd("parseFacts: ${result.size}/${factsArray.length()} facts hợp lệ")
            result
        } catch (e: Exception) {
            loge("parseJsonStructure lỗi: ${e.message}")
            emptyList()
        }
    }

    private fun parseDirectArray(array: org.json.JSONArray): List<FactItem> {
        val result = mutableListOf<FactItem>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i)
            if (item != null) {
                val fact = parseFactObject(item, i)
                if (fact != null) result.add(fact)
            }
        }
        return result
    }

    private fun parseFactObject(obj: org.json.JSONObject, index: Int): FactItem? {
        val concept = obj.optString("concept", "").trim()
        val factStatement = obj.optString("factStatement", "").trim()
        val sourceSnippet = obj.optString("sourceSnippet", "").trim().take(200)

        if (factStatement.isBlank()) {
            logw("parseFact[$index]: factStatement rỗng")
            return null
        }

        if (factStatement.length < 20) {
            logw("parseFact[$index]: factStatement quá ngắn (<20 chars)")
            return null
        }

        if (factStatement.length > 250) {
            logw("parseFact[$index]: factStatement quá dài (>250 chars)")
            return null
        }

        // Extract page info
        val sourcePageStart = if (obj.has("sourcePageStart")) {
            obj.optInt("sourcePageStart", -1).takeIf { it > 0 }
        } else null
        val sourcePageEnd = if (obj.has("sourcePageEnd")) {
            obj.optInt("sourcePageEnd", -1).takeIf { it > 0 }
        } else null

        val sourceType = obj.optString("sourceType", "MERGED")

        // Nếu không có concept → extract từ factStatement
        val finalConcept = if (concept.isBlank()) {
            extractConceptFromFact(factStatement)
        } else {
            concept.take(40)
        }

        // Validate concept
        if (finalConcept.isBlank()) {
            logw("parseFact[$index]: concept rỗng sau khi extract")
            return null
        }

        return FactItem(
            id = "fact_${index}_${System.currentTimeMillis()}",
            concept = finalConcept,
            factStatement = factStatement,
            sourceSnippet = sourceSnippet,
            sourcePageStart = sourcePageStart,
            sourcePageEnd = sourcePageEnd,
            sourceType = sourceType,
            confidence = 0.5f  // Placeholder, sẽ được recalculate bởi FactQualityScorer
        )
    }

    /**
     * Extract concept từ factStatement khi LLM không trả concept.
     */
    private fun extractConceptFromFact(factStatement: String): String {
        val words = factStatement.trim().split(Regex("""[\s]+"""))

        // Skip common opening words
        val skipWords = setOf(
            "the", "a", "an", "this", "these", "it", "that", "is", "are",
            "was", "were", "be", "been", "being",
            "và", "là", "của", "được", "có", "một", "các", "những", "này", "đó"
        )

        val meaningful = words.dropWhile {
            skipWords.contains(it.lowercase().replace(Regex("""[^\p{L}]"""), ""))
        }.filter {
            val cleaned = it.lowercase().replace(Regex("""[^\p{L}]"""), "")
            cleaned.length >= 3 && !skipWords.contains(cleaned)
        }

        return if (meaningful.isNotEmpty()) {
            meaningful.take(3).joinToString(" ").take(40)
        } else {
            factStatement.take(30)
        }
    }

    private fun logd(message: String) {
        if (LlmGenerationConfig.DEBUG_LOGGING) Log.d(TAG, message)
    }

    private fun logw(message: String) {
        Log.w(TAG, message)
    }

    private fun loge(message: String) {
        Log.e(TAG, message)
    }
}
