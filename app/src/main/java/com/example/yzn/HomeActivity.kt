package com.example.yzn

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.yzn.services.LocationService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class HomeActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null

    private lateinit var logoutButton: ImageButton

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var locationToggle: Switch

    private lateinit var foregroundPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var backgroundPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        logoutButton = findViewById(R.id.logoutButton)

        logoutButton.setOnClickListener {
            logout()
        }



    mapView = findViewById(R.id.mapView)
        locationToggle = findViewById(R.id.locationToggle)
        val historyButton: Button = findViewById(R.id.historyButton)
        sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        initializePermissionLaunchers()
        requestLocationPermissions()

        val isToggleChecked = sharedPreferences.getBoolean("locationToggleState", false)
        locationToggle.isChecked = isToggleChecked
        setupToggle()

        if (isToggleChecked) {
            startLocationService()
        }

        historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun logout() {
        val preferences = getSharedPreferences("user_data", MODE_PRIVATE)
        val isRemoved = preferences.edit().remove("email").commit()

        if (isRemoved) {
            Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to Logout", Toast.LENGTH_SHORT).show()
        }
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }



    private fun setupToggle() {
        locationToggle.setOnCheckedChangeListener { _, isChecked ->
            println("Switch state changed: $isChecked")

            sharedPreferences.edit().putBoolean("locationToggleState", isChecked).apply()

            if (isChecked) {
                startLocationService()
                println("Enabling location updates to server.")
            } else {
                stopLocationService()
                println("Disabling location updates to server.")
            }
        }
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java).apply {
            putExtra("isLocationEnabled", true)
        }
        startService(intent)
        println("Location service started.")
    }

    private fun stopLocationService() {
        val intent = Intent(this, LocationService::class.java).apply {
            putExtra("isLocationEnabled", false)
        }
        startService(intent)
        println("Location service stopped.")
    }

    private fun initializePermissionLaunchers() {
        foregroundPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    requestBackgroundLocationPermission()
                } else {
                    println("Foreground location permission denied")
                }
            }

        backgroundPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    println("Background location permission granted")
                } else {
                    println("Background location permission denied")
                }
            }
    }

    private fun requestLocationPermissions() {
        foregroundPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
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
            googleMap?.isMyLocationEnabled = true
            getCurrentLocation()
        } else {
            println("Location permission not granted")
        }
    }

    private fun getCurrentLocation() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                googleMap?.addMarker(MarkerOptions().position(currentLatLng).title("You are here"))
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            } else {
                println("Failed to get location")
            }
        }.addOnFailureListener { exception ->
            println("Error fetching location: ${exception.message}")
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