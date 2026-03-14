package com.example.expensetrackerapp.Activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
import kotlin.random.Random

class SignIn : AppCompatActivity() {

    private val API_KEY1 = "\$2a\$10$"
    private val API_KEY2 = "hPDzuJOstFCGQJp/WyXF/OCUkVjzUbrXHE1W6CMVm4jMb.MXdAz92"
    private val API_KEY = API_KEY1 + API_KEY2
    private val BIN_ID = "69171b69d0ea881f40e7f4cd"

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
            ).apply {
                marginEnd = 32 // space between spinner and text
            }
        }

        val tv = TextView(this).apply {
            text = message
            textSize = 18f
            setTextColor(Color.BLACK)
        }

        ll.addView(progressBar)
        ll.addView(tv)

        return AlertDialog.Builder(this)
            .setCancelable(false)           // user can't dismiss by touching outside / back
            .setView(ll)
            .create()
            .apply { show() }
    }

    private fun loginUser(email: String, password: String) {
        val loadingDialog = showLoadingDialog("Logging in...")

        CoroutineScope(Dispatchers.IO).launch {
            // Minimum display time = 2 seconds
            val startTime = System.currentTimeMillis()

            try {
                val response = RetrofitClient.instance.getUsers(BIN_ID, API_KEY)
                val user = response.record.users.find {
                    it.email.equals(email, ignoreCase = true) && it.password == password
                }

                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 2000) {
                    delay(2000 - elapsed)   // wait until at least 2 seconds passed
                }

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()

                    if (user != null) {
                        saveLoginState(email)
                        Toast.makeText(this@SignIn, "Welcome back, ${user.name}!", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@SignIn, Home::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@SignIn, "Invalid email or password", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
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