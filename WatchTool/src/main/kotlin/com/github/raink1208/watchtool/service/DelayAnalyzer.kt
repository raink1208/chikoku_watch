package com.github.raink1208.watchtool.service

import com.github.raink1208.watchtool.models.Video
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId

class DelayAnalyzer {
    private val logger = LoggerFactory.getLogger(DelayAnalyzer::class.java)

    data class DelayAnalysis(
        val byMonth: Map<String, Int>,
        val byDayOfWeek: Map<String, Int>,
        val byHourOfDay: Map<Int, Int>,
        val topDelayedVideos: List<Video>
    )

    fun analyze(videos: List<Video>): DelayAnalysis {
        val delayedVideos = videos.filter {
            it.scheduledStartTime != null &&
            it.actualStartTime != null &&
            it.delaySeconds > 0
        }

        return DelayAnalysis(
            byMonth = analyzeByMonth(delayedVideos),
            byDayOfWeek = analyzeByDayOfWeek(delayedVideos),
            byHourOfDay = analyzeByHourOfDay(delayedVideos),
            topDelayedVideos = delayedVideos.sortedByDescending { it.delaySeconds }.take(10)
        )
    }

    private fun analyzeByMonth(videos: List<Video>): Map<String, Int> {
        return videos
            .mapNotNull { video ->
                video.scheduledStartTime?.let {
                    try {
                        val instant = Instant.parse(it)
                        val date = instant.atZone(ZoneId.systemDefault())
                        "${date.year}-${String.format("%02d", date.monthValue)}"
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            .groupingBy { it }
            .eachCount()
            .toSortedMap()
    }

    private fun analyzeByDayOfWeek(videos: List<Video>): Map<String, Int> {
        val dayNames = mapOf(
            1 to "月曜日",
            2 to "火曜日",
            3 to "水曜日",
            4 to "木曜日",
            5 to "金曜日",
            6 to "土曜日",
            7 to "日曜日"
        )

        return videos
            .mapNotNull { video ->
                video.scheduledStartTime?.let {
                    try {
                        val instant = Instant.parse(it)
                        val date = instant.atZone(ZoneId.systemDefault())
                        dayNames[date.dayOfWeek.value]
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            .groupingBy { it }
            .eachCount()
    }

    private fun analyzeByHourOfDay(videos: List<Video>): Map<Int, Int> {
        return videos
            .mapNotNull { video ->
                video.scheduledStartTime?.let {
                    try {
                        val instant = Instant.parse(it)
                        val date = instant.atZone(ZoneId.systemDefault())
                        date.hour
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            .groupingBy { it }
            .eachCount()
            .toSortedMap()
    }
}