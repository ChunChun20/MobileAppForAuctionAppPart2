package com.example.auctiontest

import android.graphics.BitmapFactory
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

class OwnedItems : AppCompatActivity() {

    private var currentPage = 1
    private val perPage = 5
    private lateinit var userId: String

    private var currentUser: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_owned_items)

        userId = intent.getStringExtra("id") ?: ""
        currentUser = intent.getStringExtra("username") ?: ""

        fetchData(currentPage)

        val nextPageButton = findViewById<Button>(R.id.nextPageButton)
        val prevPageButton = findViewById<Button>(R.id.prevPageButton)

        nextPageButton.setOnClickListener {
            currentPage++
            fetchData(currentPage)

            // Update button texts with the current page numbers
            nextPageButton.text = "Page ${currentPage + 1}"
            prevPageButton.text = "Page ${currentPage - 1}"

            // Show prevPageButton when currentPage > 1
            if (currentPage > 1) {
                prevPageButton.visibility = View.VISIBLE
            }
        }

        prevPageButton.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                fetchData(currentPage)
            }

            // Update button texts with the current page numbers
            nextPageButton.text = "Page ${currentPage + 1}"
            prevPageButton.text = "Page ${currentPage - 1}"

            // Hide prevPageButton if currentPage is 1
            if (currentPage == 1) {
                prevPageButton.visibility = View.GONE
            }
        }

        // Hide prevPageButton if currentPage is 1 initially
        if (currentPage == 1) {
            prevPageButton.visibility = View.GONE
        }
    }

    private fun fetchData(page: Int){
        val loadingLayout = findViewById<FrameLayout>(R.id.loadingLayout)
        loadingLayout.visibility = View.VISIBLE
        FetchData().execute(page)
    }

    inner class FetchData : AsyncTask<Int, Void, Triple<List<RecentlySoldItem>?, String?, Int>>() {
        override fun doInBackground(vararg params: Int?): Triple<List<RecentlySoldItem>?, String?, Int> {
            val page = params[0] ?: 1
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("http://192.168.0.104:5000/owned_items_mobile/$userId?page=$page")
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val items = parseData(responseBody)
                    val jsonObject = JSONObject(responseBody)
                    val totalPages = jsonObject.getInt("total_pages")
                    return Triple(items, null, totalPages)
                } else {
                    return Triple(null, "Failed to fetch data: ${response.code}", 0)
                }
            } catch (e: Exception) {
                return Triple(null, "Exception: ${e.message}", 0)
            }
        }

        override fun onPostExecute(result: Triple<List<RecentlySoldItem>?, String?, Int>) {
            super.onPostExecute(result)
            val loadingLayout = findViewById<FrameLayout>(R.id.loadingLayout)
            loadingLayout.visibility = View.GONE
            result.first?.let { items ->
                val inflater = LayoutInflater.from(this@OwnedItems)
                val itemContainer = findViewById<LinearLayout>(R.id.parentLayout)

                // Clear existing views
                itemContainer.removeAllViews()

                val nextPageButton = findViewById<Button>(R.id.nextPageButton)
                val prevPageButton = findViewById<Button>(R.id.prevPageButton)

                // Update button texts with the current page numbers
                nextPageButton.text = "Page ${currentPage + 1}"
                prevPageButton.text = "Page ${currentPage - 1}"

                nextPageButton.visibility = if (currentPage == result.third) View.GONE else View.VISIBLE

                for (item in items) {
                    val itemView = inflater.inflate(R.layout.owned_item_layout, null)

                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.setMargins(0, 8, 0, 16)

                    itemView.layoutParams = layoutParams

                    val nameView = itemView.findViewById<TextView>(R.id.nameView)
                    val dateView = itemView.findViewById<TextView>(R.id.dateView)
                    val priceView = itemView.findViewById<TextView>(R.id.priceView)
                    val categoryView = itemView.findViewById<TextView>(R.id.categoryView)
                    val infoButton = itemView.findViewById<Button>(R.id.infoButton)
                    val deleteButton = itemView.findViewById<Button>(R.id.deleteButton)
                    val reviewButton = itemView.findViewById<Button>(R.id.reviewButton)
                    deleteButton.tag = item.id

                    val RecentlySoldItemName = item.name.substring(0, item.name.length - 6)
                    val RecentlySoldItemDate = item.date.substringBefore(",")
                    val RecentlySoldItemPrice = item.price
                    val RecentlySoldItemCategory = item.category

                    nameView.text = "Name: $RecentlySoldItemName"
                    dateView.text = "Date: $RecentlySoldItemDate"
                    priceView.text = "Bought for: $RecentlySoldItemPrice$"
                    categoryView.text = "Category: $RecentlySoldItemCategory"


                    reviewButton.setOnClickListener {


                        val seller = item.seller
                        val reviewer_id = userId
                        val reviewer_name = currentUser


                        val dialogView = layoutInflater.inflate(R.layout.leave_review, null)
                        val dialogBuilder = AlertDialog.Builder(this@OwnedItems)
                            .setView(dialogView)
                            .setTitle("Add review to owner of ${item.name.substring(0, item.name.length - 6)}")

                        val dialog = dialogBuilder.create()
                        dialog.show()

                        val btnSend = dialogView.findViewById<Button>(R.id.btnSend)
                        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
                        val reviewType = dialogView.findViewById<Spinner>(R.id.reviewType)
                        val editMessage = dialogView.findViewById<EditText>(R.id.editMessage)



                        btnSend.setOnClickListener {
                            val review = reviewType.selectedItem.toString()
                            val message = editMessage.text.toString()


                            GlobalScope.launch(Dispatchers.Main) {
                                try {
                                    val response = sendReview(review, message,seller,reviewer_id,reviewer_name,item.name)
                                    handleSendReplyReviewResponse(response)
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



                    infoButton.setOnClickListener {
                        val dialogView = LayoutInflater.from(this@OwnedItems)
                            .inflate(R.layout.recently_sold_info, null)
                        val dialogBuilder = AlertDialog.Builder(this@OwnedItems)
                            .setView(dialogView)
                            .setTitle("Item: $RecentlySoldItemName")

                        val recentlySoldDescriptionEditText =
                            dialogView.findViewById<TextView>(R.id.recentlySoldDescription)
                        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)
                        val itemImageView = dialogView.findViewById<ImageView>(R.id.recentlySoldItemImage)
                        val switchViewButton = dialogView.findViewById<Button>(R.id.recentlySoldBtnSwitchView)

                        val bitmap = BitmapFactory.decodeByteArray(item.imageData, 0, item.imageData.size)

                        itemImageView.setImageBitmap(bitmap)

                        recentlySoldDescriptionEditText.setText("Description: ${item.description}")

                        val alertDialog = dialogBuilder.create()

                        switchViewButton.setOnClickListener {
                            // Toggle visibility of description and image views
                            if (recentlySoldDescriptionEditText.visibility == View.VISIBLE) {
                                recentlySoldDescriptionEditText.visibility = View.GONE
                                itemImageView.visibility = View.VISIBLE
                                switchViewButton.text = "<-"
                            } else {
                                recentlySoldDescriptionEditText.visibility = View.VISIBLE
                                itemImageView.visibility = View.GONE
                                switchViewButton.text = "->"
                            }
                        }

                        btnClose.setOnClickListener {
                            alertDialog.dismiss()
                        }

                        alertDialog.show()
                    }

                    deleteButton.setOnClickListener {
                        val itemId = it.tag as String
                        val url = "http://192.168.0.104:5000/delete_item_mobile/$itemId"

                        AsyncTask.execute {
                            val client = OkHttpClient()
                            val request = Request.Builder()
                                .url(url)
                                .delete()
                                .build()

                            try {
                                val response = client.newCall(request).execute()
                                if (response.isSuccessful) {
                                    runOnUiThread {
                                        Toast.makeText(applicationContext, "Item deleted successfully", Toast.LENGTH_LONG).show()
                                        recreate()
                                    }
                                } else {
                                    runOnUiThread {
                                        Toast.makeText(applicationContext, "Failed to delete item", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } catch (e: Exception) {
                                runOnUiThread {
                                    Toast.makeText(applicationContext, "Exception occurred: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }

                    itemContainer.addView(itemView)
                }
            }
            result.second?.let { errorMessage ->
                Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        private suspend fun sendReview(review: String, message: String,seller: Int,reviewer_id: String,reviewer_name: String,item_name: String): String {
            val client = OkHttpClient()

            val url = "http://192.168.0.104:5000/leave_review_mobile"
            val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = JSONObject().apply {
                put("seller_id", seller)
                put("review_type", review)
                put("reviewer_id", reviewer_id)
                put("review_message", message)
                put("reviewer_name", reviewer_name)
                put("item_name",item_name)
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

        private fun handleSendReplyReviewResponse(response: String) {
            val jsonResponse = JSONObject(response)
            val success = jsonResponse.getBoolean("success")

            if (success) {
                Toast.makeText(this@OwnedItems, "Successfuly placed review!", Toast.LENGTH_LONG).show()
                recreate()
            } else {
                val message = jsonResponse.optString("message", "")
                Toast.makeText(this@OwnedItems, message, Toast.LENGTH_LONG).show()

            }
        }

        private fun parseData(data: String?): List<RecentlySoldItem>? {
            data ?: return null
            val jsonObject = JSONObject(data)
            val jsonArray = jsonObject.getJSONArray("owned_items")
            val items = mutableListOf<RecentlySoldItem>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val category = item.getString("category")
                val date = item.getString("end")
                val name = item.getString("name")
                val price = item.getString("bid")
                val description = item.getString("description")
                val seller = item.getInt("seller")
                val itemId = item.getString("id")
                val imageBase64 = item.getString("image")

                val imageData = Base64.decode(imageBase64, Base64.DEFAULT)
                items.add(RecentlySoldItem(category, name, date, price, description,seller, itemId, imageData))
            }
            return items
        }
    }
}
