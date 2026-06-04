package com.hasbyte.hasweather

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.hasbyte.hasweather.data.WeatherResponse
import com.hasbyte.hasweather.data.WeatherServiceAPI
import com.hasbyte.hasweather.utils.Constants
import okhttp3.Callback
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private val REQUEST_LOCATION_CODE = 123
    private var isDialogSettingShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    }

    override fun onResume() {
        super.onResume()
        // Cek status GPS dan Izin setiap kali pengguna kembali ke aplikasi
        if (!isDialogSettingShowing) {
            checkLocationAndPermissionState()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                requestLocationData()
            } else {
                // Pengguna menolak izin sistem
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()

                // Cek apakah penolakan ini bersifat permanen (Don't ask again)
                val showRationaleFine =
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
                val showRationaleCoarse = ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )

                if (!showRationaleFine && !showRationaleCoarse) {
                    // Jika keduanya FALSE setelah ditolak, berarti pengguna mengunci izin (Ditolak Permanen)
                    // BARU di sini kita tampilkan dialog untuk pergi ke App Settings HP
                    showGoToSettingsDialog()
                }
            }
        }
    }

    private fun checkLocationAndPermissionState() {
        if (!isLocationEnabled()) {
            showGPSDisabledDialog()
        } else {
            //cek apakah izin sudah di dapatkan sebelumnya
            if (isPermissionGranted()) {
                requestLocationData()
            } else {
                requestPermission()
            }
        }
    }

    private fun isPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }


    @SuppressLint("MissingPermission")
    fun requestLocationData() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(500) // interval tercepat jika ada aplikasi lain yang minta lokasi
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    Toast.makeText(
                        this@MainActivity,
                        "latitude: ${location.latitude} longitude: ${location.longitude}",
                        Toast.LENGTH_SHORT
                    ).show()
                    //passing latitude and longitude
                    getLocationWeatherDetails(location.latitude, location.longitude)
                    mFusedLocationClient.removeLocationUpdates(this)
                }

            }
        }
        mFusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

    }

    private fun getLocationWeatherDetails(lat: Double, lon: Double) {
        if (Constants.isNetworkAvailable(this)) {
            val retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val serviceApi = retrofit.create(WeatherServiceAPI::class.java)
            val call = serviceApi.getWeatherDetails(
                lat, lon, Constants.API_KEY,
                Constants.METRIC_UNIT
            )
            call.enqueue(object: retrofit2.Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse?>,
                    response: Response<WeatherResponse?>
                ) {
                    if(response.isSuccessful){
                        val weather = response.body()


                        Toast.makeText(this@MainActivity, "$weather", Toast.LENGTH_SHORT).show()

//                        Log.d("WEATHER RESPONSE", weather.toString())

                    } else {
                        Toast.makeText(this@MainActivity, "Something went wrong ${response.code().toString()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(
                    call: Call<WeatherResponse?>,
                    t: Throwable
                ) {
                    Toast.makeText(this@MainActivity, "Error ${t.toString()}", Toast.LENGTH_SHORT).show()
                }

            })
//            Toast.makeText(this, "There is internet connection", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "There is no internet connection", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun requestPermission() {
        val showRationaleFine =
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val showRationaleCoarse =
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        // 1. Cek apakah salah satu dari izin lokasi pernah ditolak sebelumnya
        if (showRationaleFine || showRationaleCoarse) {
            // Tampilkan dialog
            showRationaleDialog()
        } else {

            triggerSystemPermissionRequest()
        }
    }

    /* DIALOG */
    private fun triggerSystemPermissionRequest() {
        //dialog permission bawaan android
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            REQUEST_LOCATION_CODE
        )
    }

    private fun showRationaleDialog(): AlertDialog {
        val builder = AlertDialog.Builder(this)
            .setTitle("Location Permission Needed")
            .setMessage("This app requires location permission to access this app.")
            .setPositiveButton("Continue") { _, _ ->
                triggerSystemPermissionRequest()

            }.setNegativeButton("Close") { dialog, _ ->
                dialog.cancel()
            }
            .setCancelable(false)
            .show()
        return builder
    }

    private fun showGPSDisabledDialog() {
        AlertDialog.Builder(this)
            .setTitle("GPS Tidak Aktif")
            .setMessage("Aplikasi ini membutuhkan GPS yang aktif untuk mencari data cuaca di lokasi Anda. Silakan aktifkan GPS Anda.")
            .setPositiveButton("Aktifkan") { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Keluar") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showGoToSettingsDialog() {
        isDialogSettingShowing = true
        AlertDialog.Builder(this)
            .setTitle("Izin Diblokir Permanen")
            .setMessage("Anda telah menolak izin lokasi secara permanen. Silakan aktifkan izin secara manual di Pengaturan Aplikasi agar fitur cuaca dapat digunakan.")
            .setPositiveButton("Ke Pengaturan") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.cancel()
                Toast.makeText(this, "Fitur cuaca tidak dapat digunakan tanpa lokasi", Toast.LENGTH_LONG).show()
            }
            .setCancelable(false)
            .show()
    }
}