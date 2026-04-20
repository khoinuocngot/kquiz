package com.example.quizfromfileapp.ui.screens.processing

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizfromfileapp.data.model.ImportedFile
import com.example.quizfromfileapp.domain.model.ExtractedContent
import com.example.quizfromfileapp.domain.usecase.ExtractTextFromFileUseCase
import com.example.quizfromfileapp.ui.screens.AppSharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ProcessingViewModel"

sealed class ProcessingUiState {
    data object Idle : ProcessingUiState()
    data class Extracting(val fileName: String) : ProcessingUiState()
    data class Success(val content: ExtractedContent) : ProcessingUiState()
    data class Error(val message: String) : ProcessingUiState()
}

class ProcessingViewModel(
    private val context: Context,
    private val sharedViewModel: AppSharedViewModel
) : ViewModel() {

    private val useCase = ExtractTextFromFileUseCase(context)

    private val _uiState = MutableStateFlow<ProcessingUiState>(ProcessingUiState.Idle)
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()

    fun startExtraction() {
        val file = sharedViewModel.selectedFile.value

        if (file == null) {
            Log.e(TAG, "startExtraction: selectedFile == null")
            _uiState.value = ProcessingUiState.Error("Chưa có file mà bạn ê")
            return
        }

        // Chỉ gọi extract khi đang ở trạng thái Idle hoặc Error (tránh gọi lại khi đã Success)
        val currentState = _uiState.value
        if (currentState is ProcessingUiState.Extracting || currentState is ProcessingUiState.Success) {
            Log.w(TAG, "startExtraction: bỏ qua vì đang ở trạng thái $currentState")
            return
        }

        Log.d(TAG, "startExtraction: bắt đầu cho file=${file.name}, mimeType=${file.mimeType}, uri=${file.uri}")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { ProcessingUiState.Extracting(file.name) }
                Log.d(TAG, "startExtraction: state=Extracting")

                val result = useCase.execute(file)

                result.fold(
                    onSuccess = { content ->
                        Log.d(TAG, "startExtraction: SUCCESS, rawCharCount=${content.rawCharCount}, " +
                "cleanedCharCount=${content.cleanedCharCount}, segments=${content.segmentCount}")
                        sharedViewModel.setExtractedContent(content)
                        _uiState.update { ProcessingUiState.Success(content) }
                    },
                    onFailure = { error ->
                        val message = error.message ?: "Lỗi không xác định"
                        Log.e(TAG, "startExtraction: FAILURE ${error.javaClass.simpleName}: $message", error)
                        _uiState.update { ProcessingUiState.Error(message) }
                    }
                )
            } catch (e: Exception) {
                val message = e.message ?: "Lỗi không xác định"
                Log.e(TAG, "startExtraction: EXCEPTION ${e.javaClass.simpleName}: $message", e)
                _uiState.update { ProcessingUiState.Error(message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { ProcessingUiState.Idle }
    }
}
