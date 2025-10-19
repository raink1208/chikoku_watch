package com.github.raink1208.deploytool.storage

/**
 * ストレージアップロードの抽象化インターフェース
 */
interface StorageUploader {
    /**
     * ファイルをアップロード
     */
    suspend fun upload(localPath: String, remotePath: String): UploadResult

    /**
     * 設定の妥当性を検証
     */
    fun validate(): Boolean
}

/**
 * アップロード結果
 */
sealed class UploadResult {
    data class Success(val remotePath: String, val size: Long) : UploadResult()
    data class Failure(val error: String, val exception: Exception? = null) : UploadResult()
}
