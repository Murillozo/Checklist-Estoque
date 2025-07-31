package com.example.appproducao.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ComprasRequest(
    val pendencias: List<Item>
)
