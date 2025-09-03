package com.example.apestoque.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object JsonNetworkModule {
    fun api(context: Context): JsonApiService {
        val ip = context.getSharedPreferences("app", Context.MODE_PRIVATE)
            .getString("api_ip", "192.168.0.135")
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://$ip:5000/json_api/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        return retrofit.create(JsonApiService::class.java)
    }
}
