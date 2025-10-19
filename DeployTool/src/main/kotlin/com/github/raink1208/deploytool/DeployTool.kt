package com.github.raink1208.deploytool

import com.github.raink1208.deploytool.config.Config
import com.github.raink1208.deploytool.config.StorageConfig
import com.github.raink1208.deploytool.state.StateManager
import com.github.raink1208.deploytool.storage.R2Uploader
import com.github.raink1208.deploytool.storage.RetryableUploader
import com.github.raink1208.deploytool.storage.StorageUploader
import com.github.raink1208.deploytool.storage.UploadResult
import com.github.raink1208.deploytool.watcher.ChangeStatus
import com.github.raink1208.deploytool.watcher.FileWatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * デプロイツールのメインクラス
 */
class DeployTool(private val config: Config) {
    private val logger = LoggerFactory.getLogger(DeployTool::class.java)
    private val stateManager = StateManager(config.stateFile)
    private val fileWatcher = FileWatcher(stateManager)
    private val uploader: StorageUploader

    init {
        // ストレージアップローダーの初期化
        val baseUploader = when (config.storage) {
            is StorageConfig.CloudflareR2 -> R2Uploader(config.storage)
        }

        // リトライ機能を追加
        uploader = RetryableUploader(
            uploader = baseUploader,
            maxAttempts = config.retry.maxAttempts,
            initialDelayMs = config.retry.initialDelayMs,
            maxDelayMs = config.retry.maxDelayMs,
            multiplier = config.retry.multiplier
        )

        // 設定の妥当性チェック
        uploader.validate()
    }

    /**
     * デプロイツールを実行
     */
    fun run(dryRun: Boolean = false, forceUpload: Boolean = false, specificFile: String? = null): ExecutionResult {
        logger.info("DeployTool started")

        try {
            // 設定の検証
            config.validate()

            // 状態ファイルの読み込み
            var currentState = stateManager.loadState()
            logger.info("Loaded state from: ${config.stateFile}")

            // 対象ファイルのフィルタリング
            val targetFiles = if (specificFile != null) {
                config.files.filter { it.localPath == specificFile }
            } else {
                config.files
            }

            if (targetFiles.isEmpty()) {
                logger.warn("No files to process")
                return ExecutionResult(0, 0, 0)
            }

            // ファイルの変更チェック
            val changedFiles = fileWatcher.checkChanges(targetFiles, currentState, forceUpload)

            // アップロード対象のファイルを抽出
            val filesToUpload = changedFiles.filter { it.status == ChangeStatus.CHANGED }

            if (filesToUpload.isEmpty()) {
                logger.info("No files to upload")
                return ExecutionResult(0, changedFiles.count { it.status == ChangeStatus.UNCHANGED }, 0)
            }

            if (dryRun) {
                logger.info("DRY RUN: Would upload ${filesToUpload.size} file(s)")
                filesToUpload.forEach {
                    logger.info("  - ${it.config.localPath} -> ${it.config.remotePath}")
                }
                return ExecutionResult(0, 0, 0)
            }

            // ファイルのアップロード（並列実行）
            val results = runBlocking {
                filesToUpload.map { changedFile ->
                    async {
                        val result = uploader.upload(changedFile.config.localPath, changedFile.config.remotePath)
                        Pair(changedFile, result)
                    }
                }.awaitAll()
            }

            // 結果の集計と状態の更新
            var uploadedCount = 0
            var failedCount = 0

            results.forEach { (changedFile, result) ->
                when (result) {
                    is UploadResult.Success -> {
                        uploadedCount++
                        currentState = stateManager.updateFileState(
                            state = currentState,
                            filePath = changedFile.config.localPath,
                            hash = changedFile.hash ?: "",
                            size = result.size,
                            status = "success"
                        )
                    }
                    is UploadResult.Failure -> {
                        failedCount++
                        logger.error("Upload failed: ${changedFile.config.localPath} - ${result.error}")
                    }
                }
            }

            // 状態ファイルの保存
            stateManager.saveState(currentState)

            val skippedCount = changedFiles.count { it.status == ChangeStatus.UNCHANGED }
            logger.info("DeployTool completed: $uploadedCount uploaded, $skippedCount skipped, $failedCount failed")

            return ExecutionResult(uploadedCount, skippedCount, failedCount)

        } catch (e: Exception) {
            logger.error("DeployTool failed with error", e)
            throw e
        }
    }
}

/**
 * 実行結果
 */
data class ExecutionResult(
    val uploaded: Int,
    val skipped: Int,
    val failed: Int
) {
    fun isSuccess(): Boolean = failed == 0
}
