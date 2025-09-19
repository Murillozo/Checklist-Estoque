package com.example.apestoque.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Projeto(
    val arquivo: String,
    val obra: String,
    val ano: String
)

@JsonClass(generateAdapter = true)
data class ProjetoListResponse(
    val projetos: List<Projeto>
)
