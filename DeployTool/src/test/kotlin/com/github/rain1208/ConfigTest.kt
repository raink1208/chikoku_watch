package com.github.rain1208

import com.github.raink1208.deploytool.config.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import java.io.File

class ConfigTest {
    @Test
    fun testConfigLoad() {
        // テスト用の設定ファイルを作成
        val testConfig = """
            state.file=.test-state.json
            files.count=1
            files.0.local_path=test.txt
            files.0.remote_path=uploads/test.txt
            files.0.enabled=true
            storage.provider=cloudflare-r2
            storage.cloudflare_r2.account_id=test-account
            storage.cloudflare_r2.access_key_id=test-key
            storage.cloudflare_r2.secret_access_key=test-secret
            storage.cloudflare_r2.bucket_name=test-bucket
            storage.cloudflare_r2.endpoint=https://test.r2.cloudflarestorage.com
            logging.level=INFO
            logging.file=logs/test.log
            logging.console=true
            retry.max_attempts=3
            retry.initial_delay_ms=1000
            retry.max_delay_ms=10000
            retry.multiplier=2.0
        """.trimIndent()

        val testFile = File("test-config.properties")
        testFile.writeText(testConfig)

        try {
            val config = Config.load("test-config.properties")

            assertNotNull(config)
            assertEquals(".test-state.json", config.stateFile)
            assertEquals(1, config.files.size)
            assertEquals("test.txt", config.files[0].localPath)
            assertEquals(3, config.retry.maxAttempts)
        } finally {
            testFile.delete()
        }
    }
}

