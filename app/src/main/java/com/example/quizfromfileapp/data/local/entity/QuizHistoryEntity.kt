package com.example.quizfromfileapp.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class QuizHistoryEntity(
    val id: Long = 0,
    val fileName: String,
    val scorePercent: Int,
    val correctCount: Int,
    val totalQuestions: Int,
    val difficulty: String,
    val questionType: String,
    val createdAt: Long
)
