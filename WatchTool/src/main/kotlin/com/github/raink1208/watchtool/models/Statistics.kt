package com.github.raink1208.watchtool.models

import kotlinx.serialization.Serializable

@Serializable
data class Statistics(
    val totalStreams: Int = 0,
    val delayedStreams: Int = 0,
    val delayRate: Double = 0.0,
    val averageDelaySeconds: Double = 0.0,  // 平均遅刻時間（秒）
    val maxDelaySeconds: Long = 0,  // 最大遅刻時間（秒）
    val minDelaySeconds: Long = 0,  // 最小遅刻時間（秒）
    val medianDelaySeconds: Double = 0.0,  // 中央値（秒）
    val onTimeStreams: Int = 0,
    val minorDelayStreams: Int = 0,      // 1-300秒（5分）
    val normalDelayStreams: Int = 0,      // 301-900秒（5-15分）
    val majorDelayStreams: Int = 0,       // 901-1800秒（15-30分）
    val severeDelayStreams: Int = 0,      // 1801秒以上（30分超）
    val totalStreamDurationSeconds: Long = 0,  // 総配信時間（秒）
    val averageStreamDurationSeconds: Double = 0.0,  // 平均配信時間（秒）
    val maxStreamDurationSeconds: Long = 0,  // 最長配信時間（秒）
    val minStreamDurationSeconds: Long = 0,  // 最短配信時間（秒）
    val totalDelaySeconds: Long = 0  // 総遅刻時間（秒）
)

@Serializable
data class StreamReport(
    val channelInfo: Channel,
    val statistics: Statistics,
    val streams: List<Video>
)
