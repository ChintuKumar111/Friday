package com.example.friday

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val messages = mutableListOf<Message>()
    private lateinit var chatAdapter: ChatAdapter

    private val apiKey = "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"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val logOut = findViewById<ImageView>(R.id.logout)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)
        val loadingAnimation = findViewById<LottieAnimationView>(R.id.loadingAnimation)

        chatAdapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = chatAdapter

        btnSend.setOnClickListener {
            val userMessage = etMessage.text.toString().trim()
            if (userMessage.isEmpty()) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSend.isEnabled = false
            addMessageToChat(userMessage, true)

            loadingAnimation.visibility = View.VISIBLE
            loadingAnimation.playAnimation()

            getChatbotResponse(userMessage, loadingAnimation) { response ->
                runOnUiThread {
                    addMessageToChat(response, false)
                    loadingAnimation.cancelAnimation()
                    loadingAnimation.visibility = View.GONE
                    btnSend.isEnabled = true
                }
            }

            etMessage.text.clear()
        }

        logOut.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Logout")
            builder.setMessage("Are you sure you want to logout?")

            builder.setPositiveButton("Yes") { dialog, _ ->
                // Perform logout action here (e.g., clearing session, navigating to login)
                dialog.dismiss()
                startActivity(Intent(this, LoginActivity:: class.java))
                finish() // Close the activity
            }

            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }

            val alertDialog = builder.create()
            alertDialog.show()
        }

    }

    private fun addMessageToChat(message: String, isUser: Boolean) {
        messages.add(Message(message, isUser))
        chatAdapter.notifyItemInserted(messages.size - 1)
        findViewById<RecyclerView>(R.id.recyclerView).scrollToPosition(messages.size - 1)
    }
    private fun getChatbotResponse(userMessage: String, loadingAnimation: LottieAnimationView, callback: (String) -> Unit) {
        val url = "https://openrouter.ai/api/v1/chat/completions"

        // Request body for the API
        val requestBody = """
        {
            "model": "openai/gpt-3.5-turbo-0613",
            "messages": [
                {
                    "role": "user",
                    "content": "$userMessage"
                }
            ]
        }
    """.trimIndent()

        // OkHttp client with timeouts
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        // Build the request
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://localhost") // Required by OpenRouter
            .addHeader("X-Title", "friday") // Required by OpenRouter
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        // Enqueue the request
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    addMessageToChat("❌ Network Error: ${e.message}", false)
                    loadingAnimation.cancelAnimation()
                    loadingAnimation.visibility = View.GONE
                }
                Log.e("API_ERROR", "Request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                Log.d("API_RESPONSE", "Raw response: $body") // Log the raw response

                if (!response.isSuccessful) {
                    runOnUiThread {
                        val errorMessage = when (response.code) {
                            401 -> "⚠️ Invalid API Key"
                            402 -> "⚠️ Payment Required: Upgrade Plan"
                            429 -> "⚠️ Rate Limit Exceeded"
                            else -> "⚠️ API Error: ${response.code}, Message: $body"
                        }
                        addMessageToChat(errorMessage, false)
                        loadingAnimation.cancelAnimation()
                        loadingAnimation.visibility = View.GONE
                    }
                    Log.e("API_ERROR", "Error: ${response.code}, Message: $body")
                    return
                }

                try {
                    val jsonResponse = JSONObject(body ?: "{}")
                    Log.d("API_RESPONSE", "Parsed JSON: $jsonResponse") // Log the parsed JSON

                    // Check if the response contains the "choices" field
                    if (!jsonResponse.has("choices")) {
                        runOnUiThread {
                            addMessageToChat("⚠️ No 'choices' field in response", false)
                        }
                        Log.e("API_ERROR", "No 'choices' field in response: $jsonResponse")
                        return
                    }

                    // Extract the "choices" array
                    val choicesArray = jsonResponse.optJSONArray("choices")
                    if (choicesArray == null || choicesArray.length() == 0) {
                        runOnUiThread {
                            addMessageToChat("⚠️ No choices in response", false)
                        }
                        Log.e("API_ERROR", "No choices in response: $jsonResponse")
                        return
                    }

                    // Extract the first choice
                    val firstChoice = choicesArray.optJSONObject(0)
                    if (firstChoice == null) {
                        runOnUiThread {
                            addMessageToChat("⚠️ Invalid choice format", false)
                        }
                        Log.e("API_ERROR", "Invalid choice format: $jsonResponse")
                        return
                    }

                    // Extract the "message" object
                    val messageObject = firstChoice.optJSONObject("message")
                    if (messageObject == null) {
                        runOnUiThread {
                            addMessageToChat("⚠️ Invalid message format", false)
                        }
                        Log.e("API_ERROR", "Invalid message format: $jsonResponse")
                        return
                    }

                    // Extract the "content" field
                    val resultText = messageObject.optString("content", "No response")
                    runOnUiThread {
                        callback(resultText)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        addMessageToChat("⚠️ Response Parsing Error", false)
                    }
                    Log.e("API_ERROR", "Parsing error: ${e.message}")
                }
            }
        })
         var backPressedTime: Long = 0
         val backToastDuration = 2000 // 2 seconds
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedTime + backToastDuration > System.currentTimeMillis()) {
                    finish()
                } else {
                    Toast.makeText(this@MainActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()
                    backPressedTime = System.currentTimeMillis()
                }
            }
        })
    }


}
