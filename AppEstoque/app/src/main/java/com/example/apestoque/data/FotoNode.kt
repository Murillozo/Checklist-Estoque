package com.example.apestoque.data

data class FotoNode(
    val name: String,
    val path: String? = null,
    val children: List<FotoNode>? = null,
)
