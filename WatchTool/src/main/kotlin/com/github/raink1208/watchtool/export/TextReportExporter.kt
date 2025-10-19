package com.github.raink1208.watchtool.export

import com.github.raink1208.watchtool.models.StreamReport
import com.github.raink1208.watchtool.service.DelayAnalyzer
import com.github.raink1208.watchtool.utils.DateTimeUtil
import java.io.File

class TextReportExporter {
    fun export(report: StreamReport, outputPath: String, analysis: DelayAnalyzer.DelayAnalysis? = null) {
        val text = buildString {
            appendLine("=" .repeat(60))
            appendLine("YouTube配信遅刻分析レポート")
            appendLine("=" .repeat(60))
            appendLine()

            // チャンネル情報
            appendLine("【チャンネル情報】")
            appendLine("チャンネル名: ${report.channelInfo.name}")
            appendLine("チャンネルID: ${report.channelInfo.id}")
            appendLine("登録者数: ${formatNumber(report.channelInfo.subscriberCount)}人")
            appendLine("総動画数: ${formatNumber(report.channelInfo.videoCount)}本")
            appendLine()

            // 統計情報
            appendLine("【統計サマリー】")
            appendLine("総配信数: ${report.statistics.totalStreams}回")
            appendLine("遅刻配信数: ${report.statistics.delayedStreams}回")
            appendLine("遅刻率: ${String.format("%.2f", report.statistics.delayRate)}%")
            appendLine()

            // 配信時間統計
            if (report.statistics.totalStreamDurationSeconds > 0) {
                appendLine("【配信時間統計】")
                appendLine("総配信時間: ${DateTimeUtil.formatDuration(report.statistics.totalStreamDurationSeconds)}")
                appendLine("平均配信時間: ${DateTimeUtil.formatDuration(report.statistics.averageStreamDurationSeconds.toLong())}")
                appendLine("最長配信時間: ${DateTimeUtil.formatDuration(report.statistics.maxStreamDurationSeconds)}")
                appendLine("最短配信時間: ${DateTimeUtil.formatDuration(report.statistics.minStreamDurationSeconds)}")
                appendLine()
            }

            if (report.statistics.delayedStreams > 0) {
                appendLine("【遅刻時間統計】")
                appendLine("総遅刻時間: ${DateTimeUtil.formatDuration(report.statistics.totalDelaySeconds)}")
                appendLine("平均遅刻時間: ${DateTimeUtil.formatDuration(report.statistics.averageDelaySeconds.toLong())}")
                appendLine("最大遅刻時間: ${DateTimeUtil.formatDuration(report.statistics.maxDelaySeconds)}")
                appendLine("最小遅刻時間: ${DateTimeUtil.formatDuration(report.statistics.minDelaySeconds)}")
                appendLine("中央値: ${DateTimeUtil.formatDuration(report.statistics.medianDelaySeconds.toLong())}")
                appendLine()

                appendLine("【遅刻分類】")
                appendLine("定刻開始 (0秒): ${report.statistics.onTimeStreams}回")
                appendLine("軽微な遅刻 (1-300秒/5分以内): ${report.statistics.minorDelayStreams}回")
                appendLine("通常遅刻 (301-900秒/5-15分): ${report.statistics.normalDelayStreams}回")
                appendLine("大幅遅刻 (901-1800秒/15-30分): ${report.statistics.majorDelayStreams}回")
                appendLine("深刻な遅刻 (1801秒以上/30分超): ${report.statistics.severeDelayStreams}回")
                appendLine()
            }

            // 詳細分析
            if (analysis != null) {
                if (analysis.byMonth.isNotEmpty()) {
                    appendLine("【月別遅刻統計】")
                    analysis.byMonth.forEach { (month, count) ->
                        appendLine("  $month: ${count}回")
                    }
                    appendLine()
                }

                if (analysis.byDayOfWeek.isNotEmpty()) {
                    appendLine("【曜日別遅刻統計】")
                    analysis.byDayOfWeek.forEach { (day, count) ->
                        appendLine("  $day: ${count}回")
                    }
                    appendLine()
                }

                if (analysis.byHourOfDay.isNotEmpty()) {
                    appendLine("【時間帯別遅刻統計】")
                    analysis.byHourOfDay.forEach { (hour, count) ->
                        appendLine("  ${String.format("%02d", hour)}時台: ${count}回")
                    }
                    appendLine()
                }

                if (analysis.topDelayedVideos.isNotEmpty()) {
                    appendLine("【遅刻時間トップ10】")
                    analysis.topDelayedVideos.forEachIndexed { index, video ->
                        appendLine("${index + 1}. ${video.title}")
                        appendLine("   遅刻時間: ${DateTimeUtil.formatDuration(video.delaySeconds)} | 配信時間: ${DateTimeUtil.formatDuration(video.streamDurationSeconds)} | 動画ID: ${video.videoId}")
                    }
                    appendLine()
                }
            }

            appendLine("=" .repeat(60))
        }

        File(outputPath).apply {
            parentFile?.mkdirs()
            writeText(text)
        }
    }

    private fun formatNumber(number: Long): String {
        return String.format("%,d", number)
    }
}
