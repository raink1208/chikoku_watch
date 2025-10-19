package com.github.raink1208.watchtool.export

import com.github.raink1208.watchtool.models.StreamReport
import com.github.raink1208.watchtool.utils.DateTimeUtil
import java.io.File

class CsvExporter {
    fun export(report: StreamReport, outputPath: String) {
        val csv = buildString {
            // ヘッダー
            appendLine("動画ID,タイトル,予定開始時刻,実際の開始時刻,遅刻時間(秒),配信時間(秒),視聴回数,高評価数,コメント数")

            // データ行
            report.streams.forEach { video ->
                val scheduledTime = video.scheduledStartTime?.let { DateTimeUtil.toLocalDateTime(it) } ?: ""
                val actualTime = video.actualStartTime?.let { DateTimeUtil.toLocalDateTime(it) } ?: ""

                appendLine(
                    listOf(
                        video.videoId,
                        escapeCsv(video.title),
                        scheduledTime,
                        actualTime,
                        video.delaySeconds,
                        video.streamDurationSeconds,
                        video.viewCount,
                        video.likeCount,
                        video.commentCount
                    ).joinToString(",")
                )
            }
        }

        File(outputPath).apply {
            parentFile?.mkdirs()
            writeText(csv)
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
