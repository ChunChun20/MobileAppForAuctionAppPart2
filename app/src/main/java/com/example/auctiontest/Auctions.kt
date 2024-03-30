package com.example.auctiontest

import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class Auctions : AppCompatActivity() {
    private lateinit var itemName: TextView
    private lateinit var itemBid: TextView
    private lateinit var itemId: TextView
    private lateinit var itemBidder: TextView
    private lateinit var itemEnd: TextView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auctions)
        itemName = findViewById(R.id.itemName)
        itemBid = findViewById(R.id.itemBid)
        itemId = findViewById(R.id.itemId)
        itemBidder = findViewById(R.id.itemBidder)
        itemEnd = findViewById(R.id.itemEnd)
        FetchData().execute()
    }

    inner class FetchData : AsyncTask<Void, Void, Pair<List<String>?, String?>>() {
        override fun doInBackground(vararg params: Void?): Pair<List<String>?, String?> {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("http://192.168.0.104:5000/auctions_mobile")
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

        override fun onPostExecute(result: Pair<List<String>?, String?>) {
            super.onPostExecute(result)
            result.first?.let { items ->
                val itemNames = mutableListOf<String>()
                val itemBids = mutableListOf<String>()
                val itemCategories = mutableListOf<String>()
                val itemBidders = mutableListOf<String>()
                val itemEnds = mutableListOf<String>()

                for (itemString in items) {
                    val itemData = itemString.split(" - ")
                    val name = itemData[0].substring(0, itemData[0].length - 6)
                    val price = itemData[1].substringAfter(":").trim() + " $"
                    val category = itemData[2].substringAfter(":").trim()
                    val end = itemData[3].substringAfter(":").trim()
                    val bidder = itemData[4].substringAfter(":").trim()

                    itemNames.add(name)
                    itemBids.add(price)
                    itemCategories.add(category)
                    itemBidders.add(bidder)
                    itemEnds.add(end)
                }

                itemName.text = itemNames.joinToString("\n")
                itemBid.text = itemBids.joinToString("\n")
                itemId.text = itemCategories.joinToString("\n")
                itemBidder.text = itemBidders.joinToString("\n")
                itemEnd.text = itemEnds.joinToString("\n")
            }
            result.second?.let { errorMessage ->
                Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        private fun parseData(data: String?): List<String>? {
            data ?: return null
            val jsonObject = JSONObject(data)
            val jsonArray = jsonObject.getJSONArray("auctions")
            val items = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val category = item.getString("category")
                val name = item.getString("name")
                val price = item.getDouble("bid")
                val bidder = item.getString("highest_bidder")
                val end = item.getString("end")
                Log.d("FetchData", "Item: $category - $name - $price - $bidder - $end")
                items.add("$name - Price: $price - Category: $category - End: $end - Highest bidder: $bidder")
            }
            return items
        }
    }
}