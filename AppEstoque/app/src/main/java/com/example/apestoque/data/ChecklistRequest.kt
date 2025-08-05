package com.example.apestoque.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChecklistRequest(
    val obra: String,
    val conteudo: String
)
