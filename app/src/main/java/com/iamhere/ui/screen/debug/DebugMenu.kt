package com.iamhere.ui.screen.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.iamhere.data.local.AppDatabase
import com.iamhere.ui.viewmodel.ContactViewModel
import kotlinx.coroutines.launch

@Composable
fun DebugMenu(database: AppDatabase, contactViewModel: ContactViewModel) {
    if (!com.iamhere.BuildConfig.DEBUG) return
    val scope = rememberCoroutineScope()
    Column {
        Button(onClick = { scope.launch { database.clearAllTables() } }) { Text("Clear DB") }
        Button(onClick = { contactViewModel.resetKeys() }) { Text("Reset Keys") }
    }
}
