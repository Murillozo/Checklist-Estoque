package com.example.apestoque.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface JsonApiService {
    @POST("checklist")
    suspend fun salvarChecklist(
        @Body body: ChecklistRequest
    ): ChecklistResponse

    @GET("revisao")
    suspend fun listarRevisao(): List<RevisaoChecklist>
}
