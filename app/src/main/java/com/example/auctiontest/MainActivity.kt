package com.example.auctiontest

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import android.util.Base64
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel


class MainActivity : AppCompatActivity() {

    var CHANNEL_ID = "1"
    var NOTIFICATION_ID = 1

    private lateinit var recentlySoldBtn: ImageButton
    private lateinit var loggedUser: TextView
    private lateinit var budget: TextView
    private lateinit var ownedBtn: ImageButton

    private lateinit var createItem: ImageButton
    private lateinit var mailBoxBtn: ImageButton
    private lateinit var pageReloadBtn: ImageButton

    private var currentUser: String = ""
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recentlySoldBtn = findViewById(R.id.RecentlySold)
        loggedUser = findViewById(R.id.loggedUser)
        budget = findViewById(R.id.budget)
        ownedBtn = findViewById(R.id.ownedItems)
        createItem = findViewById(R.id.createItem)
        mailBoxBtn = findViewById(R.id.mailBox)
        pageReloadBtn = findViewById(R.id.page_reload)


        val userId = intent.getStringExtra("id").toString()
        currentUserId = userId


        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = geUserInfo(userId)
                handleUserInfoResponse(response)
            } catch (e: IOException) {
                // Handle network error
                e.printStackTrace()
//                runOnUiThread { showError("Network error") }
            }
        }

        recentlySoldBtn.setOnClickListener {
            val intent = Intent(this@MainActivity, RecentlySold::class.java)
            startActivity(intent)
        }

        ownedBtn.setOnClickListener {
            val intent = Intent(this@MainActivity, OwnedItems::class.java)
            intent.putExtra("id", userId)
            startActivity(intent)
        }

        createItem.setOnClickListener {
            val intent = Intent(this@MainActivity, CreateItem::class.java)
            intent.putExtra("id", userId)
            startActivity(intent)
        }

        mailBoxBtn.setOnClickListener {
            val intent = Intent(this@MainActivity, MailBox::class.java)
            intent.putExtra("id", userId)
            intent.putExtra("username", currentUser)
            startActivity(intent)
        }

        loggedUser.setOnClickListener {
            val intent = Intent(this@MainActivity, UserProfile::class.java)
            intent.putExtra("id", userId)
            startActivity(intent)
        }

        pageReloadBtn.setOnClickListener {
            recreate()
        }


        val loadingLayout = findViewById<FrameLayout>(R.id.loadingLayout)
        loadingLayout.visibility = View.VISIBLE

        FetchData().execute()
    }

    inner class FetchData : AsyncTask<Void, Void, Pair<List<AuctionItem>?, String?>>() {
        override fun doInBackground(vararg params: Void?): Pair<List<AuctionItem>?, String?> {
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


        override fun onPostExecute(result: Pair<List<AuctionItem>?, String?>) {
            super.onPostExecute(result)
            val loadingLayout = findViewById<FrameLayout>(R.id.loadingLayout)
            loadingLayout.visibility = View.GONE
            result.first?.let { items ->
                val inflater = LayoutInflater.from(this@MainActivity)
                val itemContainer = findViewById<LinearLayout>(R.id.parentLayout)


                itemContainer.removeAllViews()

                for (item in items) {
                    val itemView = inflater.inflate(R.layout.auctions_layout, null)


                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.setMargins(0, 8, 0, 8) //

                    itemView.layoutParams = layoutParams

                    val nameView = itemView.findViewById<TextView>(R.id.nameView)
                    val endView = itemView.findViewById<TextView>(R.id.endView)
                    val priceView = itemView.findViewById<TextView>(R.id.priceView)
                    val categoryView = itemView.findViewById<TextView>(R.id.categoryView)
                    val bidderView = itemView.findViewById<TextView>(R.id.bidderView)
                    val infoButton = itemView.findViewById<Button>(R.id.infoButton)
                    infoButton.tag = item.id

                    var ItemName = item.name.substring(0, item.name.length - 6)
                    var ItemEndDate = item.end
                    var ItemCurrentBid = item.price
                    var ItemCategory = item.category
                    val ItemHighestBidder = item.highestBidder



                    nameView.text = "Name: $ItemName"
                    endView.text = "Ends: $ItemEndDate"
                    priceView.text = "Current Bid: $ItemCurrentBid$"
                    categoryView.text = "Category: $ItemCategory"
                    bidderView.text = "Highest Bidder: $ItemHighestBidder"

                    infoButton.setOnClickListener {
                        val dialogView = LayoutInflater.from(this@MainActivity)
                            .inflate(R.layout.auction_info, null)
                        val dialogBuilder = AlertDialog.Builder(this@MainActivity)
                            .setView(dialogView)
                            .setTitle("Item: $ItemName")

                        val recentlySoldDescriptionEditText =
                            dialogView.findViewById<TextView>(R.id.recentlySoldDescription)
                        val itemImageView = dialogView.findViewById<ImageView>(R.id.itemImage)
                        val btnMailOwner = dialogView.findViewById<Button>(R.id.btnMailOwner)
                        val btnBidNow = dialogView.findViewById<Button>(R.id.btnBid)
                        val btnCustomBidNow = dialogView.findViewById<Button>(R.id.btnCustomBid)
                        val switchViewButton = dialogView.findViewById<Button>(R.id.btnSwitchView)
                        val btnViewUser = dialogView.findViewById<Button>(R.id.btnViewUser)


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

                        btnViewUser.setOnClickListener {
                            val intent = Intent(this@MainActivity, UserProfile::class.java)
                            intent.putExtra("id", item.sellerId)
                            startActivity(intent)
                        }

                        btnMailOwner.setOnClickListener {


                            val senderId = item.sellerId
                            val receiverId = currentUserId
                            val senderUsername = currentUser


                            val dialogView = layoutInflater.inflate(R.layout.reply_mail, null)
                            val dialogBuilder = AlertDialog.Builder(this@MainActivity)
                                .setView(dialogView)
                                .setTitle("Send Mail to owner of ${item.name.substring(0, item.name.length - 6)}")

                            val dialog = dialogBuilder.create()
                            dialog.show()

                            val btnSend = dialogView.findViewById<Button>(R.id.btnSend)
                            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
                            val editSubject = dialogView.findViewById<EditText>(R.id.editSubject)
                            val editMessage = dialogView.findViewById<EditText>(R.id.editMessage)



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

                        btnBidNow.setOnClickListener {
                            val dialogView = layoutInflater.inflate(R.layout.bid_now_button, null)
                            val dialogBuilder = AlertDialog.Builder(this@MainActivity)
                                .setView(dialogView)
                                .setTitle("Bid on ${item.name.substring(0, item.name.length - 6)}")
                            val dialog = dialogBuilder.create()

                            dialog.show()

                            val minimumNextBid = dialogView.findViewById<TextView>(R.id.minimumNextBid)

                            GlobalScope.launch(Dispatchers.Main) {
                                try {
                                    val nextMinimumBid = getNextMinimumBid(item.id)
                                    minimumNextBid.text = "Minimum next bid: $nextMinimumBid$"
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                    Toast.makeText(this@MainActivity, "Error with getting minimum next bid", Toast.LENGTH_LONG).show()
                                }
                            }


                            val btnBid = dialogView.findViewById<Button>(R.id.btnBidNow)


                            btnBid.setOnClickListener {
                                GlobalScope.launch(Dispatchers.Main) {
                                    try {
                                        val response = sendBid(item.name,currentUser)
                                        handleSendBidResponse(response)
                                    } catch (e: IOException) {
                                        // Handle network error
                                        e.printStackTrace()
                                        Toast.makeText(this@MainActivity, "Error", Toast.LENGTH_LONG).show()
                                        // Show error on UI if necessary
                                    }
                                }
                                dialog.dismiss()
                            }
                        }


                        btnCustomBidNow.setOnClickListener {
                            val dialogView = layoutInflater.inflate(R.layout.custom_bid_now_button, null)
                            val dialogBuilder = AlertDialog.Builder(this@MainActivity)
                                .setView(dialogView)
                                .setTitle("Custom Bid on ${item.name.substring(0, item.name.length - 6)}")

                            val dialog = dialogBuilder.create()
                            dialog.show()


                            val minimumNextBid = dialogView.findViewById<TextView>(R.id.minimumNextBid)

                            GlobalScope.launch(Dispatchers.Main) {
                                try {
                                    val nextMinimumBid = getNextMinimumBid(item.id)
                                    minimumNextBid.text = "Enter custom bid above: $nextMinimumBid$"
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                    Toast.makeText(this@MainActivity, "Error with getting custom next bid", Toast.LENGTH_LONG).show()
                                }
                            }

                            val btnCustomBidNow = dialogView.findViewById<Button>(R.id.customBidNow)
                            val editTextCustomBid = dialogView.findViewById<EditText>(R.id.editTextBid)


                            btnCustomBidNow.setOnClickListener {
                                GlobalScope.launch(Dispatchers.Main) {
                                    try {
                                        val userCustomBid = editTextCustomBid.text.toString()
                                        val response = sendCustomBid(item.name,currentUser,userCustomBid)
                                        handleSendCustomBidResponse(response)
                                    } catch (e: IOException) {
                                        // Handle network error
                                        e.printStackTrace()
                                        Toast.makeText(this@MainActivity, "Error", Toast.LENGTH_LONG).show()
                                        // Show error on UI if necessary
                                    }
                                }
                                dialog.dismiss()
                            }
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
                Toast.makeText(this@MainActivity, "Success!", Toast.LENGTH_LONG).show()
                recreate()
            } else {
                val message = jsonResponse.optString("message", "")
                // Handle error message if needed
            }
        }


        private suspend fun sendBid(itemName: String,currentUser: String): String {
            val client = OkHttpClient()

            val url = "http://192.168.0.104:5000/auctions_bid_mobile"
            val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = JSONObject().apply {
                put("itemName", itemName)
                put("currentUser", currentUser)
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

        private fun handleSendBidResponse(response: String) {
            val jsonResponse = JSONObject(response)
            val success = jsonResponse.getBoolean("success")
            val message = jsonResponse.optString("message", "")

            Toast.makeText(this@MainActivity, "$success - $message", Toast.LENGTH_LONG).show()


            if (success) {
                val success = "Successful Bid"
                showBidNotification(message,success)
                recreate()
            }
            else {
                val success = "Failed bid"
                showBidNotification(message,success)
                recreate()
            }
        }


        private suspend fun sendCustomBid(itemName: String,currentUser: String,customBid: String): String {
            val client = OkHttpClient()

            val url = "http://192.168.0.104:5000/auctions_custom_bid_mobile"
            val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = JSONObject().apply {
                put("itemName", itemName)
                put("currentUser", currentUser)
                put("customBid", customBid)
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

        private fun handleSendCustomBidResponse(response: String) {
            val jsonResponse = JSONObject(response)
            val success = jsonResponse.getBoolean("success")
            val message = jsonResponse.optString("message", "")

            


            if (success) {
                val success = "Successful Bid"
                showBidNotification(message,success)
                recreate()
            }
            else {
                val success = "Failed Bid"
                showBidNotification(message,success)
                recreate()
            }
        }


        private fun parseData(data: String?): List<AuctionItem>? {
            data ?: return null
            val jsonObject = JSONObject(data)
            val jsonArray = jsonObject.getJSONArray("auctions")
            val items = mutableListOf<AuctionItem>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val itemId = item.getString("id")
                val description = item.getString("description")
                val start = item.getString("start")
                val category = item.getString("category")
                val name = item.getString("name")
                val sellerId = item.getString("seller_id")
                val price = item.getString("bid")
                val end = item.getString("end")
                val highestBidder = item.getString("highest_bidder")
                val imageBase64 = item.getString("image")

                val imageData = Base64.decode(imageBase64, Base64.DEFAULT)




                items.add(AuctionItem(itemId,description, start,category,name,sellerId,price, end,highestBidder, imageData ))
            }
            return items
        }


    }
    private fun geUserInfo(id: String): String {
        val client = OkHttpClient()

        val url = "http://192.168.0.104:5000/get_user_data"
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

    private fun handleUserInfoResponse(response: String) {
        val jsonResponse = JSONObject(response)
        val success = jsonResponse.getBoolean("success")

        if (success) {
            val user = jsonResponse.getJSONObject("user")
            val username = user.getString("username")
            currentUser = username
            val userBudget = user.getString("budget")
            val userId = user.getString("id")

            // Move UI update to main thread
            runOnUiThread {
                loggedUser.text = "Welcome, $username"
                val roundedBudget = BigDecimal(userBudget.toString()).setScale(2, RoundingMode.HALF_UP).toDouble()

                budget.text = "Budget: $roundedBudget$"

            }
        } else {
            val message = jsonResponse.getString("message")
//            runOnUiThread { showError(message) }
        }
    }

    private suspend fun getNextMinimumBid(itemId: String): String {
        val client = OkHttpClient()
        val url = "http://192.168.0.104:5000/get_next_bid/$itemId"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return withContext(Dispatchers.IO) { // Perform network operation on IO dispatcher
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    // Parse the JSON response to extract the minimum next bid
                    val jsonResponse = JSONObject(responseBody)
                    val minimumNextBid = jsonResponse.getString("minimum_next_bid")
                    minimumNextBid
                } else {
                    throw IOException("Unexpected response code: ${response.code}")
                }
            } catch (e: IOException) {
                throw IOException("Error fetching next minimum bid: ${e.message}")
            }
        }


    }


    private fun showBidNotification(message: String, success: String) {
        // Create an explicit intent for launching the MainActivity when the notification is clicked
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // Adding FLAG_IMMUTABLE
        )

        // Create notification
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.img)
            .setContentTitle(success)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Get the notification manager
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Check if Android version is Oreo or higher, and if so, create a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Channel Name",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Show the notification
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

}