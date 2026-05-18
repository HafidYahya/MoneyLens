package com.app.moneylens.auth

import android.util.Log
import com.app.moneylens.api.ApiResult
import com.app.moneylens.api.CreateUserRequest
import com.app.moneylens.api.MoneyLensApiService
import com.app.moneylens.api.UserResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * AuthRepository: Handle authentication logic
 * - Google Sign In via Firebase
 * - Save user to database via API
 * - Manage session
 */
class AuthRepository(
    private val sessionManager: SessionManager,
    private val apiService: MoneyLensApiService
) {

    private val firebaseAuth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "AuthRepository"
    }

    // ─── Get current user ──────────────────────────────────────────
    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    // ─── Sync user ke API database ─────────────────────────────────
    suspend fun syncUserToApi(deviceInfo: DeviceInfo): ApiResult<UserResponse> {
        val firebaseUser = getCurrentUser() ?: return ApiResult.Error("User not signed in")

        val request = CreateUserRequest(
            firebaseUid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: "Anonymous",
            photoUrl = firebaseUser.photoUrl?.toString(),
            deviceBrand = deviceInfo.deviceBrand,
            deviceModel = deviceInfo.deviceModel,
            androidVersion = deviceInfo.androidVersion,
            appVersion = deviceInfo.appVersion,
            lastSignInAt = getCurrentTimestamp(),
            lastActiveAt = getCurrentTimestamp()
        )

        return suspendCancellableCoroutine { continuation ->
            try {
                apiService.createUser(request).enqueue(
                    object : Callback<com.app.moneylens.api.ApiResponse<UserResponse>> {
                        override fun onResponse(
                            call: Call<com.app.moneylens.api.ApiResponse<UserResponse>>,
                            response: Response<com.app.moneylens.api.ApiResponse<UserResponse>>
                        ) {
                            if (response.isSuccessful) {
                                val apiResponse = response.body()
                                if (apiResponse?.data != null) {
                                    continuation.resume(ApiResult.Success(apiResponse.data))
                                    Log.d(TAG, "User synced successfully")
                                } else {
                                    val error = apiResponse?.message ?: "Empty data"
                                    continuation.resume(ApiResult.Error(error))
                                    Log.w(TAG, "API response with no data: $error")
                                }
                            } else {
                                val message = response.message() ?: "API Error"
                                continuation.resume(ApiResult.Error(message, response.code()))
                                Log.e(TAG, "API Error: ${response.code()} - $message")
                            }
                        }

                        override fun onFailure(
                            call: Call<com.app.moneylens.api.ApiResponse<UserResponse>>,
                            t: Throwable
                        ) {
                            continuation.resume(ApiResult.Error(t.message ?: "Network error"))
                            Log.e(TAG, "Network error: ${t.message}", t)
                        }
                    }
                )
            } catch (e: Exception) {
                continuation.resumeWithException(e)
                Log.e(TAG, "Exception in sync: ${e.message}", e)
            }
        }
    }

    private fun getCurrentTimestamp(): String {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return formatter.format(java.util.Date())
    }

    // ─── Sign out ──────────────────────────────────────────────────
    fun signOut() {
        firebaseAuth.signOut()
        sessionManager.clearSession()
        Log.d(TAG, "User signed out")
    }
}

