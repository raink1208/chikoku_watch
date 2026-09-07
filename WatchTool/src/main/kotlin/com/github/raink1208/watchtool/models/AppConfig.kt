package com.github.raink1208.watchtool.models

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    /** デビュー日（YYYY-MM-DD）。年度分割の基準日。未設定なら分割しない */
    val debutDate: String? = null,
    val external: List<String> = emptyList(),
    val sessions: List<SessionConfig> = emptyList(),
    val overrides: List<OverrideConfig> = emptyList()
)

@Serializable
data class SessionConfig(
    val id: String,
    val name: String,
    val videos: List<String> = emptyList()
)

@Serializable
data class OverrideConfig(
    val videoId: String,
    val title: String? = null,
    val publishedAt: String? = null,
    val scheduledStartTime: String? = null,
    val actualStartTime: String? = null,
    val actualEndTime: String? = null,
    val viewCount: Long? = null,
    val likeCount: Long? = null,
    val commentCount: Long? = null,
    val delaySeconds: Long? = null,
    val streamDurationSeconds: Long? = null,
    val thumbnail: String? = null
)

