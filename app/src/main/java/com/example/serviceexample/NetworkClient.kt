package com.example.serviceexample

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor

class NetworkClient {

    val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    fun request(url: String): String? {
        val requestBuilder = Request.Builder()
            .url(url)
            .build()

        try {
            val response = client.newCall(requestBuilder).execute()
            if (response.isSuccessful) {
                return response.body?.string()
            }
        } catch (e: Exception) {
            Log.e("NetworkClient", "error during network call", e)
        }

        return null
    }
}