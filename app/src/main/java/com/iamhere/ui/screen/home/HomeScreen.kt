package com.iamhere.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iamhere.ui.components.NetworkStatusBanner
import com.iamhere.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onChat: (String) -> Unit,
    onContacts: () -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val status = vm.networkStatus.value.name

    Scaffold(
        topBar = {
            Column {
                NetworkStatusBanner(status)
                androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth()) {
                    TextButton(onClick = onContacts) { Text("Contacts") }
                    TextButton(onClick = onSettings) { Text("Settings") }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onChat("self") }) { Text("+") }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (vm.messages.value.isEmpty()) {
                Card(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(
                        "No peers nearby. Try sending a message to yourself to test encryption!",
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            LazyColumn {
                items(vm.messages.value.groupBy { it.senderId }.toList()) { (sender, msgs) ->
                    val preview = msgs.lastOrNull()?.content.orEmpty()
                    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).clickable { onChat(sender) }) {
                        Column(Modifier.padding(12.dp)) {
                            Text(sender)
                            Text(preview, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
