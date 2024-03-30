package com.example.auctiontest

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class Login : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var errorTextView: TextView
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.loginBtn)
        errorTextView = findViewById(R.id.errorTextView)
        registerButton = findViewById(R.id.registerBtn)


        registerButton.setOnClickListener {
            val intent = Intent(this@Login, Register::class.java)
            startActivity(intent)
        }



        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            // Make a network request in a background thread
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val response = loginUser(username, password)
                    handleLoginResponse(response)
                } catch (e: IOException) {
                    // Handle network error
                    e.printStackTrace()
                    runOnUiThread { showError("Network error") }
                }
            }
        }
    }

    private fun loginUser(username: String, password: String): String {
        val client = OkHttpClient()

        val url = "http://192.168.0.104:5000/login_mobile"
        val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = JSONObject().apply {
            put("username", username)
            put("password", password)
        }.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }

    private fun handleLoginResponse(response: String) {
        val jsonResponse = JSONObject(response)
        val success = jsonResponse.getBoolean("success")

        if (success) {
            // Login successful, proceed with your app logic
            val user = jsonResponse.getJSONObject("user")
            val id = user.getString("id")

            // Move UI update to main thread
            runOnUiThread {
                val intent = Intent(this@Login, MainActivity::class.java)
                intent.putExtra("id", id)
                startActivity(intent)
            }
        } else {
            // Login failed, show error message
            val message = jsonResponse.getString("message")
            runOnUiThread { showError(message) }
        }
    }

    private fun showError(message: String) {
        // Display error message to the user (e.g., in a TextView)
        errorTextView.text = message
    }
}