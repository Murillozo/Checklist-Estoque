package com.example.apestoque.data

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("api/solicitacoes")
    suspend fun listarSolicitacoes(): List<Solicitacao>

    @POST("api/solicitacoes/{id}/aprovar")
    suspend fun aprovarSolicitacao(@Path("id") id: Int)
}
