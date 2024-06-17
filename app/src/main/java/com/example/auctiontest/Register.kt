package com.example.auctiontest

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class Register : AppCompatActivity() {
    private lateinit var usernameEditText: EditText
    private lateinit var password1EditText: EditText
    private lateinit var password2EditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var errorTextView: TextView
    private lateinit var registerButton: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        usernameEditText = findViewById(R.id.username)
        password1EditText = findViewById(R.id.password1)
        password2EditText = findViewById(R.id.password2)
        registerButton = findViewById(R.id.register)
        emailEditText = findViewById(R.id.Email)
        phoneNumberEditText = findViewById(R.id.phoneNumber)
        errorTextView = findViewById(R.id.errorTextView)


        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password1 = password1EditText.text.toString()
            val password2 = password2EditText.text.toString()
            val email = emailEditText.text.toString()
            val phoneNumber = phoneNumberEditText.text.toString()

            // Make a network request in a background thread
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val response = registerUser(username, password1,password2,email,phoneNumber)
                    handleRegisterResponse(response)
                } catch (e: IOException) {
                    // Handle network error
                    e.printStackTrace()
                    runOnUiThread { showError("Network error") }
                }
            }
        }
    }
    private fun registerUser(username: String, password1: String,password2: String,email: String,phoneNumber: String): String {
        val client = OkHttpClient()

        val url = "http://192.168.0.104:5000/register_mobile"
        val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = JSONObject().apply {
            put("username", username)
            put("password1", password1)
            put("password2", password2)
            put("email_address", email)
            put("phone_number", phoneNumber)
        }.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }

    private fun handleRegisterResponse(response: String) {
        val jsonResponse = JSONObject(response)
        val success = jsonResponse.getBoolean("success")

        if (success) {

            // Move UI update to main thread
            runOnUiThread {
                Toast.makeText(applicationContext, "User created successfully", Toast.LENGTH_LONG).show()
                val intent = Intent(this@Register, Login::class.java)
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
        Toast.makeText(this@Register, message, Toast.LENGTH_LONG).show()
    }
}