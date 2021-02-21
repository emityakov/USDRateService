package com.example.serviceexample

import android.location.Location
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    val usdRate = MutableLiveData<String>()
    val atmAddress = MutableLiveData<String>()
    val rateCheckInteractor = RateCheckInteractor()
    val atmSearchInteractor = AtmSearchInteractor()

    fun onCreate() {
        refreshRate()
    }

    fun onRefreshClicked() {
        refreshRate()
    }

    fun onFindAtm(location: Location) {
        GlobalScope.launch(Dispatchers.Main) {
            val atmResult = atmSearchInteractor.requestPlaceSearch(location)
            Log.d(TAG, "atmResult = $atmResult")
            atmAddress.value = atmResult
        }
    }

    private fun refreshRate() {
        GlobalScope.launch(Dispatchers.Main) {
            val rate = rateCheckInteractor.requestRate()
            Log.d(TAG, "usdRate = $rate")

            usdRate.value = rate
        }
    }

    companion object {
        const val TAG = "MainViewModel"
        const val USD_RATE_URL = "https://www.freeforexapi.com/api/live?pairs=USDRUB"
        const val NEARBY_PLACES_URL =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json?key=%s&location=%s&rankby=distance&type=atm&language=ru"
    }
}