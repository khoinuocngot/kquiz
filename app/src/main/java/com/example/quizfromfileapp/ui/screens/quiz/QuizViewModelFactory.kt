package com.example.quizfromfileapp.ui.screens.quiz

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.quizfromfileapp.ui.screens.AppSharedViewModel

/**
 * Factory để tạo QuizViewModel với Application context.
 *
 * QuizViewModel cần Application context để khởi tạo GenerateQuizUseCase
 * (để truyền context vào HybridQuizGenerator cho việc kiểm tra model).
 */
class QuizViewModelFactory(
    private val application: Application,
    private val sharedViewModel: AppSharedViewModel
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuizViewModel::class.java)) {
            return QuizViewModel(application, sharedViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
