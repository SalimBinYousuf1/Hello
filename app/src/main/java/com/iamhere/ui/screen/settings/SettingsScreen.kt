package com.iamhere.ui.screen.settings

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val battery = remember { mutableStateOf(prefs.getBoolean("battery_saver", false)) }
    val autoDelete = remember { mutableStateOf(prefs.getBoolean("auto_delete_24h", false)) }
    val autoStart = remember { mutableStateOf(prefs.getBoolean("auto_start", true)) }

    Column(Modifier.padding(16.dp)) {
        Text("Battery Saver Mode")
        Switch(checked = battery.value, onCheckedChange = {
            battery.value = it
            prefs.edit().putBoolean("battery_saver", it).apply()
        })

        Text("Auto-Delete Messages after 24h", modifier = Modifier.padding(top = 12.dp))
        Switch(checked = autoDelete.value, onCheckedChange = {
            autoDelete.value = it
            prefs.edit().putBoolean("auto_delete_24h", it).apply()
        })

        Text("Auto-Start on Boot", modifier = Modifier.padding(top = 12.dp))
        Switch(checked = autoStart.value, onCheckedChange = {
            autoStart.value = it
            prefs.edit().putBoolean("auto_start", it).apply()
        })

        Text("Export/Import Database: available from device file manager for app data backup.", modifier = Modifier.padding(top = 16.dp))
        Text("Open Source Licenses: Nearby, Room, Compose, Libsodium, ZXing, CameraX, ML Kit.", modifier = Modifier.padding(top = 8.dp))
    }
}
