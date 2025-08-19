package com.example.apestoque.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InspecaoSolicitacao(
    val id: Int,
    val itens: List<Item>
)
