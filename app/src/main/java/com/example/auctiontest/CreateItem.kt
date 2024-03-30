package com.example.auctiontest

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException

class CreateItem : AppCompatActivity() {

    private lateinit var createName: EditText
    private lateinit var createDesc: EditText
    private lateinit var createBid: EditText
    private lateinit var createDuration: EditText
    private lateinit var create: Button
    private lateinit var imageUri: Uri

    // Constant for requesting image selection
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_item)

        val spinner: Spinner = findViewById(R.id.spinnerCategory)
        createName = findViewById(R.id.createName)
        createDesc = findViewById(R.id.createDesc)
        createBid = findViewById(R.id.createBid)
        createDuration = findViewById(R.id.createDuration)
        create = findViewById(R.id.create)
        val userId = intent.getStringExtra("id").toString()


        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            this,
            R.array.dropdown_items_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }

        // Button to choose image
        val chooseImageButton: Button = findViewById(R.id.chooseImageBtn)
        chooseImageButton.setOnClickListener {
            openGallery()
        }

        // Button to create item
        create.setOnClickListener {
            val itemName = createName.text.toString()
            val itemDesc = createDesc.text.toString()
            val itemBid = createBid.text.toString()
            val itemDuration = createDuration.text.toString()

            // Get selected category from spinner
            val spinnerCategory: Spinner = findViewById(R.id.spinnerCategory)
            val selectedCategory = spinnerCategory.selectedItem.toString()

            if (::imageUri.isInitialized) { // Check if imageUri is initialized
                // Upload item data along with image
                val imageFile = File(imageUri.path)
                uploadItemData(itemName, selectedCategory, itemDesc, itemBid, itemDuration, imageUri, userId)
            } else {
                // Show a message to the user indicating that an image is required
                Toast.makeText(applicationContext, "Please choose an image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to open image picker
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // Function to handle image selection result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data!!
            // Now you have the image URI, you can use it to display or upload the image
            val imageView: ImageView = findViewById(R.id.imageView)
            imageView.setImageURI(imageUri)
        }
    }

    // Function to upload item data along with image
    private fun uploadItemData(name: String, category: String, description: String, bid: String, duration: String, selectedImageUri: Uri, id: String) {
        val client = OkHttpClient()

        val inputStream = contentResolver.openInputStream(selectedImageUri)
        val imageFileName = "${System.currentTimeMillis()}_${(1..1000).random()}.jpg" // Generate a unique filename
        val imageFile = File(cacheDir, imageFileName) // Create image file with the generated filename
        inputStream?.use { input ->
            imageFile.outputStream().use { output ->
                input.copyTo(output) // Copy input stream to the image file
            }
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("name", name)
            .addFormDataPart("category", category)
            .addFormDataPart("description", description)
            .addFormDataPart("bid", bid)
            .addFormDataPart("duration", duration)
            .addFormDataPart("id", id)
            .addFormDataPart("image", imageFileName, RequestBody.create("image/*".toMediaTypeOrNull(), imageFile)) // Use the generated filename here
            .build()

        val request = Request.Builder()
            .url("http://192.168.0.104:5000/create_item_mobile")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
                e.printStackTrace()
                runOnUiThread {
                    // Show error message or retry option to the user
                    Toast.makeText(applicationContext, "Failed to upload item", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                // Handle success
                if (!response.isSuccessful) {
                    // Handle unsuccessful response
                    runOnUiThread {
                        // Show error message to the user
                        Toast.makeText(applicationContext, "Failed to upload item", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Handle successful response
                    runOnUiThread {
                        // Show success message or navigate to next screen
                        Toast.makeText(applicationContext, "Item uploaded successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@CreateItem, MainActivity::class.java)
                        intent.putExtra("id", id)
                        startActivity(intent)
                    }
                }
            }
        })
    }
}