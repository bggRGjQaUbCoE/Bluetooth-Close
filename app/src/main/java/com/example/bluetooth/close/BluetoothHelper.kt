package com.example.bluetooth.close

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import com.example.bluetooth.close.MainActivity.Companion.TAG

/**
 * Created by bggRGjQaUbCoE on 2024/6/25
 */
object BluetoothHelper {

    var connectedDevice: BluetoothDevice? = null

    fun disconnectDevice(context: Context) {
        if (connectedDevice == null) return
        try {
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter

            val serviceListener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    val disconnectMethod = proxy.javaClass.getMethod(
                        "disconnect",
                        BluetoothDevice::class.java
                    )
                    disconnectMethod.invoke(proxy, connectedDevice)
                    bluetoothAdapter.closeProfileProxy(profile, proxy)
                }

                override fun onServiceDisconnected(profile: Int) {}
            }

            bluetoothAdapter.getProfileProxy(context, serviceListener, BluetoothProfile.HEADSET)
            bluetoothAdapter.getProfileProxy(context, serviceListener, BluetoothProfile.A2DP)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect device", e)
        }
    }
}


