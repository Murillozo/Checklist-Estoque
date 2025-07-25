package com.example.apestoque.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Item(
    val referencia: String,
    val quantidade: Int
)

@JsonClass(generateAdapter = true)
data class Solicitacao(
    val id: Int,
    val obra: String,
    val data: String,
    val itens: List<Item>
)
