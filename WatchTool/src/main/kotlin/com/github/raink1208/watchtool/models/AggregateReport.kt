package com.github.raink1208.watchtool.models

import kotlinx.serialization.Serializable

/** セッション内の各動画を指す参照 */
@Serializable
data class VideoRef(
    val videoId: String,
    val title: String,
    val channelId: String
)

/** 1配信イベント分の集計データ */
@Serializable
data class AggregateStream(
    val id: String,
    val title: String,
    val durationSeconds: Long,
    val delaySeconds: Long,
    val viewCount: Long,
    val likeCount: Long,
    val commentCount: Long,
    val scheduledStartTime: String,
    val actualStartTime: String? = null,
    val actualEndTime: String? = null,
    /** config.json の sessions に明示的に紐付けられているか */
    val linked: Boolean,
    val videos: List<VideoRef>
)

@Serializable
data class AggregateReport(
    val streams: List<AggregateStream>
)
