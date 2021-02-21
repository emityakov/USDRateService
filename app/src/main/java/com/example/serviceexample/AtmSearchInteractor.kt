package com.example.serviceexample

import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AtmSearchInteractor {
    val networkClient = NetworkClient()

    suspend fun requestPlaceSearch(location: Location): String {
        return withContext<String>(Dispatchers.IO) {
            val url = MainViewModel.NEARBY_PLACES_URL.format(
                BuildConfig.PLACES_API_KEY,
                "${location.latitude},${location.longitude}"
            )
            val result = networkClient.request(url)
            if (!result.isNullOrEmpty()) {
                parseNearestAtm(result)
            } else {
                ""
            }
        }
    }

    private fun parseNearestAtm(jsonString: String): String {
        val results = JSONObject(jsonString).getJSONArray("results")
        if (results.length() > 0) {
            val firstResult = results.getJSONObject(0)
            val name = firstResult.getString("name")
            val address = firstResult.getString("vicinity")
            return "$name, $address"
        }
        return ""
    }
}