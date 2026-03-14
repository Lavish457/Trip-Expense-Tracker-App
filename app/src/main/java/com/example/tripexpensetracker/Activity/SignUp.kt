package com.example.expensetrackerapp.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
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
import com.example.expensetrackerapp.KT_DataClass.User
import com.example.expensetrackerapp.KT_DataClass.UserListWrapper
import com.example.tripexpensetracker.R
import com.example.expensetrackerapp.RetrofitClient
import kotlinx.coroutines.*
import kotlin.random.Random

class SignUp : AppCompatActivity() {

    private val API_KEY1 = "\$2a\$10$"
    private val API_KEY2 = "hPDzuJOstFCGQJp/WyXF/OCUkVjzUbrXHE1W6CMVm4jMb.MXdAz92"
    private val API_KEY = API_KEY1 + API_KEY2
    private val BIN_ID = "69171b69d0ea881f40e7f4cd"

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
    private fun isLoggedIn(): Boolean {
        return getSharedPreferences("auth", MODE_PRIVATE)
            .getBoolean("isLoggedIn", false)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isLoggedIn()) {
            startActivity(Intent(this, Home::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_sign_up)

        findViewById<TextView>(R.id.txtLogin).setOnClickListener {
            startActivity(Intent(this, SignIn::class.java))
            finish()
        }

        findViewById<AppCompatButton>(R.id.btn).setOnClickListener {
            val name = findViewById<EditText>(R.id.edtName).text.toString().trim()
            val email = findViewById<EditText>(R.id.edtEmail).text.toString().trim()
            val password = findViewById<EditText>(R.id.edtPassword).text.toString().trim()
            val memberId = Random.nextLong(10000000L, 99999999L)
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be 6+ characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            signUpUser(name, email, password)
        }
    }

    private fun signUpUser(name: String, email: String, password: String) {
        val loadingDialog = showLoadingDialog("Creating account...")

        CoroutineScope(Dispatchers.IO).launch {
            val startTime = System.currentTimeMillis()

            try {
                val response = RetrofitClient.instance.getUsers(BIN_ID, API_KEY)
                val currentUsers = response.record.users.toMutableList()

                if (currentUsers.any { it.email.equals(email, ignoreCase = true) }) {
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed < 2000) delay(2000 - elapsed)

                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()
                        Toast.makeText(this@SignUp, "Email already exists!", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val memberId = Random.nextLong(10000000L, 99999999L)
                currentUsers.add(User(name, email, password, memberId))

                getSharedPreferences("memberId", MODE_PRIVATE).edit()
                    .putLong("memberId", memberId)
                    .apply()

                val wrapper = UserListWrapper(currentUsers)
                RetrofitClient.instance.updateUsers(BIN_ID, API_KEY, wrapper = wrapper)

                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 2000) delay(2000 - elapsed)

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@SignUp, "Sign Up Successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SignUp, SignIn::class.java))
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 2000) delay(2000 - elapsed)

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@SignUp, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}