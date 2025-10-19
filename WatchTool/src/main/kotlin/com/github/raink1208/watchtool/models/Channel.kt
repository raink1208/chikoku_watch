package com.github.raink1208.watchtool.models

import kotlinx.serialization.Serializable

@Serializable
data class Channel(
    val id: String,
    val name: String,
    val description: String = "",
    val subscriberCount: Long = 0,
    val videoCount: Long = 0,
    val viewCount: Long = 0
)
