package com.github.raink1208.watchtool.service

import com.github.raink1208.watchtool.models.Statistics
import com.github.raink1208.watchtool.models.Video
import org.slf4j.LoggerFactory

class StatisticsCalculator {
    private val logger = LoggerFactory.getLogger(StatisticsCalculator::class.java)

    fun calculate(videos: List<Video>): Statistics {
        if (videos.isEmpty()) {
            logger.warn("No videos to calculate statistics")
            return Statistics()
        }

        // 遅刻判定が可能な配信のみを対象
        val analyzableVideos = videos.filter {
            it.scheduledStartTime != null && it.actualStartTime != null
        }

        if (analyzableVideos.isEmpty()) {
            logger.warn("No videos with both scheduled and actual start times")
            return Statistics(totalStreams = videos.size)
        }

        val delayedVideos = analyzableVideos.filter { it.delaySeconds > 0 }
        val delays = delayedVideos.map { it.delaySeconds }

        // 配信時間の統計（終了時刻がある配信のみ）
        val videosWithDuration = analyzableVideos.filter { it.streamDurationSeconds > 0 }
        val durations = videosWithDuration.map { it.streamDurationSeconds }

        val statistics = Statistics(
            totalStreams = analyzableVideos.size,
            delayedStreams = delayedVideos.size,
            delayRate = if (analyzableVideos.isNotEmpty())
                (delayedVideos.size.toDouble() / analyzableVideos.size * 100)
                else 0.0,
            averageDelaySeconds = if (delayedVideos.isNotEmpty())
                delays.average()
                else 0.0,
            maxDelaySeconds = delays.maxOrNull() ?: 0,
            minDelaySeconds = delays.minOrNull() ?: 0,
            medianDelaySeconds = calculateMedian(delays),
            totalStreamDurationSeconds = durations.sum(),
            averageStreamDurationSeconds = if (durations.isNotEmpty()) durations.average() else 0.0,
            maxStreamDurationSeconds = durations.maxOrNull() ?: 0,
            minStreamDurationSeconds = if (durations.isNotEmpty()) durations.minOrNull() ?: 0 else 0,
            totalDelaySeconds = delays.sum()
        )

        logger.info("Statistics calculated: ${statistics.delayedStreams}/${statistics.totalStreams} delayed (${String.format("%.2f", statistics.delayRate)}%)")
        logger.info("Total stream duration: ${statistics.totalStreamDurationSeconds} seconds, Average: ${String.format("%.2f", statistics.averageStreamDurationSeconds)} seconds")
        logger.info("Total delay: ${statistics.totalDelaySeconds} seconds")

        return statistics
    }

    private fun calculateMedian(values: List<Long>): Double {
        if (values.isEmpty()) return 0.0

        val sorted = values.sorted()
        val size = sorted.size

        return if (size % 2 == 0) {
            (sorted[size / 2 - 1] + sorted[size / 2]) / 2.0
        } else {
            sorted[size / 2].toDouble()
        }
    }
}
