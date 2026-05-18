package com.app.moneylens.api

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * MoneyLens API Service
 * Base URL: http://moneylens.datanex.org/api
 */
interface MoneyLensApiService {

    // ─── User Endpoints ────────────────────────────────────────────

    /**
     * POST /moneylens-users
     * Membuat user baru atau update jika sudah ada
     */
    @POST("moneylens-users")
    fun createUser(@Body request: CreateUserRequest): Call<ApiResponse<UserResponse>>

    /**
     * GET /moneylens-users/{id}
     * Ambil detail user
     */
    @GET("moneylens-users/{id}")
    fun getUserDetail(@Path("id") id: Int): Call<ApiResponse<UserResponse>>

    /**
     * GET /moneylens-users?search=...&per_page=...
     * List users dengan pagination
     */
    @GET("moneylens-users")
    fun listUsers(
        @Query("search") search: String? = null,
        @Query("per_page") perPage: Int? = null
    ): Call<ApiResponse<List<UserResponse>>>

    /**
     * PUT /moneylens-users/{id}
     * Update user data
     */
    @PUT("moneylens-users/{id}")
    fun updateUser(
        @Path("id") id: Int,
        @Body request: UpdateUserRequest
    ): Call<ApiResponse<UserResponse>>
}

// ─── Request/Response Models ────────────────────────────────────

data class CreateUserRequest(
    @SerializedName("firebase_uid")
    val firebaseUid: String,
    
    @SerializedName("provider")
    val provider: String = "google.com",
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("email_verified")
    val emailVerified: Boolean = true,
    
    @SerializedName("display_name")
    val displayName: String,
    
    @SerializedName("photo_url")
    val photoUrl: String? = null,
    
    @SerializedName("device_brand")
    val deviceBrand: String,
    
    @SerializedName("device_model")
    val deviceModel: String,
    
    @SerializedName("android_version")
    val androidVersion: String,
    
    @SerializedName("app_version")
    val appVersion: String,
    
    @SerializedName("fcm_token")
    val fcmToken: String? = null,
    
    @SerializedName("last_sign_in_at")
    val lastSignInAt: String? = null,
    
    @SerializedName("last_active_at")
    val lastActiveAt: String? = null
)

data class UpdateUserRequest(
    @SerializedName("last_active_at")
    val lastActiveAt: String? = null,
    
    @SerializedName("last_sign_in_at")
    val lastSignInAt: String? = null,
    
    @SerializedName("app_version")
    val appVersion: String? = null,
    
    @SerializedName("fcm_token")
    val fcmToken: String? = null,
    
    @SerializedName("display_name")
    val displayName: String? = null
)

data class UserResponse(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("firebase_uid")
    val firebaseUid: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("display_name")
    val displayName: String,
    
    @SerializedName("photo_url")
    val photoUrl: String? = null,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("updated_at")
    val updatedAt: String
)

data class ApiResponse<T>(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: T? = null,
    
    @SerializedName("errors")
    val errors: Map<String, List<String>>? = null
)

// ─── API Result Wrapper ────────────────────────────────────────

sealed class ApiResult<T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error<T>(val message: String, val code: Int? = null) : ApiResult<T>()
    class Loading<T> : ApiResult<T>()
}

