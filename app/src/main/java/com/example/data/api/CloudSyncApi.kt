package com.example.data.api

import com.example.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// 1. Cloud Database Sync Request & Response Payloads
data class CloudSyncPayload(
    val lastSyncTime: Long,
    val users: List<User>,
    val fournisseurs: List<Fournisseur>,
    val clients: List<Client>,
    val depots: List<Depot>,
    val articles: List<Article>,
    val entrees: List<Entree>,
    val sorties: List<Sortie>,
    val mouvements: List<MouvementStock>,
    val stockDepots: List<StockDepot>
)

data class CloudSyncResponse(
    val success: Boolean,
    val message: String,
    val timestamp: Long,
    // Returned data from server (to merge back)
    val users: List<User>?,
    val fournisseurs: List<Fournisseur>?,
    val clients: List<Client>?,
    val depots: List<Depot>?,
    val articles: List<Article>?,
    val entrees: List<Entree>?,
    val sorties: List<Sortie>?,
    val mouvements: List<MouvementStock>?,
    val stockDepots: List<StockDepot>?
)

// 2. Retrofit API Interface
interface CloudSyncApiService {
    
    @POST("sync/push")
    suspend fun pushLocalData(
        @Header("Authorization") token: String,
        @Body payload: CloudSyncPayload
    ): Response<CloudSyncResponse>

    @GET("sync/pull")
    suspend fun pullCloudData(
        @Header("Authorization") token: String
    ): Response<CloudSyncResponse>
    
    @POST("sync/full")
    suspend fun syncBidirectional(
        @Header("Authorization") token: String,
        @Body payload: CloudSyncPayload
    ): Response<CloudSyncResponse>
}

// 3. Retrofit API client builder
object CloudSyncClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun getApiService(baseUrl: String): CloudSyncApiService {
        // Clean URL ensure it ends with slash
        val cleanedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(cleanedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CloudSyncApiService::class.java)
    }
}
