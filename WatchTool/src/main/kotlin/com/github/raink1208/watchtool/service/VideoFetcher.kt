package com.github.raink1208.watchtool.service

import com.github.raink1208.watchtool.api.YouTubeApiClient
import com.github.raink1208.watchtool.models.Video
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

class VideoFetcher(private val apiClient: YouTubeApiClient) {
    private val logger = LoggerFactory.getLogger(VideoFetcher::class.java)

    suspend fun fetchAllVideos(
        channelId: String,
        maxVideos: Int = Int.MAX_VALUE
    ): List<Video> {
        val allVideos = mutableListOf<Video>()
        var pageToken: String? = null
        var retryCount = 0
        val maxRetries = 3

        logger.info("Starting to fetch videos for channel: $channelId")

        do {
            try {
                val (videoIds, nextPageToken) = apiClient.searchVideos(
                    channelId = channelId,
                    maxResults = 50,
                    pageToken = pageToken
                )

                if (videoIds.isEmpty()) {
                    logger.info("No more videos found")
                    break
                }

                // 動画の詳細を取得（50件ずつ）
                val videos = apiClient.getVideoDetails(videoIds)

                allVideos.addAll(videos)
                logger.info("Fetched ${videos.size} videos (total: ${allVideos.size})")

                pageToken = nextPageToken
                retryCount = 0

                // レート制限対策
                if (pageToken != null) {
                    delay(500)
                }

                // 最大件数チェック
                if (allVideos.size >= maxVideos) {
                    logger.info("Reached maximum video limit: $maxVideos")
                    break
                }

            } catch (e: Exception) {
                logger.error("Error fetching videos: ${e.message}", e)
                retryCount++

                if (retryCount >= maxRetries) {
                    logger.error("Max retries reached, stopping fetch")
                    break
                }

                val waitTime = (1000L * retryCount * retryCount) // 指数バックオフ
                logger.warn("Retrying in ${waitTime}ms... (attempt $retryCount/$maxRetries)")
                delay(waitTime)
            }

        } while (pageToken != null)

        logger.info("Finished fetching videos. Total: ${allVideos.size}")
        logger.info("Total API quota used: ${apiClient.getUsedQuota()}")

        return allVideos.take(maxVideos)
    }
}

