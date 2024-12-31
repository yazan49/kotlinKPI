package com.example.yzn

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE)
        val email = sharedPreferences.getString("email", null)
        println("sharedPreferences $sharedPreferences email $email")

        val intent = if (email != null) {
            Intent(this, HomeActivity::class.java)
        } else {
            Intent(this, RegisterActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}