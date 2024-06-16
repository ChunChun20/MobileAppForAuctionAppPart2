package com.example.auctiontest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode

class UserProfile : AppCompatActivity() {

    private val reviewData: MutableList<JSONObject> = mutableListOf()
    private lateinit var nameView: TextView
    private lateinit var emailView: TextView
    private lateinit var phone: TextView
    private lateinit var positive: TextView
    private lateinit var negative: TextView

    private var currentUserId: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        nameView = findViewById(R.id.nameView)
        emailView = findViewById(R.id.EmailView)
        phone = findViewById(R.id.Phone)
        positive = findViewById(R.id.positive)
        negative = findViewById(R.id.negative)

        val userId = intent.getStringExtra("id").toString()
        currentUserId = userId

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = getUserProfile(userId)
                handleUserProfileResponse(response)
            } catch (e: IOException) {
                // Handle network error
                e.printStackTrace()
//                runOnUiThread { showError("Network error") }
            }
        }


    }
    private fun getUserProfile(id: String): String {
        val client = OkHttpClient()

        val url = "http://192.168.0.104:5000/get_user_profile"
        val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = JSONObject().apply {
            put("id", id)
        }.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }

    private suspend fun handleUserProfileResponse(response: String) {
        val jsonResponse = JSONObject(response)
        val success = jsonResponse.getBoolean("success")

        withContext(Dispatchers.Main) {
            // Clear existing content if any
            val parentLayout = findViewById<ConstraintLayout>(R.id.parentLayout)
            parentLayout.removeAllViews()

            if (success) {
                val user = jsonResponse.getJSONObject("user")
                val reviews = jsonResponse.optJSONArray("reviews")
                val username = user.getString("username")
                val userEmail = user.getString("email")
                val userPhone = user.getString("phone")
                val userPositive = user.getString("positive")
                val userNegative = user.getString("negative")

                // Move UI update to main thread
                runOnUiThread {
                    nameView.text = username
                    emailView.text = "Email: $userEmail"
                    phone.text = "Phone: $userPhone"
                    positive.text = "Positive: $userPositive"
                    negative.text = "Negative: $userNegative"

                }
                if (reviews != null) {
                    var previousReviewLayout: ConstraintLayout? = null

                    for (i in 0 until reviews.length()) {
                        val review = reviews.getJSONObject(i)
                        reviewData.add(review)
                        val reviewer = review.optString("reviewer_name", "")
                        val reviewType = review.optString("review_type", "")
                        val reviewMessage = review.optString("message", "")
                        val reviewId = review.optString("id", "")

                        // Inflate layout for review
                        val inflater = LayoutInflater.from(this@UserProfile)
                        val reviewLayout = inflater.inflate(R.layout.review_layout, null) as ConstraintLayout

                        // Set a unique ID for the review item layout
                        reviewLayout.id = View.generateViewId()
                        reviewLayout.tag = reviewId

                        val reviewerTextView = reviewLayout.findViewById<TextView>(R.id.reviewUser)
                        val reviewMessageTextView = reviewLayout.findViewById<TextView>(R.id.reviewMessage)

                        reviewerTextView.text = "From: $reviewer"
                        reviewMessageTextView.text = "Review: $reviewMessage"

                        if (reviewType.equals("Positive", ignoreCase = true)) {
                            reviewMessageTextView.setBackgroundResource(R.drawable.custom_edittext_green)
                            reviewMessageTextView.setTextColor(ContextCompat.getColor(this@UserProfile, R.color.black))

                        } else if (reviewType.equals("Negative", ignoreCase = true)) {
                            reviewMessageTextView.setBackgroundResource(R.drawable.custom_edittext_red)
                            reviewMessageTextView.setTextColor(ContextCompat.getColor(this@UserProfile, R.color.white))
                        }


                        val layoutParams = ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.MATCH_PARENT,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT
                        )
                        reviewLayout.layoutParams = layoutParams

                        // Add review layout to parent layout
                        parentLayout.addView(reviewLayout)

                        // Set constraints for the review item layout
                        val constraintSet = ConstraintSet()
                        constraintSet.clone(parentLayout)

                        if (i == 0) {
                            // For the first review item, constrain it to the top of the parent layout
                            constraintSet.connect(
                                reviewLayout.id,
                                ConstraintSet.TOP,
                                parentLayout.id,
                                ConstraintSet.TOP
                            )
                        } else {
                            // For subsequent review items, constrain them below the previous review item
                            constraintSet.connect(
                                reviewLayout.id,
                                ConstraintSet.TOP,
                                previousReviewLayout!!.id,
                                ConstraintSet.BOTTOM,
                                resources.getDimensionPixelSize(R.dimen.mail_item_margin)
                            )
                        }

                        constraintSet.applyTo(parentLayout)

                        previousReviewLayout = reviewLayout
                    }
                } else {
                    Log.e("Reviews", "No 'reviews' array found in JSON response")
                }
            } else {
                val message = jsonResponse.getString("message")
//            runOnUiThread { showError(message) }
            }
        }
    }

}