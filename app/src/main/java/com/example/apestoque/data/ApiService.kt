package com.example.apestoque.data

import retrofit2.http.GET

interface ApiService {
    @GET("api/solicitacoes")
    suspend fun listarSolicitacoes(): List<Solicitacao>
}
