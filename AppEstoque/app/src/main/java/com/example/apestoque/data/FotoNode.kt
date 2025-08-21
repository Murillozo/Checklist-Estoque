package com.example.apestoque.data

data class FotoNode(
    val name: String,
    val children: List<FotoNode>? = null
)
