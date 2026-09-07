package com.github.raink1208.watchtool

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import com.github.raink1208.watchtool.api.ApiConfig
import com.github.raink1208.watchtool.api.YouTubeApiClient
import com.github.raink1208.watchtool.export.AggregateExporter
import com.github.raink1208.watchtool.service.AggregateService
import com.github.raink1208.watchtool.utils.ConfigLoader
import com.github.raink1208.watchtool.utils.DateTimeUtil
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

class WatchToolCommand : CliktCommand(
    name = "watchtool",
    help = "YouTube配信集計ツール"
) {
    private val logger = LoggerFactory.getLogger(WatchToolCommand::class.java)

    private val channelId by option("-c", "--channel", help = "対象チャンネルID（必須）")
        .required()

    private val outputPath by option("-o", "--output", help = "result.json 出力先パス")
        .default("output/result.json")

    private val limit by option("-l", "--limit", help = "取得する配信の最大数")
        .int()
        .default(Int.MAX_VALUE)

    override fun run() = runBlocking {
        try {
            logger.info("YouTube配信集計ツールを開始します")
            logger.info("チャンネルID: $channelId")
            logger.info("出力先: $outputPath")

            // config.json 読み込み
            val appConfig = ConfigLoader.loadAppConfig()
            logger.info(
                "config.json 読み込み完了 - " +
                    "debutDate: ${appConfig.debutDate ?: "未設定"}, " +
                    "external: ${appConfig.external.size}件, " +
                    "sessions: ${appConfig.sessions.size}件, " +
                    "overrides: ${appConfig.overrides.size}件"
            )

            // APIクライアントの初期化
            val config = ApiConfig()
            val apiClient = YouTubeApiClient(config)

            // チャンネル情報の確認
            echo("チャンネル情報を確認中...")
            val channelInfo = apiClient.getChannelInfo(channelId)
            if (channelInfo == null) {
                echo("エラー: チャンネル情報を取得できませんでした", err = true)
                exitProcess(1)
            }
            echo("チャンネル名: ${channelInfo.name}")

            // 集計フロー実行
            // 1. 自チャンネルの動画取得
            // 2. external の動画取得
            // 3. videoId => Video の Map 作成
            // 4. overrides 適用
            // 5. sessions 構築
            // 6. 集計済みデータ生成（配信時間・遅刻時間・視聴数・高評価数）
            echo("集計を実行中...")
            val aggregateService = AggregateService(apiClient)
            val aggregateReport = aggregateService.aggregate(
                channelId = channelId,
                appConfig = appConfig,
                maxVideos = limit
            )

            // 7. result.json 出力
            echo("result.json を出力中...")
            val exporter = AggregateExporter()
            exporter.export(aggregateReport, outputPath)

            echo("完了しました: $outputPath")
            echo()
            echo("=== 集計結果サマリー ===")
            val streams = aggregateReport.years.flatMap { it.streams } + aggregateReport.unknown
            echo("総配信数: ${streams.size}件 (セッション: ${streams.count { it.linked }}件 / スタンドアロン: ${streams.count { !it.linked }}件)")
            val totalDuration = streams.sumOf { it.durationSeconds }
            val totalDelay = streams.sumOf { it.delaySeconds }
            val totalView = streams.sumOf { it.viewCount }
            val totalLike = streams.sumOf { it.likeCount }
            if (totalDuration > 0) echo("総配信時間  : ${DateTimeUtil.formatDuration(totalDuration)}")
            if (totalDelay > 0)    echo("総遅刻時間  : ${DateTimeUtil.formatDuration(totalDelay)}")
            echo("総視聴数    : $totalView")
            echo("総高評価数  : $totalLike")
            echo()

            // 年度別の内訳
            echo("=== 年度別内訳 ===")
            if (aggregateReport.debutDate != null) {
                echo("デビュー日: ${aggregateReport.debutDate}")
            }
            aggregateReport.years.forEach { year ->
                val period = if (year.startDate.isNotEmpty()) " (${year.startDate} 〜 ${year.endDate})" else ""
                echo("${year.label}$period: ${year.streams.size}件")
            }
            if (aggregateReport.unknown.isNotEmpty()) {
                echo("年度不明: ${aggregateReport.unknown.size}件")
            }
            echo()
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