package com.example.bluetooth.close

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.bluetooth.close.BluetoothHelper.connectedDevice
import com.example.bluetooth.close.BluetoothHelper.disconnectDevice
import com.example.bluetooth.close.MainActivity.Companion.HOUR
import com.example.bluetooth.close.MainActivity.Companion.MINUTE
import com.example.bluetooth.close.MainActivity.Companion.PERMISSION
import java.util.Timer
import java.util.TimerTask


/**
 * Created by bggRGjQaUbCoE on 2024/6/25
 */
class BluetoothService : Service() {

    private val timer = Timer()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ContextCompat.checkSelfPermission(this, PERMISSION) == 0) {
            connectedDevice?.let {
                val pref = getSharedPreferences("settings", MODE_PRIVATE)
                val hour = pref.getInt(HOUR, 0)
                val minute = pref.getInt(MINUTE, 30)
                val time = (hour * 3600 + minute * 60) * 1000L

                startForegroundService(hour, minute)

                timer.schedule(object : TimerTask() {
                    override fun run() {
                        try {
                            disconnectDevice(this@BluetoothService)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }, time)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService(hour: Int, minute: Int) {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(this, BluetoothReceiver::class.java)
        intent.setAction("STOP_SERVICE")
        val stopIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("LIMIT: ${if (hour != 0) "${hour}h" else ""}${minute}min")
            .addAction(R.mipmap.ic_launcher, "STOP", stopIntent)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        timer.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "TaskForegroundServiceChannel"
        const val CHANNEL_NAME = "Channel name"
        const val NOTIFICATION_ID = 1
    }

}