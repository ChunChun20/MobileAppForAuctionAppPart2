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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class OwnedItems : AppCompatActivity() {

    private var currentPage = 1
    private val perPage = 5
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_owned_items)

        userId = intent.getStringExtra("id") ?: ""

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
                    deleteButton.tag = item.id

                    val RecentlySoldItemName = item.name.substring(0, item.name.length - 6)
                    val RecentlySoldItemDate = item.date.substringBefore(",")
                    val RecentlySoldItemPrice = item.price
                    val RecentlySoldItemCategory = item.category

                    nameView.text = "Name: $RecentlySoldItemName"
                    dateView.text = "Date: $RecentlySoldItemDate"
                    priceView.text = "Sold for: $RecentlySoldItemPrice$"
                    categoryView.text = "Category: $RecentlySoldItemCategory"

                    infoButton.setOnClickListener {
                        val dialogView = LayoutInflater.from(this@OwnedItems)
                            .inflate(R.layout.recently_sold_info, null)
                        val dialogBuilder = AlertDialog.Builder(this@OwnedItems)
                            .setView(dialogView)
                            .setTitle("$RecentlySoldItemName")

                        val recentlySoldDescriptionEditText =
                            dialogView.findViewById<TextView>(R.id.recentlySoldDescription)
                        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)
                        val itemImageView = dialogView.findViewById<ImageView>(R.id.recentlySoldItemImage)
                        val switchViewButton = dialogView.findViewById<Button>(R.id.recentlySoldBtnSwitchView)

                        val bitmap = BitmapFactory.decodeByteArray(item.imageData, 0, item.imageData.size)

                        itemImageView.setImageBitmap(bitmap)

                        recentlySoldDescriptionEditText.setText(item.description)

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
                val itemId = item.getString("id")
                val imageBase64 = item.getString("image")

                val imageData = Base64.decode(imageBase64, Base64.DEFAULT)
                items.add(RecentlySoldItem(category, name, date, price, description, itemId, imageData))
            }
            return items
        }
    }
}
