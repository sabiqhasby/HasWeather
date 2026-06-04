package com.hasbyte.hasweather.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants {
    const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
    const val API_KEY = "e780044175929f338a4c017c422405fd"
    const val METRIC_UNIT = "metric"
    fun isNetworkAvailable(context: Context): Boolean {
        // Get an instance of ConnectivityManager using context parameter
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        //cek if the device is API level 23 or higher
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // get currently active network and return false if none
            val network = connectivityManager.activeNetwork ?: return false

            // get capabilities of the active network and return false if none
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            // check the type network, return true if it WIFI, CELLULAR, or ETHERNET
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        }else{
            // for API below 23 get active network info and return true if it is connected or connecting
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }
}