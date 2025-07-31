package com.example.apestoque.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ComprasRequest(
    val pendencias: List<Item>
)
