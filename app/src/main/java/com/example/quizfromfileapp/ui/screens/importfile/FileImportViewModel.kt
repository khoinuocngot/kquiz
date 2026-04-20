package com.example.quizfromfileapp.ui.screens.importfile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizfromfileapp.data.helper.FileMetadataHelper
import com.example.quizfromfileapp.data.model.ImportedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "FileImportViewModel"

sealed class FileImportUiState {
    data object Idle : FileImportUiState()
    data class FileSelected(val file: ImportedFile) : FileImportUiState()
    data class Error(val message: String) : FileImportUiState()
}

class FileImportViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<FileImportUiState>(FileImportUiState.Idle)
    val uiState: StateFlow<FileImportUiState> = _uiState.asStateFlow()

    fun processSelectedUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "processSelectedUri: uri=${uri}")

                val mimeType = context.contentResolver.getType(uri)
                Log.d(TAG, "processSelectedUri: mimeType=$mimeType")

                if (mimeType == null) {
                    Log.e(TAG, "processSelectedUri: mimeType == null cho uri=$uri")
                    _uiState.update {
                        FileImportUiState.Error(message = "Không xác định được loại file")
                    }
                    return@launch
                }

                if (!FileMetadataHelper.isSupported(mimeType)) {
                    Log.w(TAG, "processSelectedUri: mimeType không được hỗ trợ: $mimeType")
                    _uiState.update {
                        FileImportUiState.Error(message = "Định dạng file chưa được hỗ trợ: $mimeType")
                    }
                    return@launch
                }

                val file = FileMetadataHelper.extractFromUri(context, uri)
                if (file != null) {
                    Log.d(TAG, "processSelectedUri: extracted file name=${file.name}, size=${file.formattedSize}")
                    _uiState.update { FileImportUiState.FileSelected(file) }
                } else {
                    Log.e(TAG, "processSelectedUri: extractFromUri trả về null")
                    _uiState.update {
                        FileImportUiState.Error(message = "Không đọc được metadata file")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "processSelectedUri: EXCEPTION ${e.javaClass.simpleName}: ${e.message}", e)
                _uiState.update {
                    FileImportUiState.Error(message = "Lỗi khi xử lý file: ${e.message}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { FileImportUiState.Idle }
    }

    fun clearSelection() {
        _uiState.value = FileImportUiState.Idle
    }

    fun getSelectedFile(): ImportedFile? {
        return (_uiState.value as? FileImportUiState.FileSelected)?.file
    }
}
