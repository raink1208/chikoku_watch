package com.github.raink1208.watchtool.api

import com.github.raink1208.watchtool.models.Channel
import com.github.raink1208.watchtool.models.Video
import com.github.raink1208.watchtool.utils.DateTimeUtil
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

class YouTubeApiClient(private val config: ApiConfig) {
    private val logger = LoggerFactory.getLogger(YouTubeApiClient::class.java)
    private val youtube: YouTube

    init {
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        youtube = YouTube.Builder(httpTransport, jsonFactory) { request ->
            request.connectTimeout = 60000
            request.readTimeout = 60000
        }
            .setApplicationName("YouTube-Stream-Analyzer")
            .build()
    }

    suspend fun getChannelInfo(channelId: String): Channel? {
        return try {
            val request = youtube.channels()
                .list(listOf("snippet", "statistics"))
                .setId(listOf(channelId))
                .setKey(config.apiKey)

            config.addQuotaUsage(1)
            val response = request.execute()

            response.items?.firstOrNull()?.let { channel ->
                Channel(
                    id = channel.id,
                    name = channel.snippet.title,
                    description = channel.snippet.description ?: "",
                    subscriberCount = channel.statistics?.subscriberCount?.toLong() ?: 0,
                    videoCount = channel.statistics?.videoCount?.toLong() ?: 0,
                    viewCount = channel.statistics?.viewCount?.toLong() ?: 0
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch channel info: ${e.message}", e)
            handleApiError(e)
            null
        }
    }

    suspend fun searchVideos(
        channelId: String,
        maxResults: Int = 50,
        pageToken: String? = null
    ): Pair<List<String>, String?> {
        return try {
            // まずチャンネルのアップロードプレイリストIDを取得
            val channelRequest = youtube.channels()
                .list(listOf("contentDetails"))
                .setId(listOf(channelId))
                .setKey(config.apiKey)
            
            config.addQuotaUsage(1)
            val channelResponse = channelRequest.execute()
            
            val uploadsPlaylistId = channelResponse.items?.firstOrNull()
                ?.contentDetails?.relatedPlaylists?.uploads
            
            if (uploadsPlaylistId == null) {
                logger.error("Could not find uploads playlist for channel: $channelId")
                return Pair(emptyList(), null)
            }
            
            // playlistItemsを使用して動画を取得
            val request = youtube.playlistItems()
                .list(listOf("contentDetails"))
                .setPlaylistId(uploadsPlaylistId)
                .setMaxResults(maxResults.toLong())
                .setKey(config.apiKey)

            if (pageToken != null) {
                request.pageToken = pageToken
            }

            config.addQuotaUsage(1)
            val response = request.execute()

            val videoIds = response.items?.mapNotNull { it.contentDetails?.videoId } ?: emptyList()
            val nextPageToken = response.nextPageToken

            logger.info("Found ${videoIds.size} videos from playlist, nextPageToken: $nextPageToken")

            Pair(videoIds, nextPageToken)
        } catch (e: Exception) {
            logger.error("Failed to get videos from playlist: ${e.message}", e)
            handleApiError(e)
            Pair(emptyList(), null)
        }
    }

    suspend fun getVideoDetails(videoIds: List<String>): List<Video> {
        if (videoIds.isEmpty()) return emptyList()

        return try {
            val request = youtube.videos()
                .list(listOf("snippet", "liveStreamingDetails", "statistics"))
                .setId(videoIds)
                .setKey(config.apiKey)

            config.addQuotaUsage(1)
            val response = request.execute()

            response.items?.mapNotNull { video ->
                try {
                    val snippet = video.snippet
                    val liveDetails = video.liveStreamingDetails
                    val stats = video.statistics

                    // ライブ配信の詳細がない場合はスキップ
                    if (liveDetails == null) {
                        logger.debug("Video ${video.id} has no live streaming details, skipping")
                        return@mapNotNull null
                    }

                    val scheduledStartTime = liveDetails.scheduledStartTime?.toString()
                    val actualStartTime = liveDetails.actualStartTime?.toString()
                    val actualEndTime = liveDetails.actualEndTime?.toString()

                    // actualEndTimeがない場合（配信が終了していない場合）はスキップ
                    if (actualEndTime == null) {
                        logger.debug("Video ${video.id} has no actual end time (stream not finished), skipping")
                        return@mapNotNull null
                    }

                    val delaySeconds = DateTimeUtil.calculateDelaySeconds(scheduledStartTime, actualStartTime)
                    val streamDurationSeconds = DateTimeUtil.calculateStreamDurationSeconds(actualStartTime, actualEndTime)

                    Video(
                        videoId = video.id,
                        title = snippet?.title ?: "",
                        publishedAt = snippet?.publishedAt?.toString() ?: "",
                        scheduledStartTime = scheduledStartTime,
                        actualStartTime = actualStartTime,
                        actualEndTime = actualEndTime,
                        concurrentViewers = liveDetails.concurrentViewers?.toLong(),
                        viewCount = stats?.viewCount?.toLong() ?: 0,
                        likeCount = stats?.likeCount?.toLong() ?: 0,
                        commentCount = stats?.commentCount?.toLong() ?: 0,
                        delaySeconds = delaySeconds,
                        streamDurationSeconds = streamDurationSeconds
                    )
                } catch (e: Exception) {
                    logger.warn("Failed to parse video ${video.id}: ${e.message}")
                    null
                }
            } ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to fetch video details: ${e.message}", e)
            handleApiError(e)
            emptyList()
        }
    }

    private suspend fun handleApiError(e: Exception) {
        when {
            e.message?.contains("403") == true -> {
                logger.error("API key is invalid or quota exceeded")
                throw IllegalStateException("API authentication failed or quota exceeded", e)
            }
            e.message?.contains("429") == true -> {
                logger.warn("Rate limit exceeded, waiting before retry...")
                delay(5000)
            }
            e.message?.contains("500") == true || e.message?.contains("503") == true -> {
                logger.warn("YouTube API server error, waiting before retry...")
                delay(2000)
            }
        }
    }

    fun getUsedQuota(): Int = config.getUsedQuota()
}