package com.iamhere.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iamhere.data.network.MeshEngine
import com.iamhere.domain.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val meshEngine: MeshEngine
) : ViewModel() {
    val contacts = contactRepository.contacts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val myPublicKey: String get() = meshEngine.myPublicKeyBase64()

    fun trust(pubKey: String, alias: String) {
        viewModelScope.launch { contactRepository.trust(pubKey, alias) }
    }

    fun resetKeys() {
        meshEngine.resetKeys()
    }
}
