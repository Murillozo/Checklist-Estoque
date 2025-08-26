package com.example.apestoque.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChecklistItem(
    val numero: Int,
    val pergunta: String,
    val resposta: List<String>
)

@JsonClass(generateAdapter = true)
data class ChecklistMaterial(
    val material: String,
    val quantidade: Int,
    val completo: Boolean
)

@JsonClass(generateAdapter = true)
data class ChecklistRequest(
    val obra: String,
    val ano: String,
    val suprimento: String,
    val itens: List<ChecklistItem>,
    val materiais: List<ChecklistMaterial>
)
