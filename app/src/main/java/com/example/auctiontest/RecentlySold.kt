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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class RecentlySold : AppCompatActivity() {


    private var currentPage = 1
    private val perPage = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recently_sold)

        fetchData(currentPage)

        val nextPageButton = findViewById<Button>(R.id.nextPageButton)
        nextPageButton.setOnClickListener {
            currentPage++
            fetchData(currentPage)

            // Show prevPageButton when currentPage > 1
            val prevPageButton = findViewById<Button>(R.id.prevPageButton)
            if (currentPage > 1) {
                prevPageButton.visibility = View.VISIBLE
            }
        }

        // Button to load previous page
        val prevPageButton = findViewById<Button>(R.id.prevPageButton)
        prevPageButton.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                fetchData(currentPage)
            }

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


    private fun fetchData(page: Int) {
        val loadingLayout = findViewById<FrameLayout>(R.id.loadingLayout)
        loadingLayout.visibility = View.VISIBLE
        FetchData().execute(page)
    }

    inner class FetchData : AsyncTask<Int, Void, Triple<List<RecentlySoldItem>?, String?, Int>>() {
        override fun doInBackground(vararg params: Int?): Triple<List<RecentlySoldItem>?, String?, Int> {
            val page = params[0] ?: 1
            val client = OkHttpClient()
            val url = "http://192.168.0.104:5000/recently_sold_mobile?page=$page"
            val request = Request.Builder()
                .url(url)
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
                val inflater = LayoutInflater.from(this@RecentlySold)
                val itemContainer = findViewById<LinearLayout>(R.id.parentLayout)

                // Clear existing views
                itemContainer.removeAllViews()

                val nextPageButton = findViewById<Button>(R.id.nextPageButton)
                nextPageButton.visibility = if (currentPage == result.third) View.GONE else View.VISIBLE

                for (item in items) {
                    val itemView = inflater.inflate(R.layout.recently_sold_layout, null)


                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.setMargins(0, 8, 0, 8) //

                    itemView.layoutParams = layoutParams

                    val nameView = itemView.findViewById<TextView>(R.id.nameView)
                    val dateView = itemView.findViewById<TextView>(R.id.dateView)
                    val priceView = itemView.findViewById<TextView>(R.id.priceView)
                    val categoryView = itemView.findViewById<TextView>(R.id.categoryView)
                    val infoButton = itemView.findViewById<Button>(R.id.infoButton)

                    var RecentlySoldItemName = item.name.substring(0, item.name.length - 6)
                    var RecentlySoldItemDate = item.date.substringBefore(",")
                    var RecentlySoldItemPrice = item.price
                    var RecentlySoldItemCategory = item.category



                    nameView.text = "Name: $RecentlySoldItemName"
                    dateView.text = "Date: $RecentlySoldItemDate"
                    priceView.text = "Sold for: $RecentlySoldItemPrice$"
                    categoryView.text = "Category: $RecentlySoldItemCategory"

                    infoButton.setOnClickListener {
                        val dialogView = LayoutInflater.from(this@RecentlySold).inflate(R.layout.recently_sold_info, null)
                        val dialogBuilder = AlertDialog.Builder(this@RecentlySold)
                            .setView(dialogView)
                            .setTitle("$RecentlySoldItemName")

//                        val recentlySoldItemTextView = dialogView.findViewById<TextView>(R.id.recentlySoldItem)
                        val recentlySoldDescriptionEditText = dialogView.findViewById<TextView>(R.id.recentlySoldDescription)
                        val itemImageView = dialogView.findViewById<ImageView>(R.id.recentlySoldItemImage)
                        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)
                        val switchViewButton = dialogView.findViewById<Button>(R.id.recentlySoldBtnSwitchView)


//                        recentlySoldItemTextView.text = item.name
                        recentlySoldDescriptionEditText.setText(item.description)

                        // Convert decoded byte array to Bitmap
                        val bitmap = BitmapFactory.decodeByteArray(item.imageData, 0, item.imageData.size)
                        // Set bitmap to ImageView
                        itemImageView.setImageBitmap(bitmap)





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
            val jsonArray = jsonObject.getJSONArray("recently_sold")
            val items = mutableListOf<RecentlySoldItem>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val category = item.getString("category")
                val name = item.getString("name")
                val date = item.getString("end")
                val price = item.getString("bid")
                val description = item.getString("description")
                val itemId = item.getString("id")
                val imageBase64 = item.getString("image")

                val imageData = Base64.decode(imageBase64, Base64.DEFAULT)

                items.add(RecentlySoldItem(category, name, date, price, description,itemId,imageData))
            }
            return items
        }
    }
}