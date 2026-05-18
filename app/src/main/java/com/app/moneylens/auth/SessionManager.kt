package com.app.moneylens.auth

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

/**
 * SessionManager: Handle semua session data
 * - Menyimpan user info (Firebase UID, email, name, photo)
 * - Menyimpan device info (brand, model, Android version, app version)
 * - Cek apakah user sudah login
 */
class SessionManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("moneylens_session", Context.MODE_PRIVATE)

    // ─── Keys ────────────────────────────────────────────────────
    companion object {
        private const val KEY_FIREBASE_UID = "firebase_uid"
        private const val KEY_EMAIL = "email"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_PHOTO_URL = "photo_url"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_EMAIL_VERIFIED = "email_verified"
        private const val KEY_DEVICE_BRAND = "device_brand"
        private const val KEY_DEVICE_MODEL = "device_model"
        private const val KEY_ANDROID_VERSION = "android_version"
        private const val KEY_APP_VERSION = "app_version"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_LAST_SIGN_IN = "last_sign_in_at"
        private const val KEY_LAST_ACTIVE = "last_active_at"
        private const val KEY_SESSION_ID = "session_id"
    }

    // ─── User Data ──────────────────────────────────────────────
    fun saveUserData(
        firebaseUid: String,
        email: String,
        displayName: String,
        photoUrl: String?,
        provider: String = "google.com"
    ) {
        sharedPreferences.edit().apply {
            putString(KEY_FIREBASE_UID, firebaseUid)
            putString(KEY_EMAIL, email)
            putString(KEY_DISPLAY_NAME, displayName)
            putString(KEY_PHOTO_URL, photoUrl ?: "")
            putString(KEY_PROVIDER, provider)
            putString(KEY_EMAIL_VERIFIED, "true")
            putString(KEY_LAST_SIGN_IN, System.currentTimeMillis().toString())
            apply()
        }
        // Update device info otomatis saat sign in
        updateDeviceInfo()
    }

    fun getUserData(): UserData? {
        val firebaseUid = sharedPreferences.getString(KEY_FIREBASE_UID, null) ?: return null
        
        return UserData(
            firebaseUid = firebaseUid,
            email = sharedPreferences.getString(KEY_EMAIL, "") ?: "",
            displayName = sharedPreferences.getString(KEY_DISPLAY_NAME, "") ?: "",
            photoUrl = sharedPreferences.getString(KEY_PHOTO_URL, "") ?: "",
            provider = sharedPreferences.getString(KEY_PROVIDER, "google.com") ?: "google.com",
            emailVerified = sharedPreferences.getString(KEY_EMAIL_VERIFIED, "false") == "true"
        )
    }

    // ─── Device Info ────────────────────────────────────────────
    private fun updateDeviceInfo() {
        sharedPreferences.edit().apply {
            putString(KEY_DEVICE_BRAND, Build.BRAND)
            putString(KEY_DEVICE_MODEL, Build.MODEL)
            putString(KEY_ANDROID_VERSION, Build.VERSION.RELEASE)
            putString(KEY_APP_VERSION, getAppVersion())
            putString(KEY_LAST_ACTIVE, System.currentTimeMillis().toString())
            apply()
        }
    }

    fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            deviceBrand = sharedPreferences.getString(KEY_DEVICE_BRAND, Build.BRAND) ?: Build.BRAND,
            deviceModel = sharedPreferences.getString(KEY_DEVICE_MODEL, Build.MODEL) ?: Build.MODEL,
            androidVersion = sharedPreferences.getString(KEY_ANDROID_VERSION, Build.VERSION.RELEASE) ?: Build.VERSION.RELEASE,
            appVersion = sharedPreferences.getString(KEY_APP_VERSION, getAppVersion()) ?: getAppVersion()
        )
    }

    private fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    fun updateLastActive() {
        sharedPreferences.edit().apply {
            putString(KEY_LAST_ACTIVE, System.currentTimeMillis().toString())
            apply()
        }
    }

    fun setFcmToken(token: String) {
        sharedPreferences.edit().apply {
            putString(KEY_FCM_TOKEN, token)
            apply()
        }
    }

    fun getFcmToken(): String? = sharedPreferences.getString(KEY_FCM_TOKEN, null)

    // ─── Session Check ──────────────────────────────────────────
    fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getString(KEY_FIREBASE_UID, null) != null
    }

    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }

    fun getSessionData(): SessionData {
        return SessionData(
            user = getUserData(),
            device = getDeviceInfo(),
            isLoggedIn = isUserLoggedIn()
        )
    }
}

// ─── Data Classes ───────────────────────────────────────────────

data class UserData(
    val firebaseUid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String,
    val provider: String,
    val emailVerified: Boolean
)

data class DeviceInfo(
    val deviceBrand: String,
    val deviceModel: String,
    val androidVersion: String,
    val appVersion: String
)

data class SessionData(
    val user: UserData?,
    val device: DeviceInfo,
    val isLoggedIn: Boolean
)

