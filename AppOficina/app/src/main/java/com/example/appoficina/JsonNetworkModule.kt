package com.example.appoficina

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object JsonNetworkModule {
    fun api(context: Context): JsonApiService {
        val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)
        val ip = prefs.getString("api_ip", "192.168.0.135")

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://$ip:5000/json_api/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(JsonApiService::class.java)
    }
}

