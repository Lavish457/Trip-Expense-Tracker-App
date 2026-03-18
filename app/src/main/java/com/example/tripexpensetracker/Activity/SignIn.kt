package com.example.expensetrackerapp.Activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.tripexpensetracker.R
import com.example.expensetrackerapp.RetrofitClient
import kotlinx.coroutines.*

class SignIn : AppCompatActivity() {

    private val API_KEY1 = "\$2a\$10$"
    private val API_KEY2 = "hPDzuJOstFCGQJp/WyXF/OCUkVjzUbrXHE1W6CMVm4jMb.MXdAz92"
    private val API_KEY = API_KEY1 + API_KEY2

    private val NORMAL_BIN_ID = "69171b69d0ea881f40e7f4cd"
    private val TEMP_BIN_ID = "691c6c2643b1c97be9b51ab3"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isLoggedIn()) {
            startActivity(Intent(this, Home::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_sign_in)

        findViewById<TextView>(R.id.txtSignUp).setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }

        findViewById<AppCompatButton>(R.id.btn).setOnClickListener {
            val email = findViewById<EditText>(R.id.edtEmailSignIn).text.toString().trim()
            val password = findViewById<EditText>(R.id.edtPasswordSignIn).text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }
    }

    fun Context.showLoadingDialog(message: String = "Please wait..."): AlertDialog {
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(60, 40, 60, 40)
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val progressBar = ProgressBar(this).apply {
            isIndeterminate = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = 32 }
        }

        val tv = TextView(this).apply {
            text = message
            textSize = 18f
            setTextColor(Color.BLACK)
        }

        ll.addView(progressBar)
        ll.addView(tv)

        return AlertDialog.Builder(this)
            .setCancelable(false)
            .setView(ll)
            .create()
            .apply { show() }
    }

    private fun loginUser(email: String, password: String) {
        val loadingDialog = showLoadingDialog("Logging in...")

        CoroutineScope(Dispatchers.IO).launch {
            val startTime = System.currentTimeMillis()

            try {
                val isTestAccount = email.lowercase().contains("test")
                Log.d("LoginDebug", "Email: $email → isTestAccount = $isTestAccount")

                val binIdToUse = if (isTestAccount) TEMP_BIN_ID else NORMAL_BIN_ID
                Log.d("LoginDebug", "Using bin ID: $binIdToUse")

                val response = if (isTestAccount) {
                    Log.d("LoginDebug", "Calling getTempUser()")
                    RetrofitClient.instance.getTempUser(binIdToUse, API_KEY)
                } else {
                    Log.d("LoginDebug", "Calling getUsers()")
                    RetrofitClient.instance.getUsers(binIdToUse, API_KEY)
                }

                val usersList = response.record.users
                Log.d("LoginDebug", "Fetched ${usersList.size} users from bin")

                usersList.forEachIndexed { index, user ->
                    Log.d("LoginDebug", "User $index: ${user.email} / memberID: ${user.memberID}")
                }

                val user = usersList.find {
                    val match = it.email.equals(email, ignoreCase = true) && it.password == password
                    Log.d("LoginDebug", "Checking ${it.email} → match = $match")
                    match
                }

                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 2000) delay(2000 - elapsed)

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()

                    if (user != null) {
                        // ── Save basic login state ────────────────────────────────────
                        saveLoginState(email)

                        // ── Save memberId so HomeFragment / fragments can filter data
                        //    by the logged-in user.
                        //    FIX: this was never saved for test accounts, causing
                        //    memberId to stay -1 and HomeFragment to find 0 trips.
                        // ─────────────────────────────────────────────────────────────
                        getSharedPreferences("memberId", MODE_PRIVATE).edit()
                            .putLong("memberId", user.memberID)
                            .apply()
                        Log.d("LoginDebug", "Saved memberId = ${user.memberID}")

                        // ── Extra flags for test accounts ─────────────────────────────
                        if (isTestAccount) {
                            getSharedPreferences("auth", MODE_PRIVATE).edit().apply {
                                putBoolean("isTempUser", true)
                                putString("tempEmail", email)
                                apply()
                            }
                            Log.d("LoginDebug", "Saved temp user flags → isTempUser=true, email=$email")
                        } else {
                            // Clear any stale temp-user flags from a previous test login
                            getSharedPreferences("auth", MODE_PRIVATE).edit().apply {
                                putBoolean("isTempUser", false)
                                remove("tempEmail")
                                apply()
                            }
                            Log.d("LoginDebug", "Normal user login → cleared temp user flags")
                        }

                        Toast.makeText(this@SignIn, "Welcome back, ${user.name}!", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@SignIn, Home::class.java))
                        finish()
                    } else {
                        val msg = if (isTestAccount) "Invalid test email or password"
                        else "Invalid email or password"
                        Toast.makeText(this@SignIn, msg, Toast.LENGTH_SHORT).show()
                        Log.w("LoginDebug", "No matching user found for email: $email")
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginDebug", "Login failed", e)
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 2000) delay(2000 - elapsed)

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@SignIn, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveLoginState(email: String) {
        getSharedPreferences("auth", MODE_PRIVATE).edit()
            .putBoolean("isLoggedIn", true)
            .putString("email", email)
            .apply()
    }

    private fun isLoggedIn(): Boolean {
        return getSharedPreferences("auth", MODE_PRIVATE)
            .getBoolean("isLoggedIn", false)
    }
}