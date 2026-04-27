package com.iamhere.ui.screen.contacts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iamhere.ui.components.QRCodeGenerator
import com.iamhere.ui.components.QRScanner
import com.iamhere.ui.viewmodel.ContactViewModel

@Composable
fun ContactsScreen(vm: ContactViewModel = hiltViewModel()) {
    val alias = remember { mutableStateOf("") }
    val scanned = remember { mutableStateOf<String?>(null) }
    val showMyQr = remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(value = alias.value, onValueChange = { alias.value = it }, label = { Text("Alias") })
        Button(onClick = {
            val key = scanned.value
            if (!key.isNullOrBlank() && alias.value.isNotBlank()) vm.trust(key, alias.value)
        }) { Text("Save Scanned Contact") }

        Card(Modifier.fillMaxWidth().height(220.dp)) {
            QRScanner(onRead = { scanned.value = it })
        }

        Text("Scanned Key: ${scanned.value ?: "No key yet"}")
        Button(onClick = { showMyQr.value = !showMyQr.value }) { Text(if (showMyQr.value) "Hide My QR" else "Show My QR") }
        if (showMyQr.value) QRCodeGenerator(content = vm.myPublicKey)

        Text("Trusted Contacts")
        LazyColumn {
            items(vm.contacts.value) { contact ->
                Text("${contact.alias} (${contact.pubKey.take(12)}...)")
            }
        }
    }
}
