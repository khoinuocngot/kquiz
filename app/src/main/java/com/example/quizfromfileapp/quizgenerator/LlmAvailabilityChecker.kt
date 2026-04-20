package com.example.quizfromfileapp.quizgenerator

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Kiểm tra xem local LLM model đã sẵn sàng hay chưa.
 *
 * Hỗ trợ 2 cơ chế:
 * 1. File-based: Kiểm tra model file trong assets hoặc internal storage
 * 2. MlKit LLM API: Kiểm tra qua LlmInference API (không cần file riêng)
 *
 * Nếu model chưa sẵn sàng → fallback về rule-based.
 * KHÔNG crash, luôn return false nếu không chắc chắn.
 */
object LlmAvailabilityChecker {

    private const val TAG = "LlmAvailabilityChecker"

    /**
     * Kiểm tra toàn diện: model file hoặc MlKit LLM API.
     *
     * @param context  Android context để truy cập assets/files
     * @return         true nếu model khả dụng, false otherwise
     */
    fun isModelAvailable(context: Context): Boolean {
        // Ưu tiên 1: Kiểm tra MlKit LLM API (không cần model file riêng)
        if (isMlKitLlmApiAvailableSync()) {
            logd("MlKit LLM API: CÓ thể khả dụng (cần tải model lần đầu)")
            // MlKit LLM có thể khả dụng ngay hoặc sẽ tự tải model
            return true
        }

        // Ưu tiên 2: Kiểm tra model file trong assets
        if (isModelFileAvailable(context)) {
            logd("Model file: TÌM THẤY trong assets")
            return true
        }

        // Ưu tiên 3: Kiểm tra model đã download sẵn trong internal storage
        if (isDownloadedModelAvailable(context)) {
            logd("Downloaded model: TÌM THẤY trong internal storage")
            return true
        }

        logd("Không tìm thấy model nào — fallback rule-based")
        return false
    }

    /**
     * Kiểm tra MlKit LLM API có thể được sử dụng không (phiên bản sync).
     * Dùng cho LocalLlmManager khi khởi tạo.
     */
    fun isMlKitLlmApiAvailableSync(): Boolean {
        return try {
            Class.forName("com.google.mlkit.nl.inference.LlmInference")
            true
        } catch (_: ClassNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Kiểm tra model file trong assets.
     *
     * @return true nếu file tồn tại và kích thước hợp lệ
     */
    private fun isModelFileAvailable(context: Context): Boolean {
        val assetPath = LlmGenerationConfig.MODEL_ASSET_PATH ?: return false
        return try {
            val assetManager = context.assets
            val files = assetManager.list("") ?: emptyArray()
            // Kiểm tra xem có thư mục/prefix phù hợp không
            val hasAsset = files.any { it == assetPath || it.startsWith("llm/") || assetPath.startsWith(it) }
            if (hasAsset) {
                // Thử mở file để xác nhận
                val size = assetManager.open(assetPath).use { it.available().toLong() }
                val minSize = LlmGenerationConfig.MIN_MODEL_FILE_SIZE_BYTES
                if (minSize > 0 && size < minSize) {
                    logd("Model file nhỏ hơn kích thước tối thiểu: $size < $minSize")
                    return false
                }
                logd("Model file trong assets: $assetPath (${size}B)")
                true
            } else {
                logd("Model file không tìm thấy trong assets: $assetPath")
                false
            }
        } catch (e: Exception) {
            logd("Model file check lỗi: ${e.message}")
            false
        }
    }

    /**
     * Kiểm tra model đã được download sẵn trong internal storage.
     *
     * @return true nếu model file tồn tại trong app's files directory
     */
    private fun isDownloadedModelAvailable(context: Context): Boolean {
        val assetPath = LlmGenerationConfig.MODEL_ASSET_PATH ?: return false
        val modelName = File(assetPath).name
        val modelFile = File(context.filesDir, "llm/$modelName")
        return try {
            if (modelFile.exists()) {
                val size = modelFile.length()
                val minSize = LlmGenerationConfig.MIN_MODEL_FILE_SIZE_BYTES
                if (minSize > 0 && size < minSize) {
                    logd("Downloaded model nhỏ hơn kích thước tối thiểu: $size < $minSize")
                    return false
                }
                logd("Downloaded model: ${modelFile.absolutePath} (${size}B)")
                true
            } else {
                logd("Downloaded model không tìm thấy: ${modelFile.absolutePath}")
                false
            }
        } catch (e: Exception) {
            logd("Downloaded model check lỗi: ${e.message}")
            false
        }
    }

    /**
     * Trả về mô tả trạng thái model cho debug UI.
     */
    fun getModelStatusDescription(context: Context): String {
        val hasMlKit = isMlKitLlmApiAvailableSync()
        val hasAsset = isModelFileAvailable(context)
        val hasDownloaded = isDownloadedModelAvailable(context)

        return when {
            hasMlKit -> "MlKit LLM API sẵn sàng (model sẽ được tải khi cần)"
            hasAsset -> "Model file trong assets: ${LlmGenerationConfig.MODEL_ASSET_PATH}"
            hasDownloaded -> "Model đã download: ${context.filesDir}/llm/"
            else -> "Chưa có model. Sử dụng rule-based."
        }
    }

    private fun logd(message: String) {
        if (LlmGenerationConfig.DEBUG_LOGGING) {
            Log.d(TAG, message)
        }
    }
}
