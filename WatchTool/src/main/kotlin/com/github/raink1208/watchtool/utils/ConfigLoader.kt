package com.github.raink1208.watchtool.utils

import com.github.raink1208.watchtool.models.AppConfig
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Properties

object ConfigLoader {
    private val properties = Properties()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        loadConfig()
    }

    private fun loadConfig() {
        val configFile = File("config/config.properties")
        if (configFile.exists()) {
            configFile.inputStream().use { properties.load(it) }
        }
    }

    fun loadAppConfig(): AppConfig {
        // まずファイルシステムの config/config.json を探す
        val fsFile = File("config/config.json")
        if (fsFile.exists()) {
            return json.decodeFromString(fsFile.readText())
        }
        // フォールバック: クラスパスの config.json を探す
        val resource = ConfigLoader::class.java.classLoader.getResourceAsStream("config.json")
        if (resource != null) {
            return json.decodeFromString(resource.bufferedReader().readText())
        }
        return AppConfig()
    }

    fun getApiKey(): String {
        return System.getenv("YOUTUBE_API_KEY")
            ?: properties.getProperty("youtube.api.key")
            ?: throw IllegalStateException("YouTube API key not found. Set YOUTUBE_API_KEY environment variable or youtube.api.key in config/api_config.properties")
    }

    fun getQuotaLimit(): Int {
        return properties.getProperty("youtube.api.quota.limit")?.toIntOrNull() ?: 10000
    }

    fun getMaxResults(): Int {
        return properties.getProperty("fetch.max.results")?.toIntOrNull() ?: 50
    }

    fun getMaxVideos(): Int {
        return properties.getProperty("fetch.max.videos")?.toIntOrNull() ?: 1000
    }

    fun getDelayThresholdMinutes(): Int {
        return properties.getProperty("delay.threshold.minutes")?.toIntOrNull() ?: 1
    }
}

