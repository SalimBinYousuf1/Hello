package com.iamhere

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.iamhere.service.MeshForegroundService
import com.iamhere.ui.navigation.AppNavHost
import com.iamhere.ui.theme.IAmHereTheme
import com.iamhere.ui.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, MeshForegroundService::class.java))
        } else {
            startService(Intent(this, MeshForegroundService::class.java))
        }
        val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
        launcher.launch(requiredPermissions().toTypedArray())

        setContent {
            IAmHereTheme {
                val vm: HomeViewModel = hiltViewModel()
                vm.networkStatus.collectAsState()
                LaunchedEffect(Unit) { vm.start() }
                AppNavHost()
            }
        }
    }

    private fun requiredPermissions(): List<String> {
        val base = mutableListOf(Manifest.permission.FOREGROUND_SERVICE)
        if (Build.VERSION.SDK_INT >= 31) {
            base += listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            base += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= 33) base += Manifest.permission.POST_NOTIFICATIONS
        return base
    }
}
