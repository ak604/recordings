package com.optuze.recordings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.optuze.recordings.data.NetworkModule
import com.optuze.recordings.data.SessionManager
import com.optuze.recordings.data.api.AuthService
import com.optuze.recordings.data.models.GoogleVerifyRequest
import com.optuze.recordings.databinding.ActivityWelcomeBinding
import kotlinx.coroutines.launch
import retrofit2.HttpException

class WelcomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWelcomeBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var authService: AuthService
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "WelcomeActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SessionManager
        sessionManager = SessionManager(this)

        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNetworking()
        setupGoogleSignIn()
        
        binding.btnGoogleSignIn.setOnClickListener {
            startSignIn()
        }
    }

    private fun setupNetworking() {
        val okHttpClient = NetworkModule.createAuthenticatedClient(sessionManager)
        val retrofit = NetworkModule.createRetrofit(okHttpClient)
        authService = retrofit.create(AuthService::class.java)
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        
        // Optional: Sign out from any previous session
        googleSignInClient.signOut()
    }

    private fun startSignIn() {
        setLoading(true)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                handleSignInResult(account)
            } catch (e: ApiException) {
                Log.e(TAG, "Sign In Failed: ${e.statusCode}")
                showError("Sign In Failed: ${e.message}")
                setLoading(false)
            }
        }
    }

    private fun handleSignInResult(account: GoogleSignInAccount) {
        val idToken = account.idToken
        if (idToken != null) {
            Log.d(TAG, "Got ID Token: ${idToken.take(10)}...")
            verifyTokenWithBackend(idToken)
        } else {
            showError("No ID Token received")
            setLoading(false)
        }
    }

    private fun verifyTokenWithBackend(idToken: String) {
        lifecycleScope.launch {
            try {
                val request = GoogleVerifyRequest(
                    token = idToken,
                    packageId = packageName
                )
                
                Log.d(TAG, "Verifying token with backend...")
                val response = authService.verifyGoogleToken(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data?.let { authData ->
                        // Save auth data
                        sessionManager.saveAuthToken(authData.token)
                        sessionManager.saveUser(authData.user)
                        
                        // Navigate to main screen
                        navigateToMain()
                    } ?: run {
                        throw Exception("Invalid response data")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Backend verification failed: $errorBody")
                    throw Exception("Verification failed")
                }
            } catch (e: HttpException) {
                Log.e(TAG, "Network error: ${e.message}")
                showError("Network error: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}")
                showError("Error: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun setLoading(isLoading: Boolean) {
        runOnUiThread {
            binding.btnGoogleSignIn.isEnabled = !isLoading
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}
