package com.example.serviceexample

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    lateinit var viewModel: MainViewModel
    lateinit var textRate: TextView
    lateinit var textTargetRate: EditText
    lateinit var textAtm: TextView
    lateinit var rootView: View

    private var cancellationTokenSource = CancellationTokenSource()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViewModel()
        initView()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancellationTokenSource.cancel()
    }

    fun initViewModel() {
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        viewModel.usdRate.observe(this, {
            textRate.text = "$it RUB"
        })
        viewModel.atmAddress.observe(this, {
            textAtm.text = it
        })

        viewModel.onCreate()
    }

    fun initView() {
        textRate = findViewById(R.id.textUsdRubRate)
        textTargetRate = findViewById(R.id.textTargetRate)
        textAtm = findViewById(R.id.textAtm)
        rootView = findViewById(R.id.rootView)

        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            viewModel.onRefreshClicked()
        }

        findViewById<Button>(R.id.btnSubscribeToRate).setOnClickListener {
            val targetRate = textTargetRate.text.toString()
            val startRate = viewModel.usdRate.value

            if (targetRate.isNotEmpty() && startRate?.isNotEmpty() == true) {
                RateCheckService.stopService(this)
                RateCheckService.startService(this, startRate, targetRate)
            } else if (targetRate.isEmpty()) {
                Snackbar.make(rootView, R.string.target_rate_empty, Snackbar.LENGTH_SHORT).show()
            } else if (startRate.isNullOrEmpty()) {
                Snackbar.make(rootView, R.string.current_rate_empty, Snackbar.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnFindAtm).setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                obtainLocationAndFindAtm()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQ_LOCATION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtainLocationAndFindAtm()
            } else {
                Snackbar.make(rootView, R.string.location_permission_error, Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun obtainLocationAndFindAtm() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient.getCurrentLocation(
            LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location: Location? ->
            Log.d(TAG, "location = $location")
            location?.let {
                viewModel.onFindAtm(location)
            } ?: run {
                Snackbar.make(rootView, R.string.location_resolving_error, Snackbar.LENGTH_SHORT)
                    .show()
            }
        }.addOnFailureListener {
            Log.d(TAG, "failure", it)
            Snackbar.make(
                rootView,
                R.string.location_resolving_error,
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        const val TAG = "MainActivity"
        const val REQ_LOCATION = 1
    }
}