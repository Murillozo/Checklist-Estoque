package com.example.apestoque.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.example.apestoque.data.IntListAdapter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object NetworkModule {
    fun api(context: Context): ApiService {
        val ip = context.getSharedPreferences("app", Context.MODE_PRIVATE)
            .getString("api_ip", "192.168.0.135")
        val moshi = Moshi.Builder()
            .add(IntListAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://$ip:5000/projetista/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        return retrofit.create(ApiService::class.java)
    }
}
