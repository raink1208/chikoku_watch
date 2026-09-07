package com.github.raink1208.watchtool.service

import com.github.raink1208.watchtool.models.AggregateReport
import com.github.raink1208.watchtool.models.AggregateStream
import com.github.raink1208.watchtool.models.YearGroup
import com.github.raink1208.watchtool.utils.DateTimeUtil
import org.slf4j.LoggerFactory
import java.time.temporal.ChronoUnit

/**
 * 配信をデビュー日からの経過年数（何年目）でグループ化する。
 *
 * 各グループは記念日区切り。デビュー日が 2024-01-13 の場合:
 * - 1年目: 2024-01-13 〜 2025-01-12
 * - 2年目: 2025-01-13 〜 2026-01-12
 */
class YearGrouper {
    private val logger = LoggerFactory.getLogger(YearGrouper::class.java)

    fun group(streams: List<AggregateStream>, debutDate: String?): AggregateReport {
        val debut = DateTimeUtil.parseLocalDate(debutDate)

        if (debut == null) {
            if (!debutDate.isNullOrBlank()) {
                logger.warn("デビュー日のパースに失敗しました（YYYY-MM-DD 形式で指定してください）: $debutDate")
            } else {
                logger.info("config.json に debutDate が未設定のため年度分割を行いません")
            }
            // 年度を決められないので全件を1グループにまとめる
            return AggregateReport(
                debutDate = null,
                years = if (streams.isEmpty()) {
                    emptyList()
                } else {
                    listOf(YearGroup(index = 0, label = "全期間", startDate = "", endDate = "", streams = streams))
                },
                unknown = emptyList()
            )
        }

        // scheduledStartTime から年度を決められない配信は unknown に退避する（取りこぼさない）
        val unknown = mutableListOf<AggregateStream>()
        val byIndex = mutableMapOf<Int, MutableList<AggregateStream>>()

        streams.forEach { stream ->
            val scheduled = DateTimeUtil.parseLocalDate(stream.scheduledStartTime)
            if (scheduled == null) {
                unknown.add(stream)
                return@forEach
            }
            // デビュー日より前の配信は1年目に含める（負の年数にはしない）
            val index = ChronoUnit.YEARS.between(debut, scheduled).toInt().coerceAtLeast(0)
            byIndex.getOrPut(index) { mutableListOf() }.add(stream)
        }

        // 配信が0件の年も空配列で埋めて 0 から連番にする
        val maxIndex = byIndex.keys.maxOrNull() ?: -1
        val years = (0..maxIndex).map { index ->
            val start = debut.plusYears(index.toLong())
            val end = debut.plusYears(index + 1L).minusDays(1)
            YearGroup(
                index = index,
                label = "${index + 1}年目",
                startDate = start.toString(),
                endDate = end.toString(),
                streams = byIndex[index] ?: emptyList()
            )
        }

        logger.info(
            "年度分割完了 - デビュー日: $debut, " +
                "年数: ${years.size}年分, " +
                "内訳: ${years.joinToString(", ") { "${it.label}=${it.streams.size}件" }}" +
                if (unknown.isNotEmpty()) ", 年度不明: ${unknown.size}件" else ""
        )

        return AggregateReport(
            debutDate = debut.toString(),
            years = years,
            unknown = unknown
        )
    }
}
