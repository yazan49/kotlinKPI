package com.example.yzn

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Patterns
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

class RegisterActivity : AppCompatActivity() {
    private var loader: ProgressDialog? = null

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var signUpButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        emailEditText = findViewById(R.id.et_email)
        passwordEditText = findViewById(R.id.et_password)
        firstName = findViewById(R.id.et_firstName)
        lastName = findViewById(R.id.et_lastName)

        passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD


        phoneEditText = findViewById(R.id.et_phone)
        signUpButton = findViewById(R.id.btn_signup)
        val skipButton = findViewById<TextView>(R.id.tv_skip)
        val tvHaveAccount = findViewById<TextView>(R.id.tv_haveAccount)

        signUpButton.isEnabled = false

        tvHaveAccount.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        skipButton.setOnClickListener{
            val intent = Intent(this@RegisterActivity , HomeActivity::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateButtonState()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        emailEditText.addTextChangedListener(textWatcher)
        passwordEditText.addTextChangedListener(textWatcher)
        firstName.addTextChangedListener(textWatcher)
        lastName.addTextChangedListener(textWatcher)
        phoneEditText.addTextChangedListener(textWatcher)

        signUpButton.setOnClickListener {
            validateAndSignUp()
        }
    }

    private fun validateAndSignUp() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val firstName = firstName.text.toString().trim()
        val lastName = lastName.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()

        when {
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> showAlert("Validation Error", "Please enter a valid email address.")

            else -> sendSignUpRequest(email, password, firstName , lastName, phone)
        }
    }

    private fun sendSignUpRequest(email: String, password: String, firstName: String,lastName:String, phone: String) {
        val url = Constants.REGISTER

        val jsonObject = JSONObject().apply {
            put("email", email)
            put("password", password)
            put("firstName", firstName)
            put("lastName", lastName)
            put("phone", phone)
        }

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
                    showAlert("Network Error", e.message ?: "Unknown error occurred.")
                    loader?.dismiss()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseString = response.body?.string()

                runOnUiThread {
                    loader?.dismiss()
                    if (response.isSuccessful && responseString != null) {
                        val jsonResponse = JSONObject(responseString)
                        val message = jsonResponse.optString("message", "No message provided")
                        Toast.makeText(this@RegisterActivity, "Register Successful: $message", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                        startActivity(intent)
                    } else {
                        val error = JSONObject(responseString ?: "{}").optString("message", "Unknown error")
                        showAlert("Error", error)
                    }
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
        builder.create().show()
    }

    private fun updateButtonState() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val firstName = firstName.text.toString().trim()
        val lastName = lastName.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()

        signUpButton.isEnabled = email.isNotEmpty() && password.isNotEmpty() && firstName.isNotEmpty() && lastName.isNotEmpty()
                phone.isNotEmpty()
    }
}