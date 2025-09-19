package com.example.apestoque.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
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

    @GET("api/fotos")
    suspend fun listarFotos(): List<FotoNode>

    @Multipart
    @POST("api/fotos/upload")
    suspend fun enviarFoto(
        @Part("ano") ano: RequestBody,
        @Part("obra") obra: RequestBody,
        @Part foto: MultipartBody.Part,
    )
}

