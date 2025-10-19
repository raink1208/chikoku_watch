package com.github.raink1208.watchtool.utils

import java.io.File
import java.util.Properties

object ConfigLoader {
    private val properties = Properties()

    init {
        loadConfig()
    }

    private fun loadConfig() {
        val configFile = File("config/config.properties")
        if (configFile.exists()) {
            configFile.inputStream().use { properties.load(it) }
        }
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

