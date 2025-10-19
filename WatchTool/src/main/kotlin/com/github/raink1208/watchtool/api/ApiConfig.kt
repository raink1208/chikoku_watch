package com.github.raink1208.watchtool.api

import com.github.raink1208.watchtool.utils.ConfigLoader

data class ApiConfig(
    val apiKey: String = ConfigLoader.getApiKey(),
    val quotaLimit: Int = ConfigLoader.getQuotaLimit(),
    val maxResults: Int = ConfigLoader.getMaxResults(),
    val maxVideos: Int = ConfigLoader.getMaxVideos()
) {
    private var usedQuota = 0

    fun addQuotaUsage(cost: Int) {
        usedQuota += cost
    }

    fun getUsedQuota(): Int = usedQuota

    fun hasQuotaRemaining(): Boolean = usedQuota < quotaLimit
}