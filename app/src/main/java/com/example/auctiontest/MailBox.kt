package com.example.auctiontest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class MailBox : AppCompatActivity() {

    private val mailData: MutableList<JSONObject> = mutableListOf()
    private var currentUser: String = ""
    private lateinit var mailUsername: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mail_box)

        val userId = intent.getStringExtra("id").toString()
        val user = intent.getStringExtra("username").toString()
        currentUser = user
        mailUsername = findViewById(R.id.mailUsername)
        mailUsername.text = "$currentUser's Mail Box"

        // Call the suspending function from a coroutine
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val response = getUserMails(userId)
                handleUserMailsResponse(response)
            } catch (e: IOException) {
                // Handle network error
                e.printStackTrace()
                // Show error on UI if necessary
            }
        }
    }

    private suspend fun getUserMails(id: String): String {
        val client = OkHttpClient()

        val url = "http://192.168.0.104:5000/mail_box_mobile"
        val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = JSONObject().apply {
            put("id", id)
        }.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response")
            }

            response.body?.string() ?: ""
        }
    }

    private suspend fun handleUserMailsResponse(response: String) {
        try {
            val jsonResponse = JSONObject(response)
            val success = jsonResponse.getBoolean("success")

            // Update UI elements on the main thread
            withContext(Dispatchers.Main) {
                // Clear existing content if any
                val parentLayout = findViewById<ConstraintLayout>(R.id.parentLayout)
                parentLayout.removeAllViews()

                if (success) {
                    val mails = jsonResponse.optJSONArray("mails")

                    if (mails != null) {
                        // Initialize a variable to keep track of the previously added mail layout
                        var previousMailLayout: ConstraintLayout? = null

                        // Loop through the mails array
                        for (i in 0 until mails.length()) {
                            val mail = mails.getJSONObject(i)
                            mailData.add(mail)
                            val subjectMail = mail.optString("subject", "")
                            val dateMail = mail.optString("date", "").substringBefore(",")
                            val senderMail = mail.optString("sender_username", "")
                            val sender = mail.optString("sender","")
                            val bigMessageSender = mail.optString("message", "")
                            val receiver = mail.optString("receiver","")
                            val mailId = mail.optString("id","")

                            // Inflate layout for mail
                            val inflater = LayoutInflater.from(this@MailBox)
                            val mailLayout = inflater.inflate(R.layout.mail_item_layout, null) as ConstraintLayout

                            // Set a unique ID for the mail item layout
                            mailLayout.id = View.generateViewId()
                            mailLayout.tag = mailId


                            val deleteButton = mailLayout.findViewById<Button>(R.id.deleteButton)
                            deleteButton.tag = mailId

                            // Populate mail layout with mail details
                            val subjectTextView = mailLayout.findViewById<TextView>(R.id.subject)
                            val dateTextView = mailLayout.findViewById<TextView>(R.id.date)
                            val senderTextView = mailLayout.findViewById<TextView>(R.id.sender)
                            val bigMessageTextView = mailLayout.findViewById<TextView>(R.id.bigMessage)
                            subjectTextView.text = "Subject: $subjectMail"
                            dateTextView.text = "Date: $dateMail"
                            senderTextView.text = "From: $senderMail"
                            bigMessageTextView.text = "Message: $bigMessageSender"

                            // Set constraints for the mail item layout
                            val layoutParams = ConstraintLayout.LayoutParams(
                                ConstraintLayout.LayoutParams.MATCH_PARENT,
                                ConstraintLayout.LayoutParams.WRAP_CONTENT
                            )
                            mailLayout.layoutParams = layoutParams

                            // Add mail layout to parent layout
                            parentLayout.addView(mailLayout)

                            // Set constraints for the mail item layout
                            val constraintSet = ConstraintSet()
                            constraintSet.clone(parentLayout)

                            if (i == 0) {
                                // For the first mail item, constrain it to the top of the parent layout
                                constraintSet.connect(
                                    mailLayout.id,
                                    ConstraintSet.TOP,
                                    parentLayout.id,
                                    ConstraintSet.TOP
                                )
                            } else {
                                // For subsequent mail items, constrain them below the previous mail item
                                constraintSet.connect(
                                    mailLayout.id,
                                    ConstraintSet.TOP,
                                    previousMailLayout!!.id,
                                    ConstraintSet.BOTTOM,
                                    resources.getDimensionPixelSize(R.dimen.mail_item_margin)
                                )
                            }

                            constraintSet.applyTo(parentLayout)

                            // Set the current mail layout as the previous mail layout for the next iteration
                            previousMailLayout = mailLayout
                        }
                    } else {
                        Log.e("MailBox", "No 'mails' array found in JSON response")
                    }
                } else {
                    val message = jsonResponse.optString("message", "")
                    // Handle error if needed
                }
            }
        } catch (e: JSONException) {
            Log.e("MailBox", "Error parsing JSON: ${e.message}")
        }
    }

    fun onDeleteButtonClick(view: View) {
        // Extract mail ID from the clicked mail item
        val mailLayout = view.parent as ConstraintLayout
        val mailId = mailLayout.tag as String // Use 'mailLayout.tag' directly

        // Call the function to delete mail passing mailId
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val response = deleteMail(mailId)
                handleDeleteMailResponse(response)
            } catch (e: IOException) {
                // Handle network error
                e.printStackTrace()
                // Show error on UI if necessary
            }
        }
    }

    private suspend fun deleteMail(mailId: String): String {
        val client = OkHttpClient()

        val url = "http://192.168.0.104:5000/delete_mail_mobile/$mailId" // Adjust the URL according to your Flask route
        val request = Request.Builder()
            .url(url)
            .delete()
            .build()

        return withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response")
            }

            response.body?.string() ?: ""
        }
    }


    private fun handleDeleteMailResponse(response: String) {
        val jsonResponse = JSONObject(response)
        val success = jsonResponse.getBoolean("success")

        if (success) {


            // Move UI update to main thread
            runOnUiThread {
                recreate()
            }
        } else {
            val message = jsonResponse.getString("message")
//            runOnUiThread { showError(message) }
        }
    }

    fun onReplyButtonClick(view: View) {
        val mailLayout = view.parent as ConstraintLayout
        val mailId = mailLayout.tag as String



        val mail = mailData.find { it.optString("id") == mailId }

        val senderId = mail?.optString("sender", "") ?: ""
        val receiverId = mail?.optString("receiver", "") ?: ""
        val senderUsername = mail?.optString("sender_username")
        val mailSubject = mail?.optString("subject")

        val dialogView = layoutInflater.inflate(R.layout.reply_mail, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Reply to $senderUsername")

        val dialog = dialogBuilder.create()
        dialog.show()

        val btnSend = dialogView.findViewById<Button>(R.id.btnSend)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val editSubject = dialogView.findViewById<EditText>(R.id.editSubject)
        val editMessage = dialogView.findViewById<EditText>(R.id.editMessage)

        editSubject.setText("Re:$mailSubject" ?: "")

        btnSend.setOnClickListener {
            val subject = editSubject.text.toString()
            val message = editMessage.text.toString()

            // Call the function to send reply mail passing the necessary data
            GlobalScope.launch(Dispatchers.Main) {
                try {
                    val response = sendReplyMail(subject, message,senderId,receiverId,currentUser)
                    handleSendReplyMailResponse(response)
                } catch (e: IOException) {
                    // Handle network error
                    e.printStackTrace()
                    // Show error on UI if necessary
                }
            }

            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    private suspend fun sendReplyMail(subject: String, message: String,senderId: String,receiverId: String,currentUser: String): String {
        val client = OkHttpClient()

        val url = "http://192.168.0.104:5000/send_mail_to_seller_mobile"
        val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = JSONObject().apply {
            put("sender_id", senderId)
            put("receiver_id", receiverId)
            put("subject", subject)
            put("message", message)
            put("username", currentUser)
        }.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response")
            }

            response.body?.string() ?: ""
        }
    }

    private fun handleSendReplyMailResponse(response: String) {
        val jsonResponse = JSONObject(response)
        val success = jsonResponse.getBoolean("success")

        if (success) {
            recreate()
        } else {
            val message = jsonResponse.optString("message", "")
            // Handle error message if needed
        }
    }



}