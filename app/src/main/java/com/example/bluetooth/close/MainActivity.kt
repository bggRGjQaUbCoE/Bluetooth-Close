package com.example.bluetooth.close

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bluetooth.close.BluetoothHelper.connectedDevice
import com.example.bluetooth.close.MainActivity.Companion.HOUR
import com.example.bluetooth.close.MainActivity.Companion.MINUTE
import com.example.bluetooth.close.MainActivity.Companion.PERMISSION
import com.example.bluetooth.close.ui.theme.BluetoothCloseTheme


/**
 * Created by bggRGjQaUbCoE on 2024/6/25
 */
class MainActivity : ComponentActivity() {

    private var requiredPermissions = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BluetoothCloseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(modifier = Modifier.fillMaxSize())
                }
            }
        }

        initRequiredPermissions()
        checkAndRequestPermissions()

    }

    private fun initRequiredPermissions() {
        requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)

        requiredPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (SDK_INT >= 33) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (SDK_INT >= 34) {
            requiredPermissions.add(Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE)
        }
    }

    private fun checkAndRequestPermissions() {
        requiredPermissions.removeIf {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (requiredPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, requiredPermissions.toTypedArray(), 1)
        }
    }

    companion object {
        const val TAG = "BluetoothConnection"
        const val HOUR = "HOUR"
        const val MINUTE = "MINUTE"
        const val PERMISSION = Manifest.permission.BLUETOOTH_CONNECT
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var showDevicesDialog by remember { mutableStateOf(false) }
    val pref = LocalContext.current.getSharedPreferences("settings", MODE_PRIVATE)
    val state = rememberTimePickerState(
        initialHour = pref.getInt(HOUR, 0),
        initialMinute = pref.getInt(MINUTE, 30),
    )
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(id = R.string.app_name))
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FilledTonalButton(
                onClick = { showTimePicker = true },
            ) {
                Text("Set Time")
            }
            FilledTonalButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            PERMISSION
                        ) == 0
                    ) {
                        showDevicesDialog = true
                    } else {
                        Toast.makeText(context, "permission denied", Toast.LENGTH_SHORT).show()
                    }
                },
            ) {
                Text("Connected Devices")
            }
        }
    }

    fun restartService() {
        val serviceIntent = Intent(context, BluetoothService::class.java)
        context.stopService(serviceIntent)
        context.startService(serviceIntent)
    }

    if (showTimePicker) {
        TimePickerDialog(
            onCancel = { showTimePicker = false },
            onConfirm = {
                pref.edit().putInt(HOUR, state.hour).apply()
                pref.edit().putInt(MINUTE, state.minute).apply()
                showTimePicker = false
                connectedDevice?.let {
                    restartService()
                }
            },
        ) {
            TimeInput(state = state)
        }
    }

    if (showDevicesDialog) {
        val devices =
            (context.getSystemService(
                Context.BLUETOOTH_SERVICE
            ) as BluetoothManager).adapter.bondedDevices.toList()
                .filter { it.javaClass.getMethod("isConnected").invoke(it) as Boolean }
        if (devices.isNotEmpty()) {
            DevicesDialog(
                onCancel = { showDevicesDialog = false },
                devices = devices,
            ) {
                showDevicesDialog = false
                connectedDevice = it
                restartService()
            }
        } else {
            showDevicesDialog = false
            Toast.makeText(context, "no connected device", Toast.LENGTH_SHORT)
                .show()
        }

    }

}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesDialog(
    title: String = "Connected Devices",
    devices: List<BluetoothDevice>?,
    onCancel: () -> Unit,
    onSelected: (BluetoothDevice) -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier =
            Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = title,
                )
                devices?.map { device ->
                    val isSelected = device == connectedDevice
                    ListItem(
                        modifier = Modifier.clickable {
                            if (!isSelected) {
                                onSelected(device)
                            } else {
                                onCancel()
                            }
                        },
                        headlineContent = {
                            Text(
                                device.name,
                                style = MaterialTheme.typography.labelMedium.copy(color = if (isSelected) MaterialTheme.colorScheme.outline else Color.Unspecified)
                            )
                        },
                        supportingContent = {
                            Text(
                                device.address,
                                style = MaterialTheme.typography.labelSmall.copy(color = if (isSelected) MaterialTheme.colorScheme.outline else Color.Unspecified)
                            )
                        },
                        trailingContent = {
                            if (isSelected) Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                            )
                        })
                }
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onCancel) { Text("Close") }
                }
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    title: String = "Select Time",
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    toggle: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier =
            Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = title,
                    style = MaterialTheme.typography.labelMedium
                )
                content()
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    toggle()
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onCancel) { Text("Cancel") }
                    TextButton(onClick = onConfirm) { Text("OK") }
                }
            }
        }
    }
}