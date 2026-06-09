package com.github.raink1208.watchtool.service

import com.github.raink1208.watchtool.api.YouTubeApiClient
import com.github.raink1208.watchtool.models.AggregateReport
import com.github.raink1208.watchtool.models.AggregateStream
import com.github.raink1208.watchtool.models.AppConfig
import com.github.raink1208.watchtool.models.Video
import com.github.raink1208.watchtool.models.VideoRef
import org.slf4j.LoggerFactory

class AggregateService(private val apiClient: YouTubeApiClient) {
    private val logger = LoggerFactory.getLogger(AggregateService::class.java)

    suspend fun aggregate(
        channelId: String,
        appConfig: AppConfig,
        maxVideos: Int = Int.MAX_VALUE
    ): AggregateReport {

        // 1. 自チャンネルの動画取得
        logger.info("自チャンネルの動画を取得中: $channelId")
        val fetcher = VideoFetcher(apiClient)
        val ownVideos = fetcher.fetchAllVideos(channelId = channelId, maxVideos = maxVideos)
        logger.info("自チャンネル動画: ${ownVideos.size}件")

        // 2. external の動画取得
        val externalVideos = if (appConfig.external.isNotEmpty()) {
            logger.info("external 動画を取得中: ${appConfig.external.size}件")
            appConfig.external
                .chunked(50)
                .flatMap { chunk -> apiClient.getVideoDetails(chunk) }
                .also { logger.info("external 動画取得完了: ${it.size}件") }
        } else {
            emptyList()
        }

        // 3. videoId => Video の Map 作成
        val videoMap = mutableMapOf<String, Video>()
        ownVideos.forEach { videoMap[it.videoId] = it }
        externalVideos.forEach { videoMap[it.videoId] = it }
        logger.info("videoMap 構築完了: ${videoMap.size}件")

        // 4. overrides 適用
        appConfig.overrides.forEach { override ->
            val existing = videoMap[override.videoId]
            if (existing != null) {
                videoMap[override.videoId] = existing.copy(
                    title = override.title ?: existing.title,
                    publishedAt = override.publishedAt?.takeIf { it.isNotEmpty() } ?: existing.publishedAt,
                    scheduledStartTime = override.scheduledStartTime ?: existing.scheduledStartTime,
                    actualStartTime = override.actualStartTime ?: existing.actualStartTime,
                    actualEndTime = override.actualEndTime ?: existing.actualEndTime,
                    viewCount = override.viewCount ?: existing.viewCount,
                    likeCount = override.likeCount ?: existing.likeCount,
                    commentCount = override.commentCount ?: existing.commentCount,
                    delaySeconds = override.delaySeconds ?: existing.delaySeconds,
                    streamDurationSeconds = override.streamDurationSeconds ?: existing.streamDurationSeconds,
                    thumbnail = override.thumbnail ?: existing.thumbnail
                )
                logger.debug("override 適用: ${override.videoId}")
            } else {
                // APIから取得できない動画（削除済みなど）をoverrideで補完
                // external リストにない場合は自チャンネル動画とみなす
                val videoChannelId = if (override.videoId in appConfig.external) "" else channelId
                videoMap[override.videoId] = Video(
                    videoId = override.videoId,
                    channelId = videoChannelId,
                    title = override.title ?: "",
                    publishedAt = override.publishedAt ?: "",
                    scheduledStartTime = override.scheduledStartTime,
                    actualStartTime = override.actualStartTime,
                    actualEndTime = override.actualEndTime,
                    viewCount = override.viewCount ?: 0,
                    likeCount = override.likeCount ?: 0,
                    commentCount = override.commentCount ?: 0,
                    delaySeconds = override.delaySeconds ?: 0,
                    streamDurationSeconds = override.streamDurationSeconds ?: 0,
                    thumbnail = override.thumbnail
                )
                logger.debug("override で動画を追加: ${override.videoId} (channelId=$videoChannelId)")
            }
        }
        logger.info("overrides 適用完了: ${appConfig.overrides.size}件")

        // 5. sessions 構築
        // sessions に含まれる全 videoId を収集（linked 判定用）
        val sessionVideoIds = appConfig.sessions.flatMap { it.videos }.toSet()

        // sessions → linked: true の AggregateStream
        val sessionStreams = appConfig.sessions.map { sessionConfig ->
            val sessionVideos = sessionConfig.videos.mapNotNull { vid ->
                videoMap[vid].also {
                    if (it == null) logger.warn("セッション[${sessionConfig.id}]: 動画が見つかりません videoId=$vid")
                }
            }
            // 自チャンネルの動画を primary として使用（スケジュール・遅刻時間の基準）
            val primaryVideo = sessionVideos.firstOrNull { it.channelId == channelId }
                ?: sessionVideos.firstOrNull()

            AggregateStream(
                id = sessionConfig.id,
                title = sessionConfig.name,
                durationSeconds = sessionVideos.sumOf { it.streamDurationSeconds },
                delaySeconds = primaryVideo?.delaySeconds ?: 0,
                viewCount = sessionVideos.sumOf { it.viewCount },
                likeCount = sessionVideos.sumOf { it.likeCount },
                commentCount = sessionVideos.sumOf { it.commentCount },
                scheduledStartTime = primaryVideo?.scheduledStartTime ?: "",
                actualStartTime = primaryVideo?.actualStartTime,
                actualEndTime = primaryVideo?.actualEndTime,
                linked = true,
                videos = sessionVideos.map { VideoRef(it.videoId, it.title, it.channelId) }
            ).also {
                logger.info(
                    "セッション[${it.id}] ${it.title}: " +
                        "動画${sessionVideos.size}件 " +
                        "配信時間=${it.durationSeconds}s " +
                        "遅刻=${it.delaySeconds}s " +
                        "視聴=${it.viewCount} 高評価=${it.likeCount}"
                )
            }
        }

        // 自チャンネルの動画のうち、どのセッションにも属さないもの → linked: false
        val standaloneStreams = videoMap.values
            .filter { it.channelId == channelId && it.videoId !in sessionVideoIds }
            .map { video ->
                AggregateStream(
                    id = video.videoId,
                    title = video.title,
                    durationSeconds = video.streamDurationSeconds,
                    delaySeconds = video.delaySeconds,
                    viewCount = video.viewCount,
                    likeCount = video.likeCount,
                    commentCount = video.commentCount,
                    scheduledStartTime = video.scheduledStartTime ?: "",
                    actualStartTime = video.actualStartTime,
                    actualEndTime = video.actualEndTime,
                    linked = false,
                    videos = listOf(VideoRef(video.videoId, video.title, video.channelId))
                )
            }

        logger.info("sessionStreams: ${sessionStreams.size}件, standaloneStreams: ${standaloneStreams.size}件")

        // 6. 集計済みデータ生成 → scheduledStartTime で昇順ソート
        val allStreams = (sessionStreams + standaloneStreams)
            .sortedWith(compareBy { it.scheduledStartTime })

        return AggregateReport(streams = allStreams)
    }
}
