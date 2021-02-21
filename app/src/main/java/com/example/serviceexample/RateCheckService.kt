package com.example.serviceexample

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.math.BigDecimal


class RateCheckService : Service() {
    val handler = Handler(Looper.getMainLooper())
    var rateCheckAttempt = 0
    lateinit var startRate: BigDecimal
    lateinit var targetRate: BigDecimal
    val rateCheckInteractor = RateCheckInteractor()

    val rateCheckRunnable: Runnable = Runnable {
        rateCheckAttempt++

        Log.d(TAG, "rateCheckAttempt = $rateCheckAttempt")
        if (rateCheckAttempt > RATE_CHECK_ATTEMPTS_MAX) {
            stopSelf()
            Log.d(TAG, "max attempts count reached, stopping service")
            return@Runnable
        }

        requestAndCheckRate()
    }

    private fun requestAndCheckRate() {
        GlobalScope.launch(Dispatchers.Main) {
            val rate = rateCheckInteractor.requestRate()
            Log.d(TAG, "new rate = $rate")
            val rateBigDecimal = BigDecimal(rate)
            if ((startRate >= targetRate && rateBigDecimal <= targetRate) ||
                (startRate < targetRate && rateBigDecimal >= targetRate)
            ) {
                sendNotification(rate)
                stopSelf()
            } else {
                handler.postDelayed(rateCheckRunnable, RATE_CHECK_INTERVAL)
            }
        }
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startRate = BigDecimal(intent?.getStringExtra(ARG_START_RATE))
        targetRate = BigDecimal(intent?.getStringExtra(ARG_TARGET_RATE))

        Log.d(TAG, "onStartCommand startRate = $startRate targetRate = $targetRate")

        handler.post(rateCheckRunnable)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(rateCheckRunnable)
    }

    fun sendNotification(rate: String) {
        createNotificationChannel()

        val title = getString(R.string.notification_text, rate)

        Log.d(TAG, "send notification: $title")

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        builder
            .setContentTitle(title)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)

        val notificationManagerCompat = NotificationManagerCompat.from(this)
        notificationManagerCompat.notify(0, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManagerCompat = NotificationManagerCompat.from(this)
            if (notificationManagerCompat.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                val name = "USD Rate"
                //val description = getString(R.string.channel_description)
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance)
                notificationManagerCompat.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        const val TAG = "RateCheckService"
        const val NOTIFICATION_CHANNEL_ID = "usd_rate"
        const val RATE_CHECK_INTERVAL = 5000L
        const val RATE_CHECK_ATTEMPTS_MAX = 100

        const val ARG_START_RATE = "ARG_START_RATE"
        const val ARG_TARGET_RATE = "ARG_TARGET_RATE"

        fun startService(context: Context, startRate: String, targetRate: String) {
            context.startService(Intent(context, RateCheckService::class.java).apply {
                putExtra(ARG_START_RATE, startRate)
                putExtra(ARG_TARGET_RATE, targetRate)
            })
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, RateCheckService::class.java))
        }
    }
}