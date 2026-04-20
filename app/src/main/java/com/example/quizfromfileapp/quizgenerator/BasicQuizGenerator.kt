package com.example.quizfromfileapp.quizgenerator

import com.example.quizfromfileapp.domain.model.ExtractedContent
import com.example.quizfromfileapp.domain.model.QuizConfig
import com.example.quizfromfileapp.domain.model.QuizQuestion
import com.example.quizfromfileapp.domain.model.QuizSession
import java.util.UUID

interface QuizGenerator {
    suspend fun generate(content: ExtractedContent, config: QuizConfig): QuizSession
}

class BasicQuizGenerator : QuizGenerator {

    override suspend fun generate(content: ExtractedContent, config: QuizConfig): QuizSession {
        val questions = when (config.questionType) {
            "Trắc nghiệm 4 đáp án" -> generateMultipleChoice(content.cleanedText, config.questionCount, config.difficulty)
            else -> generateMultipleChoice(content.cleanedText, config.questionCount, config.difficulty)
        }
        return QuizSession(
            fileName = content.fileName,
            questionCount = config.questionCount,
            difficulty = config.difficulty,
            questionType = config.questionType,
            questions = questions
        )
    }

    private fun generateMultipleChoice(text: String, count: Int, difficulty: String): List<QuizQuestion> {
        val sentences = extractSentences(text)
        val questionList = mutableListOf<QuizQuestion>()

        for (i in 0 until minOf(count, sentences.size)) {
            val sentence = sentences[i].trim()
            if (sentence.length < 10) continue
            val question = createQuestionFromSentence(sentence, i, difficulty)
            if (question != null) {
                questionList.add(question)
            }
        }

        while (questionList.size < count && questionList.size < sentences.size) {
            val idx = questionList.size
            val sentence = sentences[idx % sentences.size].trim()
            if (sentence.length >= 10) {
                createQuestionFromSentence(sentence, idx, difficulty)?.let { questionList.add(it) }
            }
            if (questionList.size >= sentences.size) break
        }

        while (questionList.size < count) {
            questionList.add(createFallbackQuestion(questionList.size))
        }

        return questionList
    }

    private fun extractSentences(text: String): List<String> {
        return text
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .split(Regex("(?<=[.!?])\\s+"))
            .filter { it.length > 10 }
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun createQuestionFromSentence(sentence: String, index: Int, difficulty: String): QuizQuestion? {
        val words = sentence.split(Regex("\\s+"))
        if (words.size < 4) return null

        val keyWord = words.getOrNull(words.size / 2) ?: return null

        val questionText = when {
            sentence.endsWith(".") -> "Câu nào sau đây được mô tả trong nội dung?"
            sentence.contains("không") -> "Thông tin nào đúng về nội dung trên?"
            else -> "Nội dung sau nói về điều gì?"
        }

        val correctIdx = (index % 4)
        val distractors = generateDistractors(keyWord, sentence, correctIdx)

        return QuizQuestion(
            id = "q_${index}_${System.currentTimeMillis()}",
            question = "$questionText\n\"${truncate(sentence, 120)}\"",
            options = distractors,
            correctAnswerIndex = correctIdx,
            explanation = "Đáp án đúng dựa trên nội dung: \"${truncate(sentence, 80)}\""
        )
    }

    private fun generateDistractors(keyWord: String, sentence: String, correctIdx: Int): List<String> {
        val baseOptions = listOf(
            "Đúng với nội dung",
            "Không liên quan đến nội dung",
            "Ngược lại với nội dung",
            "Thông tin không chính xác"
        )
        val shuffled = baseOptions.shuffled().toMutableList()
        shuffled[correctIdx] = "Đúng với nội dung được mô tả"
        return shuffled.take(4)
    }

    private fun createFallbackQuestion(index: Int): QuizQuestion {
        val questions = listOf(
            QuizQuestion(
                id = "fallback_$index",
                question = "Câu hỏi mẫu ${index + 1} — Hãy chọn đáp án đúng",
                options = listOf("Đáp án A", "Đáp án B", "Đáp án C", "Đáp án D"),
                correctAnswerIndex = 0,
                explanation = "Đây là câu hỏi mẫu từ nội dung được trích xuất."
            )
        )
        return questions[index % questions.size].copy(id = "fallback_${index}_${UUID.randomUUID()}")
    }

    private fun truncate(text: String, maxLen: Int): String {
        return if (text.length > maxLen) text.take(maxLen - 3) + "..." else text
    }
}