package com.example.auctiontest

import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class OwnedItems : AppCompatActivity() {


    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_owned_items)


        userId = intent.getStringExtra("id") ?: ""

        FetchData().execute()
    }

    inner class FetchData : AsyncTask<Void, Void, Pair<List<RecentlySoldItem>?, String?>>() {
        override fun doInBackground(vararg params: Void?): Pair<List<RecentlySoldItem>?, String?> {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("http://192.168.0.104:5000/owned_items_mobile/$userId")
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
                val inflater = LayoutInflater.from(this@OwnedItems)
                val itemContainer = findViewById<LinearLayout>(R.id.parentLayout)

                // Clear existing views
                itemContainer.removeAllViews()

                for (item in items) {
                    val itemView = inflater.inflate(R.layout.owned_item_layout, null)


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
                    val deleteButton = itemView.findViewById<Button>(R.id.deleteButton)
                    deleteButton.tag = item.id

                    var RecentlySoldItemName = item.name.substring(0, item.name.length - 6)
                    var RecentlySoldItemDate = item.date.substringBefore(",")
                    var RecentlySoldItemPrice = item.price
                    var RecentlySoldItemCategory = item.category



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

//                        val recentlySoldItemTextView = dialogView.findViewById<TextView>(R.id.recentlySoldItem)
                        val recentlySoldDescriptionEditText =
                            dialogView.findViewById<TextView>(R.id.recentlySoldDescription)
                        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)


//                        recentlySoldItemTextView.text = item.name
                        recentlySoldDescriptionEditText.setText(item.description)

                        val alertDialog = dialogBuilder.create()

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
                items.add(RecentlySoldItem(category,name,date,price,description,itemId))
            }
            return items
        }
    }






}