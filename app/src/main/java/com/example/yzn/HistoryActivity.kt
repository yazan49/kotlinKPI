package com.example.yzn

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class HistoryActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.backArrow).setOnClickListener {
            finish()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isMyLocationButtonEnabled = true

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
            googleMap?.isMyLocationEnabled = true
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE)
        val userId = sharedPreferences.getString("user_id", null)
        val token = sharedPreferences.getString("token", null)

        if (userId == null || token == null) {
            println("rrrrrrr Missing user data: User ID or token is null")
            return
        }
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
              //  googleMap?.addMarker(MarkerOptions().position(currentLatLng).title("You are here"))
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            } else {
                println("Failed to get location")
            }
        }.addOnFailureListener { exception ->
            println("Error fetching location: ${exception.message}")
        }
        fetchUserLocations(userId, token)
    }

    private fun fetchUserLocations(userId: String, token: String) {
        val client = OkHttpClient()
        val url = "${Constants.GET_LOCATION}$userId"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("rrrrrrr Error fetching user locations: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseString = response.body?.string()
                println("rrrrrrr API Response: $responseString")

                if (response.isSuccessful) {
                    try {
                        val jsonObject = JSONObject(responseString)
                        val dataArray = jsonObject.getJSONArray("data")

                        runOnUiThread {
                            addLocationsToMap(dataArray)
                        }
                    } catch (e: Exception) {
                        println("rrrrrrr Parsing error: ${e.message}")
                    }
                } else {
                    println("rrrrrrr API Error: ${response.code}")
                }
            }
        })
    }
    private fun addLocationsToMap(locations: JSONArray) {
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.marker2)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false)
        val customMarker = BitmapDescriptorFactory.fromBitmap(scaledBitmap)

        for (i in 0 until locations.length()) {
            val locationObject = locations.getJSONObject(i)
            val lat = locationObject.getDouble("lat")
            val long = locationObject.getDouble("long")
            val time = locationObject.optString("time")

            val formattedTime = try {
                val date = Date(time.toLong())
                val dateFormat = SimpleDateFormat("EEE hh:mm a", Locale.getDefault())
                dateFormat.timeZone = TimeZone.getDefault()
                dateFormat.format(date)
            } catch (e: Exception) {
                "Invalid Time"
            }

            val latLng = LatLng(lat, long)
            googleMap?.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(formattedTime)
                    .icon(customMarker)
            )

        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}