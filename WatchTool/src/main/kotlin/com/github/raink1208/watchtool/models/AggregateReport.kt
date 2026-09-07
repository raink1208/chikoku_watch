package com.github.raink1208.watchtool.models

import kotlinx.serialization.Serializable

/** セッション内の各動画を指す参照 */
@Serializable
data class VideoRef(
    val videoId: String,
    val title: String,
    val channelId: String
)

/** 1配信イベント分の集計データ */
@Serializable
data class AggregateStream(
    val id: String,
    val title: String,
    val durationSeconds: Long,
    val delaySeconds: Long,
    val viewCount: Long,
    val likeCount: Long,
    val commentCount: Long,
    val scheduledStartTime: String,
    val actualStartTime: String? = null,
    val actualEndTime: String? = null,
    /** config.json の sessions に明示的に紐付けられているか */
    val linked: Boolean,
    val videos: List<VideoRef>
)

/** デビューから何年目かで区切った1グループ */
@Serializable
data class YearGroup(
    /** 0 = 1年目。欠番なく 0 から連番で並ぶ */
    val index: Int,
    /** 表示用ラベル（例: "1年目"） */
    val label: String,
    /** グループの開始日（YYYY-MM-DD）。debutDate 未設定時は空文字 */
    val startDate: String,
    /** グループの終了日（YYYY-MM-DD、当日を含む）。debutDate 未設定時は空文字 */
    val endDate: String,
    /** scheduledStartTime 昇順。配信がない年は空配列 */
    val streams: List<AggregateStream>
)

@Serializable
data class AggregateReport(
    /** 年度分割の基準日（YYYY-MM-DD）。config.json 未設定時は null */
    val debutDate: String? = null,
    /** 年度別グループ。debutDate 未設定時は全件を含む1グループのみ */
    val years: List<YearGroup>,
    /** scheduledStartTime が不明で年度を決められなかった配信 */
    val unknown: List<AggregateStream> = emptyList()
)
