package com.example.apestoque.data

import retrofit2.http.Body
import retrofit2.http.POST

interface JsonApiService {
    @POST("checklist")
    suspend fun salvarChecklist(
        @Body body: ChecklistRequest
    ): ChecklistResponse
}
