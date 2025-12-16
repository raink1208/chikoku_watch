package com.github.raink1208.watchtool

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import com.github.raink1208.watchtool.api.ApiConfig
import com.github.raink1208.watchtool.api.YouTubeApiClient
import com.github.raink1208.watchtool.export.CsvExporter
import com.github.raink1208.watchtool.export.JsonExporter
import com.github.raink1208.watchtool.export.TextReportExporter
import com.github.raink1208.watchtool.models.StreamReport
import com.github.raink1208.watchtool.service.DelayAnalyzer
import com.github.raink1208.watchtool.service.StatisticsCalculator
import com.github.raink1208.watchtool.service.VideoFetcher
import com.github.raink1208.watchtool.utils.DateTimeUtil
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

class WatchToolCommand : CliktCommand(
    name = "watchtool",
    help = "YouTube配信遅刻分析ツール"
) {
    private val logger = LoggerFactory.getLogger(WatchToolCommand::class.java)

    private val channelId by option("-c", "--channel", help = "対象チャンネルID（必須）")
        .required()

    private val outputPath by option("-o", "--output", help = "出力ファイルパス")
        .default("output/result.json")

    private val format by option("-f", "--format", help = "出力形式（json/csv/text）")
        .choice("json", "csv", "text")
        .default("json")

    private val limit by option("-l", "--limit", help = "取得する配信の最大数")
        .int()
        .default(Int.MAX_VALUE)

    private val startDate by option("-s", "--start-date", help = "取得開始日（YYYY-MM-DD形式）")

    private val endDate by option("-e", "--end-date", help = "取得終了日（YYYY-MM-DD形式）")

    private val eventType by option("--event-type", help = "配信タイプ（completed/live/upcoming）")
        .choice("completed", "live", "upcoming")
        .default("completed")

    private val verbose by option("-v", "--verbose", help = "詳細ログ出力")
        .flag(default = false)

    override fun run() = runBlocking {
        try {
            logger.info("YouTube配信分析ツールを開始します")
            logger.info("チャンネルID: $channelId")
            logger.info("出力形式: $format")
            logger.info("出力先: $outputPath")

            // APIクライアントの初期化
            val config = ApiConfig()
            val apiClient = YouTubeApiClient(config)

            // チャンネル情報の取得
            echo("チャンネル情報を取得中...")
            val channelInfo = apiClient.getChannelInfo(channelId)
            if (channelInfo == null) {
                echo("エラー: チャンネル情報を取得できませんでした", err = true)
                exitProcess(1)
            }
            echo("チャンネル名: ${channelInfo.name}")

            // 動画の取得
            echo("配信情報を取得中...")
            val fetcher = VideoFetcher(apiClient)
            val videos = fetcher.fetchAllVideos(
                channelId = channelId,
                maxVideos = limit,
                startDate = startDate,
                endDate = endDate
            )

            if (videos.isEmpty()) {
                echo("警告: 配信が見つかりませんでした", err = true)
                exitProcess(0)
            }

            echo("${videos.size}件の配信を取得しました")

            // 統計計算
            echo("統計を計算中...")
            val calculator = StatisticsCalculator()
            val statistics = calculator.calculate(videos)

            // 詳細分析
            val analyzer = DelayAnalyzer()
            val analysis = analyzer.analyze(videos)

            // レポート作成
            val report = StreamReport(
                channelInfo = channelInfo,
                statistics = statistics,
                streams = videos
            )

            // 出力
            echo("結果を出力中...")
            when (format) {
                "json" -> {
                    val exporter = JsonExporter()
                    exporter.export(report, outputPath)
                }
                "csv" -> {
                    val exporter = CsvExporter()
                    exporter.export(report, outputPath)
                }
                "text" -> {
                    val exporter = TextReportExporter()
                    exporter.export(report, outputPath, analysis)
                }
            }

            echo("完了しました: $outputPath")
            echo()
            echo("=== サマリー ===")
            echo("総配信数: ${statistics.totalStreams}回")
            echo("遅刻配信数: ${statistics.delayedStreams}回")
            echo("遅刻率: ${String.format("%.2f", statistics.delayRate)}%")
            if (statistics.delayedStreams > 0) {
                echo("総遅刻時間: ${DateTimeUtil.formatDuration(statistics.totalDelaySeconds)}")
                echo("平均遅刻時間: ${DateTimeUtil.formatDuration(statistics.averageDelaySeconds.toLong())}")
            }
            if (statistics.totalStreamDurationSeconds > 0) {
                echo("総配信時間: ${DateTimeUtil.formatDuration(statistics.totalStreamDurationSeconds)}")
                echo("平均配信時間: ${DateTimeUtil.formatDuration(statistics.averageStreamDurationSeconds.toLong())}")
            }
            echo("使用APIクォータ: ${apiClient.getUsedQuota()}ユニット")

        } catch (e: Exception) {
            logger.error("エラーが発生しました", e)
            echo("エラー: ${e.message}", err = true)
            exitProcess(1)
        }
    }
}

fun main(args: Array<String>) {
    WatchToolCommand().main(args)
}