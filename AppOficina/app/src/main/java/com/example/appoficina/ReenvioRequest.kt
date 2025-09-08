package com.example.appoficina

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReenvioRequest(
    val obra: String,
    val ano: String
)

