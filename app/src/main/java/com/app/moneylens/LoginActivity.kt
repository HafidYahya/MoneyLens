package com.app.moneylens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.app.moneylens.api.ApiResult
import com.app.moneylens.api.RetrofitClient
import com.app.moneylens.auth.AuthRepository
import com.app.moneylens.auth.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

/**
 * LoginActivity: Google Sign-In dengan Firebase
 * - User login dengan akun Google
 * - Simpan session (tidak perlu login lagi sampai uninstall/clear data)
 * - Sync user ke database MySQL via API
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var sessionManager: SessionManager
    private lateinit var authRepository: AuthRepository

    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView

    private val RC_SIGN_IN = 9001

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Initialize Session Manager
        sessionManager = SessionManager(this)

        // Initialize Auth Repository
        authRepository = AuthRepository(sessionManager, RetrofitClient.getApiService())

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Setup UI
        loginButton = findViewById(R.id.loginButton)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)

        loginButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        loginButton.isEnabled = false
        progressBar.visibility = android.view.View.VISIBLE
        statusText.text = "Sedang sign in dengan Google..."

        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)

                // Sign in ke Firebase dengan Google credential
                firebaseSignInWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                statusText.text = "Sign in gagal: ${e.message}"
                loginButton.isEnabled = true
                progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Sign in cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseSignInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase sign in successful")
                    val firebaseUser = firebaseAuth.currentUser
                    if (firebaseUser != null) {
                        // Save user data to SharedPreferences
                        sessionManager.saveUserData(
                            firebaseUid = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            displayName = firebaseUser.displayName ?: "User",
                            photoUrl = firebaseUser.photoUrl?.toString()
                        )

                        // Sync ke API database
                        syncUserToDatabase()
                    }
                } else {
                    Log.w(TAG, "Firebase sign in failed", task.exception)
                    statusText.text = "Firebase sign in gagal"
                    loginButton.isEnabled = true
                    progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Firebase auth failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun syncUserToDatabase() {
        statusText.text = "Menyimpan data ke server..."
        
        lifecycleScope.launch {
            try {
                val deviceInfo = sessionManager.getDeviceInfo()
                val result = authRepository.syncUserToApi(deviceInfo)

                when (result) {
                    is ApiResult.Success -> {
                        Log.d(TAG, "User synced to database successfully")
                        statusText.text = "Login berhasil! Membuka aplikasi..."
                        
                        // Go to MainActivity
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                    is ApiResult.Error -> {
                        Log.e(TAG, "API error: ${result.message}")
                        statusText.text = "Error sync database: ${result.message}"
                        loginButton.isEnabled = true
                        progressBar.visibility = android.view.View.GONE
                        Toast.makeText(
                            this@LoginActivity,
                            "Gagal sync database: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is ApiResult.Loading -> {
                        // Loading state
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during sync", e)
                statusText.text = "Error: ${e.message}"
                loginButton.isEnabled = true
                progressBar.visibility = android.view.View.GONE
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

