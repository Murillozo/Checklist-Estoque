package com.example.appproducao.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChecklistItem(
    val descricao: String,
    var status: String
)

@JsonClass(generateAdapter = true)
data class Checklist(
    val obra: String,
    val itens: List<ChecklistItem>,
    var responsavel: String? = null
)
