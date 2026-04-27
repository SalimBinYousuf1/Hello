package com.iamhere.ui.screen.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iamhere.ui.components.MessageBubble
import com.iamhere.ui.viewmodel.HomeViewModel

@Composable
fun ChatScreen(vm: HomeViewModel = hiltViewModel()) {
    val text = remember { mutableStateOf("") }
    val status = vm.networkStatus.value?.name ?: "Offline"

    Column {
        if (status != "Connected") {
            Text(
                text = "Will send when peer found",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        LazyColumn(Modifier.weight(1f)) {
            items(vm.messages.value) { msg ->
                MessageBubble(text = msg.content, me = msg.senderId == "me", verified = msg.isVerified)
            }
        }

        Row(Modifier.fillMaxWidth().padding(8.dp)) {
            OutlinedTextField(
                value = text.value,
                onValueChange = { text.value = it },
                modifier = Modifier.weight(1f),
                label = { Text("Type a secure message") }
            )
            Button(
                onClick = {
                    if (text.value.isNotBlank()) {
                        vm.send(text.value, "")
                        text.value = ""
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) { Text("Send") }
        }
    }
}
