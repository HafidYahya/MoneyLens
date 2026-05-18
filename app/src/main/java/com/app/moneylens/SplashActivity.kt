package com.app.moneylens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.app.moneylens.auth.SessionManager

/**
 * SplashActivity: Entry point aplikasi
 * - Cek apakah user sudah login
 * - Jika sudah: buka MainActivity langsung
 * - Jika belum: buka LoginActivity
 * 
 * Timeline: ~2 detik splash screen
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    companion object {
        private const val TAG = "SplashActivity"
        private const val SPLASH_DELAY = 2000L  // 2 seconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        sessionManager = SessionManager(this)

        // Check session setelah splash delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            checkSession()
        }, SPLASH_DELAY)
    }

    private fun checkSession() {
        val isLoggedIn = sessionManager.isUserLoggedIn()
        Log.d(TAG, "Session check: isLoggedIn = $isLoggedIn")

        if (isLoggedIn) {
            // Update last active time
            sessionManager.updateLastActive()
            
            // Go to MainActivity
            Log.d(TAG, "User already logged in, going to MainActivity")
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // Go to LoginActivity
            Log.d(TAG, "User not logged in, going to LoginActivity")
            startActivity(Intent(this, LoginActivity::class.java))
        }

        finish()
    }
}

