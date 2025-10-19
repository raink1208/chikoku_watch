package com.github.raink1208.watchtool.utils

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimeUtil {
    private val formatter = DateTimeFormatter.ISO_INSTANT

    fun parseInstant(dateTimeString: String?): Instant? {
        return try {
            dateTimeString?.let { Instant.parse(it) }
        } catch (e: Exception) {
            null
        }
    }

    fun calculateDelaySeconds(scheduledStartTime: String?, actualStartTime: String?): Long {
        val scheduled = parseInstant(scheduledStartTime) ?: return 0
        val actual = parseInstant(actualStartTime) ?: return 0

        val duration = Duration.between(scheduled, actual)
        return duration.seconds.coerceAtLeast(0)
    }

    fun calculateStreamDurationSeconds(actualStartTime: String?, actualEndTime: String?): Long {
        val start = parseInstant(actualStartTime) ?: return 0
        val end = parseInstant(actualEndTime) ?: return 0

        val duration = Duration.between(start, end)
        return duration.seconds.coerceAtLeast(0)
    }

    fun formatDateTime(instant: Instant): String {
        return formatter.format(instant)
    }

    fun toLocalDateTime(dateTimeString: String): String {
        return try {
            val instant = Instant.parse(dateTimeString)
            val localDateTime = instant.atZone(ZoneId.systemDefault())
            localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } catch (e: Exception) {
            dateTimeString
        }
    }

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hours > 0 -> "${hours}時間${minutes}分${secs}秒"
            minutes > 0 -> "${minutes}分${secs}秒"
            else -> "${secs}秒"
        }
    }
}