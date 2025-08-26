package com.example.apestoque.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InspecaoItem(
    val id: Int,
    val referencia: String,
    val quantidade: Int,
    var verificado: Boolean = false,
    var faltante: Int = 0
)

@JsonClass(generateAdapter = true)
data class InspecaoSolicitacao(
    val id: Int,
    val itens: List<InspecaoItem>
)

@JsonClass(generateAdapter = true)
data class InspecaoResultadoItem(
    val id: Int,
    val verificado: Boolean,
    val faltante: Int
)

@JsonClass(generateAdapter = true)
data class InspecaoResultadoRequest(
    val itens: List<InspecaoResultadoItem>
)
