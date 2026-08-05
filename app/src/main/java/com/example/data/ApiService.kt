package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body body: Map<String, String>): retrofit2.Response<LoginResponse>

    @GET("api/products")
    suspend fun getProducts(@Query("db_name") dbName: String): List<ProductResponse>

    @POST("api/products")
    suspend fun addProduct(@Body body: ProductAddRequest): retrofit2.Response<Map<String, Any>>

    @PUT("api/products/{id}/stock")
    suspend fun updateStock(@Path("id") productId: String, @Body body: StockUpdateRequest): retrofit2.Response<Map<String, Any>>

    @DELETE("api/products/{id}")
    suspend fun deleteProduct(@Path("id") productId: String, @Query("db_name") dbName: String): retrofit2.Response<Map<String, Any>>

    @GET("api/transactions")
    suspend fun getTransactions(@Query("db_name") dbName: String): List<TransactionResponse>

    @POST("api/transactions")
    suspend fun addTransaction(@Body body: TransactionAddRequest): retrofit2.Response<Map<String, Any>>

    // --- AREAS ENDPOINTS ---
    @GET("api/areas")
    suspend fun getAreas(@Query("db_name") dbName: String): List<AreaResponse>

    @POST("api/areas")
    suspend fun addArea(@Body body: Map<String, String>): retrofit2.Response<Map<String, Any>>

    @PUT("api/areas/{id}")
    suspend fun updateArea(@Path("id") id: String, @Body body: Map<String, String>): retrofit2.Response<Map<String, Any>>

    @DELETE("api/areas/{id}")
    suspend fun deleteArea(@Path("id") id: String, @Query("db_name") dbName: String): retrofit2.Response<Map<String, Any>>

    // --- ADMINS ENDPOINTS ---
    @GET("api/admins")
    suspend fun getAdmins(@Query("db_name") dbName: String): List<AdminResponse>

    @POST("api/admins")
    suspend fun addAdmin(@Body body: Map<String, String>): retrofit2.Response<Map<String, Any>>

    @PUT("api/admins/{id}")
    suspend fun updateAdmin(@Path("id") id: String, @Body body: Map<String, String>): retrofit2.Response<Map<String, Any>>

    @DELETE("api/admins/{id}")
    suspend fun deleteAdmin(@Path("id") id: String, @Query("db_name") dbName: String): retrofit2.Response<Map<String, Any>>
}

data class AreaResponse(
    val id: String,
    val name: String
)

data class AdminResponse(
    val id: String,
    val name: String,
    val username: String,
    val password: String,
    val role: String,
    val area: String
)

data class LoginResponse(
    val success: Boolean,
    val tenant: TenantResponseConfig?,
    val error: String?
)

data class TenantResponseConfig(
    val id: String,
    val name: String,
    val ownerName: String,
    val dbName: String,
    val accentColor: String,
    val businessType: String
)

data class ProductResponse(
    val id: String,
    val name: String,
    val sku: String,
    val category: String,
    val price: Double,
    val modalPrice: Double? = 0.0,
    val stock: Int,
    val minStockAlert: Int
)

data class ProductAddRequest(
    val db_name: String,
    val name: String,
    val sku: String,
    val category: String,
    val price: Double,
    val modal_price: Double? = 0.0,
    val stock: Int,
    val min_stock_alert: Int
)

data class StockUpdateRequest(
    val db_name: String,
    val stock: Int
)

data class TransactionResponse(
    val id: String,
    val productName: String,
    val sku: String,
    val quantity: Int,
    val totalPrice: Double,
    val timestamp: Long,
    val operator: String
)

data class TransactionAddRequest(
    val db_name: String,
    val id: String,
    val productName: String,
    val sku: String,
    val quantity: Int,
    val totalPrice: Double,
    val timestamp: Long,
    val operator: String
)

object ApiClient {
    // Default connection to the production VPS server
    var baseUrl = "http://103.253.245.25:3900/"
        set(value) {
            field = if (value.endsWith("/")) value else "$value/"
            retrofit = null
            apiService = null
        }

    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null

    private fun getRetrofit(): Retrofit {
        if (retrofit == null) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        }
        return retrofit!!
    }

    fun getService(): ApiService {
        if (apiService == null) {
            apiService = getRetrofit().create(ApiService::class.java)
        }
        return apiService!!
    }
}
