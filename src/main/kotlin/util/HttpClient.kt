package org.huvz.mirai.plugin.util

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class HttpClient {

    companion object{
        val okHttpClient: OkHttpClient by lazy {
            /**
             * 初始化
             */
            OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
        }
        fun getHttp(url:String):ByteArray{
            val request = Request.Builder()
                .url(url)
                .build()
            return okHttpClient.newCall(request).execute().body!!.bytes()
        }
        public suspend fun PostHttp(url:String,data:String):ByteArray{
            var type:String = "application/x-www-form-urlencoded;";
            var  body = data.toRequestBody(type.toMediaTypeOrNull())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()
            return okHttpClient.newCall(request).execute().body!!.bytes()
        }
    }

}