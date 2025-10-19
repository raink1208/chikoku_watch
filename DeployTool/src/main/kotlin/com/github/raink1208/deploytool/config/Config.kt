package com.github.raink1208.deploytool.config

import java.io.File
import java.util.Properties

/**
 * アプリケーション設定を管理するクラス
 */
data class Config(
    val stateFile: String,
    val files: List<FileConfig>,
    val storage: StorageConfig,
    val logging: LoggingConfig,
    val retry: RetryConfig
) {
    companion object {
        /**
         * 設定ファイルを読み込む
         */
        fun load(path: String): Config {
            val props = Properties()
            File(path).inputStream().use { props.load(it) }

            // 環境変数の置換
            val resolvedProps = props.mapValues { (_, value) ->
                resolveEnvVars(value.toString())
            }

            val stateFile = resolvedProps["state.file"] ?: ".deploy-state.json"
            val filesCount = resolvedProps["files.count"]?.toIntOrNull() ?: 0

            val files = (0 until filesCount).map { i ->
                FileConfig(
                    localPath = resolvedProps["files.$i.local_path"]
                        ?: throw IllegalArgumentException("files.$i.local_path is required"),
                    remotePath = resolvedProps["files.$i.remote_path"]
                        ?: throw IllegalArgumentException("files.$i.remote_path is required"),
                    enabled = resolvedProps["files.$i.enabled"]?.toBoolean() ?: true
                )
            }

            val provider = resolvedProps["storage.provider"] ?: "cloudflare-r2"
            val storage = when (provider) {
                "cloudflare-r2" -> {
                    StorageConfig.CloudflareR2(
                        accountId = resolvedProps["storage.cloudflare_r2.account_id"]
                            ?: throw IllegalArgumentException("storage.cloudflare_r2.account_id is required"),
                        accessKeyId = resolvedProps["storage.cloudflare_r2.access_key_id"]
                            ?: throw IllegalArgumentException("storage.cloudflare_r2.access_key_id is required"),
                        secretAccessKey = resolvedProps["storage.cloudflare_r2.secret_access_key"]
                            ?: throw IllegalArgumentException("storage.cloudflare_r2.secret_access_key is required"),
                        bucketName = resolvedProps["storage.cloudflare_r2.bucket_name"]
                            ?: throw IllegalArgumentException("storage.cloudflare_r2.bucket_name is required"),
                        endpoint = resolvedProps["storage.cloudflare_r2.endpoint"]
                            ?: throw IllegalArgumentException("storage.cloudflare_r2.endpoint is required")
                    )
                }
                else -> throw IllegalArgumentException("Unsupported storage provider: $provider")
            }

            val logging = LoggingConfig(
                level = resolvedProps["logging.level"] ?: "INFO",
                file = resolvedProps["logging.file"] ?: "logs/deploy-tool.log",
                console = resolvedProps["logging.console"]?.toBoolean() ?: true
            )

            val retry = RetryConfig(
                maxAttempts = resolvedProps["retry.max_attempts"]?.toIntOrNull() ?: 3,
                initialDelayMs = resolvedProps["retry.initial_delay_ms"]?.toLongOrNull() ?: 1000L,
                maxDelayMs = resolvedProps["retry.max_delay_ms"]?.toLongOrNull() ?: 10000L,
                multiplier = resolvedProps["retry.multiplier"]?.toDoubleOrNull() ?: 2.0
            )

            return Config(stateFile, files, storage, logging, retry)
        }

        /**
         * 環境変数を置換する（${VAR_NAME}形式）
         */
        private fun resolveEnvVars(value: String): String {
            val pattern = """\$\{([^}]+)}""".toRegex()
            return pattern.replace(value) { matchResult ->
                val envVar = matchResult.groupValues[1]
                System.getenv(envVar) ?: matchResult.value
            }
        }
    }

    /**
     * 設定の妥当性を検証
     */
    fun validate(): Boolean {
        if (files.isEmpty()) {
            throw IllegalStateException("No files configured")
        }
        return true
    }
}

/**
 * ファイル設定
 */
data class FileConfig(
    val localPath: String,
    val remotePath: String,
    val enabled: Boolean = true
)

/**
 * ストレージ設定
 */
sealed class StorageConfig {
    data class CloudflareR2(
        val accountId: String,
        val accessKeyId: String,
        val secretAccessKey: String,
        val bucketName: String,
        val endpoint: String
    ) : StorageConfig()
}

/**
 * ログ設定
 */
data class LoggingConfig(
    val level: String,
    val file: String,
    val console: Boolean
)

/**
 * リトライ設定
 */
data class RetryConfig(
    val maxAttempts: Int,
    val initialDelayMs: Long,
    val maxDelayMs: Long,
    val multiplier: Double
)

