package com.example.appoficina

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import com.squareup.moshi.JsonClass

interface JsonApiService {
    @GET("revisao")
    suspend fun listarRevisao(): List<RevisaoChecklist>

    @POST("revisao/reenviar")
    suspend fun reenviarChecklist(
        @Body body: ReenvioRequest
    ) : ChecklistResponse
}

@JsonClass(generateAdapter = true)
data class ChecklistResponse(
    val status: String
)

