package com.github.raink1208.deploytool.storage

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.net.url.Url
import com.github.raink1208.deploytool.config.StorageConfig
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Cloudflare R2アップローダー（S3互換API使用）
 */
class R2Uploader(private val config: StorageConfig.CloudflareR2) : StorageUploader {
    private val logger = LoggerFactory.getLogger(R2Uploader::class.java)

    override suspend fun upload(localPath: String, remotePath: String): UploadResult {
        val file = File(localPath)
        if (!file.exists()) {
            return UploadResult.Failure("File not found: $localPath")
        }

        return try {
            S3Client {
                region = "auto"
                endpointUrl = Url.parse(config.endpoint)
                credentialsProvider = StaticCredentialsProvider {
                    accessKeyId = config.accessKeyId
                    secretAccessKey = config.secretAccessKey
                }
            }.use { s3 ->
                val request = PutObjectRequest {
                    bucket = config.bucketName
                    key = remotePath
                    body = ByteStream.fromBytes(file.readBytes())
                }

                logger.debug("Uploading file: $localPath -> $remotePath (size: ${file.length()} bytes)")
                s3.putObject(request)

                logger.info("Successfully uploaded: $remotePath")
                UploadResult.Success(remotePath, file.length())
            }
        } catch (e: Exception) {
            logger.error("Failed to upload file: $localPath", e)
            UploadResult.Failure("Upload failed: ${e.message}", e)
        }
    }

    override fun validate(): Boolean {
        if (config.accountId.isBlank()) {
            throw IllegalStateException("Account ID is required")
        }
        if (config.accessKeyId.isBlank()) {
            throw IllegalStateException("Access Key ID is required")
        }
        if (config.secretAccessKey.isBlank()) {
            throw IllegalStateException("Secret Access Key is required")
        }
        if (config.bucketName.isBlank()) {
            throw IllegalStateException("Bucket name is required")
        }
        if (config.endpoint.isBlank()) {
            throw IllegalStateException("Endpoint is required")
        }
        return true
    }
}

/**
 * リトライ機能付きアップローダーのラッパー
 */
class RetryableUploader(
    private val uploader: StorageUploader,
    private val maxAttempts: Int,
    private val initialDelayMs: Long,
    private val maxDelayMs: Long,
    private val multiplier: Double
) : StorageUploader {
    private val logger = LoggerFactory.getLogger(RetryableUploader::class.java)

    override suspend fun upload(localPath: String, remotePath: String): UploadResult {
        var attempt = 0
        var delayMs = initialDelayMs

        while (attempt < maxAttempts) {
            attempt++
            logger.debug("Upload attempt $attempt/$maxAttempts for $localPath")

            val result = uploader.upload(localPath, remotePath)

            when (result) {
                is UploadResult.Success -> return result
                is UploadResult.Failure -> {
                    if (attempt >= maxAttempts) {
                        logger.error("Upload failed after $maxAttempts attempts: $localPath")
                        return result
                    }

                    logger.warn("Upload attempt $attempt failed, retrying in ${delayMs}ms: ${result.error}")
                    delay(delayMs)

                    delayMs = minOf((delayMs * multiplier).toLong(), maxDelayMs)
                }
            }
        }

        return UploadResult.Failure("Max retry attempts reached")
    }

    override fun validate(): Boolean = uploader.validate()
}
