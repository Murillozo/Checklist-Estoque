package com.example.apestoque.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("api/inspecoes")
    suspend fun listarInspecoes(): List<InspecaoSolicitacao>

    @GET("api/solicitacoes")
    suspend fun listarSolicitacoes(): List<Solicitacao>

    @POST("api/solicitacoes/{id}/aprovar")
    suspend fun aprovarSolicitacao(@Path("id") id: Int)

    @POST("api/solicitacoes/{id}/compras")
    suspend fun marcarCompras(
        @Path("id") id: Int,
        @Body body: ComprasRequest
    )

    @POST("api/inspecoes/{id}/resultado")
    suspend fun enviarResultadoInspecao(
        @Path("id") id: Int,
        @Body body: InspecaoResultadoRequest
    )
}

