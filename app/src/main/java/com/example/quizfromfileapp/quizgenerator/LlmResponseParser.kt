package com.example.quizfromfileapp.quizgenerator

import android.util.Log
import com.example.quizfromfileapp.domain.model.QuizQuestion
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parse response JSON từ local LLM.
 *
 * Output chuẩn của LLM phải có format:
 * {
 *   "questions": [
 *     {
 *       "question": "...",
 *       "options": ["...", "...", "...", "..."],
 *       "correctAnswerIndex": 0,
 *       "explanation": "...",
 *       "sourceSnippet": "...",
 *       "sourcePageStart": 1,
 *       "sourcePageEnd": 1
 *     }
 *   ]
 * }
 *
 * Fallback: return rỗng nếu không parse được hoặc format sai.
 */
object LlmResponseParser {

    private const val TAG = "LlmResponseParser"

    /**
     * Parse JSON string thành danh sách QuizQuestion.
     *
     * @param jsonResponse  Raw JSON string từ LLM
     * @return              Danh sách QuizQuestion, hoặc rỗng nếu parse fail
     */
    fun parse(jsonResponse: String): List<QuizQuestion> {
        if (jsonResponse.isBlank()) {
            Log.w(TAG, "parse: response rỗng")
            return emptyList()
        }

        return try {
            val json = JSONObject(jsonResponse)
            val questionsArray = json.getJSONArray("questions")

            val result = mutableListOf<QuizQuestion>()
            for (i in 0 until questionsArray.length()) {
                val qObj = questionsArray.getJSONObject(i)
                val question = parseQuestion(qObj, i)
                if (question != null) {
                    result.add(question)
                }
            }

            Log.d(TAG, "parse: thành công, ${result.size} câu hỏi")
            result
        } catch (e: Exception) {
            Log.e(TAG, "parse: JSON parse thất bại: ${e.message}\nResponse: $jsonResponse")
            emptyList()
        }
    }

    private fun parseQuestion(obj: JSONObject, index: Int): QuizQuestion? {
        return try {
            val questionText = obj.optString("question", "").trim()
            if (questionText.isBlank()) return null

            val optionsArray = obj.optJSONArray("options")
            if (optionsArray == null || optionsArray.length() < 4) {
                Log.w(TAG, "parseQuestion[$index]: options < 4, bỏ qua")
                return null
            }

            val options = (0 until optionsArray.length())
                .map { optionsArray.optString(it, "").trim() }
                .filter { it.isNotBlank() }

            if (options.size < 4) {
                Log.w(TAG, "parseQuestion[$index]: options sau filter < 4, bỏ qua")
                return null
            }

            val correctIdx = obj.optInt("correctAnswerIndex", -1)
            if (correctIdx !in options.indices) {
                Log.w(TAG, "parseQuestion[$index]: correctAnswerIndex=$correctIdx không hợp lệ, dùng 0")
            }

            val sourceSnippet = obj.optString("sourceSnippet", "").trim()
                .take(200)
            val sourcePageStart = if (obj.has("sourcePageStart")) obj.optInt("sourcePageStart", -1).takeIf { it > 0 } else null
            val sourcePageEnd = if (obj.has("sourcePageEnd")) obj.optInt("sourcePageEnd", -1).takeIf { it > 0 } else null
            val explanation = obj.optString("explanation", "").trim()

            QuizQuestion(
                id = "llm_q_${index}_${System.currentTimeMillis()}",
                question = questionText,
                options = options.take(4),
                correctAnswerIndex = correctIdx.coerceIn(0, options.size - 1),
                explanation = explanation,
                sourcePageStart = sourcePageStart,
                sourcePageEnd = sourcePageEnd,
                sourceSnippet = sourceSnippet,
                sourceType = "LLM"
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseQuestion[$index]: parse thất bại: ${e.message}")
            null
        }
    }
}
