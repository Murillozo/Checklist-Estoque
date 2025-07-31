package com.example.apestoque.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object NetworkModule {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.0.135:5000/projetista/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: ApiService = retrofit.create(ApiService::class.java)
}
