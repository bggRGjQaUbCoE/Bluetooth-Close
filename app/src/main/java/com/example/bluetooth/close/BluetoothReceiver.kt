package com.example.bluetooth.close

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.bluetooth.close.BluetoothHelper.connectedDevice
import com.example.bluetooth.close.MainActivity.Companion.PERMISSION
import com.example.bluetooth.close.MainActivity.Companion.TAG

/**
 * Created by bggRGjQaUbCoE on 2024/6/25
 */
class BluetoothReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "STOP_SERVICE") {
            connectedDevice = null
            val serviceIntent = Intent(context, BluetoothService::class.java)
            context.stopService(serviceIntent)
        } else if (ContextCompat.checkSelfPermission(context, PERMISSION) == 0) {
            when (val action = intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val isConnected = action == BluetoothDevice.ACTION_ACL_CONNECTED
                    val device =
                        if (SDK_INT >= 33)
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice::class.java
                            )
                        else
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    Log.d(
                        TAG,
                        "Device ${if (isConnected) "connected" else "disconnected"}: ${device?.name} (${device?.address})"
                    )
                    if (isConnected) {
                        device?.let {
                            connectedDevice = device
                            val serviceIntent = Intent(context, BluetoothService::class.java)
                            context.startService(serviceIntent)
                        }
                    } else {
                        if (connectedDevice == device) {
                            connectedDevice = null
                            Log.i(TAG, "onReceive: stopService")
                            val serviceIntent = Intent(context, BluetoothService::class.java)
                            context.stopService(serviceIntent)
                        }
                    }
                }
            }
        }
    }

}