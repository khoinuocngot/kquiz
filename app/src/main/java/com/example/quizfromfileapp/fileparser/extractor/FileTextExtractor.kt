package com.example.quizfromfileapp.fileparser.extractor

import android.content.Context
import android.net.Uri
import com.example.quizfromfileapp.domain.model.ExtractedContent

interface FileTextExtractor {
    suspend fun extract(context: Context, uri: Uri, fileName: String, mimeType: String): Result<ExtractedContent>
    fun canHandle(mimeType: String): Boolean
}
