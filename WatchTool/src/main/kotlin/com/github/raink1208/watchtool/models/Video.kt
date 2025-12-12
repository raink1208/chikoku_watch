package com.github.raink1208.watchtool.models

import kotlinx.serialization.Serializable

@Serializable
data class Video(
    val videoId: String,
    val title: String,
    val publishedAt: String,
    val scheduledStartTime: String? = null,
    val actualStartTime: String? = null,
    val actualEndTime: String? = null,
    val concurrentViewers: Long? = null,
    val viewCount: Long = 0,
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val delaySeconds: Long = 0,  // 遅刻時間（秒）
    val streamDurationSeconds: Long = 0  // 配信時間（秒）
)
