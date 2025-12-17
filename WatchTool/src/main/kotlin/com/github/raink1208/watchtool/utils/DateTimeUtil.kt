package com.github.raink1208.watchtool.utils

import com.github.raink1208.watchtool.models.Video
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateTimeUtil {
    private val jstZoneId = ZoneId.of("Asia/Tokyo")
    private val jstFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(jstZoneId)

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
        return jstFormatter.format(instant)
    }

    fun toLocalDateTime(dateTimeString: String): String {
        return try {
            val instant = Instant.parse(dateTimeString)
            val localDateTime = instant.atZone(jstZoneId)
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

    /**
     * デビュー日から何年目かでビデオを分類する
     * @param videos 分類する動画のリスト
     * @param debutDateStr デビュー日の文字列（YYYY-MM-DD形式、例: 2024-01-13）
     * @return 年数ごとに分類されたビデオのリスト（1年目、2年目、...）
     */
    fun groupVideosByYearsSinceDebut(videos: List<Video>, debutDateStr: String): List<List<Video>> {
        // ISO Local Date形式のパーサー（YYYY-MM-DD）
        val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        val debutDate = try {
            // LocalDateとしてパースし、時刻を00:00:00として扱う
            val localDate = java.time.LocalDate.parse(debutDateStr, dateFormatter)
            localDate.atStartOfDay()
        } catch (_: Exception) {
            // パース失敗時は全て1つのリストとして返す
            println("警告: デビュー日のパースに失敗しました: $debutDateStr")
            return listOf(videos)
        }

        // scheduledStartTimeに基づいて年数を計算してグループ化
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val videosByYear = videos.groupBy { video ->
            if (video.scheduledStartTime == null) {
                -1  // scheduledStartTimeがnullの場合は-1として扱う
            } else {
                try {
                    // YYYY-MM-DD HH:mm:ss形式の日時文字列をLocalDateTimeとしてパース
                    val scheduledDateTime = LocalDateTime.parse(video.scheduledStartTime, dateTimeFormatter)
                    val scheduledDate = scheduledDateTime.toLocalDate()
                    val debutLocalDate = debutDate.toLocalDate()
                    val yearsSinceDebut = ChronoUnit.YEARS.between(debutLocalDate, scheduledDate).toInt()
                    yearsSinceDebut  // 0年目（1年目）、1年目（2年目）、...
                } catch (_: Exception) {
                    -1  // パース失敗時は-1として扱う
                }
            }
        }

        // 年数でソートして、リストのリストとして返す
        // -1（不明）は除外する
        return videosByYear
            .filterKeys { it >= 0 }
            .toSortedMap()
            .values
            .toList()
    }
}