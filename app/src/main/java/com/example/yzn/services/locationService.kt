package com.example.yzn.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.yzn.Constants
import com.google.android.gms.location.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LocationService : Service() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var isLocationEnabled: Boolean = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val newState = intent?.getBooleanExtra("isLocationEnabled", false) ?: false
        if (newState != isLocationEnabled) {
            isLocationEnabled = newState
            if (isLocationEnabled) {
                startLocationUpdates()
            } else {
                stopLocationUpdates()
            }
        }
        return START_STICKY
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationProviderClient.removeLocationUpdates(it)
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 15000
            fastestInterval = 10000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    if (isLocationEnabled) {
                        sendLocationToServer(location)
                    }
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendLocationToServer(location: Location) {
        val sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE)
        val userId = sharedPreferences.getString("user_id", null)
        val url = Constants.LOCATION_SAVE
        val jsonObject = JSONObject().apply {
            put("lat", location.latitude)
            put("long", location.longitude)
            put("speed", location.speed)
            put("accuracy", location.accuracy)
            put("speedAccuracy", location.speedAccuracyMetersPerSecond)
            put("time", System.currentTimeMillis())
            put("userId", userId)
        }

        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(requestBody).build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("rrrrrrrrrrr Error sending location: ${e.message}")
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                println("rrrrrrrrrrr Location API Response: ${response.body?.string()}")
            }
        })
    }

    private fun startForegroundService() {
        val channelId = "location_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Location Service", NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Service")
            .setContentText("Tracking your location")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { fusedLocationProviderClient.removeLocationUpdates(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}