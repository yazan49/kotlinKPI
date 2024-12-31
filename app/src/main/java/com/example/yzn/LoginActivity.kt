package com.example.yzn

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {
    private var loader: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val noAccount = findViewById<TextView>(R.id.tv_noAccount)

        noAccount.setOnClickListener {
            val intent = Intent(this , RegisterActivity::class.java)
            startActivity(intent)
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val emailEditText = findViewById<EditText>(R.id.et_email)
        val passwordEditText = findViewById<EditText>(R.id.et_password)
        val loginButton = findViewById<Button>(R.id.btn_login)

        passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD


        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                sendLoginRequest(email, password)
            } else {
                showAlert("","Please enter both email and password", )
            }
        }
    }




    private fun sendLoginRequest(email: String, password: String) {

        val url = Constants.LOGIN

        val jsonObject = JSONObject()
        jsonObject.put("email", email)
        jsonObject.put("password", password)

        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val client = OkHttpClient()

        loader = ProgressDialog(this)
        loader?.setMessage("Please Wait...")
        loader?.setCancelable(false)
        loader?.show()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    println("rrrrrrrr ${e.message}")
                    showAlert("","${e.message}", )
                    loader?.dismiss()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseString = response.body?.string()
                runOnUiThread {
                    val jsonResponse = JSONObject(responseString)
                    val message = jsonResponse.optString("message", "No message provided")

                    if (response.isSuccessful) {
                        val data = jsonResponse.optJSONObject("data")

                        val userId = data?.optString("_id", null)

                        val token = jsonResponse.optString("token", null)

                        if (userId != null) {
                            val sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE)
                            val editor = sharedPreferences.edit()
                            editor.putString("email", email)
                            editor.putString("user_id", userId)
                            editor.putString("token", token)
                            editor.apply()

                            Toast.makeText(this@LoginActivity, "Success $message", Toast.LENGTH_LONG).show()
                            val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        } else {
                            showAlert("Error", "Invalid user data or token")
                        }
                    } else {
                        showAlert("Error", "Error: $message")
                    }

                    loader?.dismiss()
                }
            }
        })
    }
    private fun showAlert(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }
}
