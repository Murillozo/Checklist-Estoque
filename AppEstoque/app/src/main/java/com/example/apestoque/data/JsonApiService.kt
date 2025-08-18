package com.example.apestoque.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface JsonApiService {
    @POST("checklist")
    suspend fun salvarChecklist(
        @Body body: ChecklistRequest
    ): ChecklistResponse

    @GET("revisao")
    suspend fun listarRevisao(): List<RevisaoChecklist>

    @POST("revisao/reenviar")
    suspend fun reenviarChecklist(
        @Body body: ReenvioRequest
    ): ChecklistResponse

    @GET("expedicao/projects")
    suspend fun listarExpedicaoProjetos(): ProjetoListResponse

    @GET("expedicao/checklist")
    suspend fun obterExpedicaoChecklist(
        @Query("obra") obra: String
    ): ExpedicaoChecklist

    @POST("expedicao/upload")
    suspend fun enviarExpedicaoChecklist(
        @Body body: ExpedicaoUploadRequest
    ): ChecklistResponse
}