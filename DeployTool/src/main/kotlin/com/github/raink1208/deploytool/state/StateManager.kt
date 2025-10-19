package com.github.raink1208.deploytool.state

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory
import java.io.File
import java.security.MessageDigest
import java.time.Instant

/**
 * ファイル状態を管理するクラス
 */
class StateManager(private val stateFilePath: String) {
    private val logger = LoggerFactory.getLogger(StateManager::class.java)
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * 状態ファイルを読み込む
     */
    fun loadState(): FileState {
        val file = File(stateFilePath)
        if (!file.exists()) {
            logger.info("State file not found, creating new state")
            return FileState(version = "1.0", lastUpdated = Instant.now().toString(), files = emptyMap())
        }

        return try {
            json.decodeFromString<FileState>(file.readText())
        } catch (e: Exception) {
            logger.warn("Failed to load state file: ${e.message}, creating new state")
            FileState(version = "1.0", lastUpdated = Instant.now().toString(), files = emptyMap())
        }
    }

    /**
     * 状態ファイルを保存
     */
    fun saveState(state: FileState) {
        val file = File(stateFilePath)
        file.parentFile?.mkdirs()

        val updatedState = state.copy(lastUpdated = Instant.now().toString())
        file.writeText(json.encodeToString(updatedState))
        logger.debug("State file saved: $stateFilePath")
    }

    /**
     * ファイルのハッシュ値を計算（SHA-256）
     */
    fun calculateHash(filePath: String): String {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File not found: $filePath")
        }

        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        val hashBytes = digest.digest()
        return "sha256:" + hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * ファイルが変更されたかチェック
     */
    fun hasChanged(filePath: String, currentHash: String, state: FileState): Boolean {
        val fileInfo = state.files[filePath]
        if (fileInfo == null) {
            logger.debug("File not in state, considering as changed: $filePath")
            return true
        }

        val changed = fileInfo.hash != currentHash
        logger.debug("File $filePath: previous hash=${fileInfo.hash}, current hash=$currentHash, changed=$changed")
        return changed
    }

    /**
     * 状態を更新
     */
    fun updateFileState(
        state: FileState,
        filePath: String,
        hash: String,
        size: Long,
        status: String
    ): FileState {
        val fileInfo = FileInfo(
            hash = hash,
            lastUpload = Instant.now().toString(),
            size = size,
            status = status
        )

        val newFiles = state.files.toMutableMap()
        newFiles[filePath] = fileInfo

        return state.copy(files = newFiles)
    }
}

/**
 * 状態ファイルのデータ構造
 */
@Serializable
data class FileState(
    val version: String,
    val lastUpdated: String,
    val files: Map<String, FileInfo>
)

/**
 * ファイル情報
 */
@Serializable
data class FileInfo(
    val hash: String,
    val lastUpload: String,
    val size: Long,
    val status: String
)

