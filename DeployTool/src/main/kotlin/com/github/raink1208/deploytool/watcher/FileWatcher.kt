package com.github.raink1208.deploytool.watcher

import com.github.raink1208.deploytool.config.FileConfig
import com.github.raink1208.deploytool.state.FileState
import com.github.raink1208.deploytool.state.StateManager
import org.slf4j.LoggerFactory
import java.io.File

/**
 * ファイル監視と変更検知を行うクラス
 */
class FileWatcher(private val stateManager: StateManager) {
    private val logger = LoggerFactory.getLogger(FileWatcher::class.java)

    /**
     * 変更されたファイルをチェック
     */
    fun checkChanges(files: List<FileConfig>, currentState: FileState, forceUpload: Boolean = false): List<ChangedFile> {
        val changedFiles = mutableListOf<ChangedFile>()

        for (fileConfig in files) {
            if (!fileConfig.enabled) {
                logger.debug("File disabled, skipping: ${fileConfig.localPath}")
                continue
            }

            val file = File(fileConfig.localPath)
            if (!file.exists()) {
                logger.warn("File not found: ${fileConfig.localPath}")
                changedFiles.add(ChangedFile(fileConfig, null, ChangeStatus.NOT_FOUND))
                continue
            }

            try {
                logger.info("Checking file: ${fileConfig.localPath}")
                val currentHash = stateManager.calculateHash(fileConfig.localPath)
                logger.debug("Current hash: $currentHash")

                val hasChanged = forceUpload || stateManager.hasChanged(fileConfig.localPath, currentHash, currentState)

                if (hasChanged) {
                    logger.info("Changes detected, will upload: ${fileConfig.localPath}")
                    changedFiles.add(ChangedFile(fileConfig, currentHash, ChangeStatus.CHANGED))
                } else {
                    logger.info("No changes detected, skipping: ${fileConfig.localPath}")
                    changedFiles.add(ChangedFile(fileConfig, currentHash, ChangeStatus.UNCHANGED))
                }
            } catch (e: Exception) {
                logger.error("Failed to calculate hash for file: ${fileConfig.localPath}", e)
                changedFiles.add(ChangedFile(fileConfig, null, ChangeStatus.ERROR))
            }
        }

        return changedFiles
    }
}

/**
 * 変更されたファイルの情報
 */
data class ChangedFile(
    val config: FileConfig,
    val hash: String?,
    val status: ChangeStatus
)

/**
 * 変更ステータス
 */
enum class ChangeStatus {
    CHANGED,      // 変更あり
    UNCHANGED,    // 変更なし
    NOT_FOUND,    // ファイルが見つからない
    ERROR         // エラー
}

