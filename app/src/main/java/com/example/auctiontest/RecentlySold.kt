package com.example.auctiontest

import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class RecentlySold : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recently_sold)

        FetchData().execute()
    }

    inner class FetchData : AsyncTask<Void, Void, Pair<List<RecentlySoldItem>?, String?>>() {
        override fun doInBackground(vararg params: Void?): Pair<List<RecentlySoldItem>?, String?> {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("http://192.168.0.104:5000/recently_sold_mobile")
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    return Pair(parseData(responseBody), null)
                } else {
                    return Pair(null, "Failed to fetch data: ${response.code}")
                }
            } catch (e: Exception) {
                return Pair(null, "Exception: ${e.message}")
            }
        }

        override fun onPostExecute(result: Pair<List<RecentlySoldItem>?, String?>) {
            super.onPostExecute(result)
            result.first?.let { items ->
                val inflater = LayoutInflater.from(this@RecentlySold)
                val itemContainer = findViewById<LinearLayout>(R.id.parentLayout)

                // Clear existing views
                itemContainer.removeAllViews()

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
                        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)


//                        recentlySoldItemTextView.text = item.name
                        recentlySoldDescriptionEditText.setText(item.description)

                        val alertDialog = dialogBuilder.create()

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
                items.add(RecentlySoldItem(category, name, date, price, description,itemId))
            }
            return items
        }
    }
}