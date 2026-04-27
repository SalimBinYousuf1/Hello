package com.iamhere.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import org.libsodium.jni.NaCl
import org.libsodium.jni.Sodium
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("crypto_prefs", Context.MODE_PRIVATE)

    init {
        NaCl.sodium()
        ensureAesKey()
    }

    fun generateKeyPair(): Pair<ByteArray, ByteArray> {
        val pub = ByteArray(32)
        val sec = ByteArray(32)
        Sodium.crypto_box_keypair(pub, sec)
        return pub to sec
    }

    fun getOrCreateIdentityKeyPair(): Pair<ByteArray, ByteArray> {
        val pubStored = prefs.getString(PUB_KEY, null)
        val secEnc = prefs.getString(PRIV_KEY_ENC, null)
        val iv = prefs.getString(PRIV_IV, null)
        if (pubStored != null && secEnc != null && iv != null) {
            val pub = Base64.decode(pubStored, Base64.NO_WRAP)
            val privateKey = decryptWithKeystore(Base64.decode(secEnc, Base64.NO_WRAP), Base64.decode(iv, Base64.NO_WRAP))
            return pub to privateKey
        }
        val pair = generateKeyPair()
        val encrypted = encryptWithKeystore(pair.second)
        prefs.edit()
            .putString(PUB_KEY, Base64.encodeToString(pair.first, Base64.NO_WRAP))
            .putString(PRIV_KEY_ENC, Base64.encodeToString(encrypted.first, Base64.NO_WRAP))
            .putString(PRIV_IV, Base64.encodeToString(encrypted.second, Base64.NO_WRAP))
            .apply()
        return pair
    }

    fun resetIdentity() {
        prefs.edit().clear().apply()
    }

    fun seal(message: String, recipientPubKey: String): ByteArray {
        val recipient = Base64.decode(recipientPubKey, Base64.NO_WRAP)
        val plain = message.toByteArray(StandardCharsets.UTF_8)
        val cipher = ByteArray(plain.size + Sodium.crypto_box_sealbytes())
        Sodium.crypto_box_seal(cipher, plain, plain.size.toLong(), recipient)
        return cipher
    }

    fun open(encrypted: ByteArray, myPublicKey: ByteArray, myPrivateKey: ByteArray): String? {
        if (encrypted.size <= Sodium.crypto_box_sealbytes()) return null
        val plain = ByteArray(encrypted.size - Sodium.crypto_box_sealbytes())
        val rc = Sodium.crypto_box_seal_open(plain, encrypted, encrypted.size.toLong(), myPublicKey, myPrivateKey)
        return if (rc == 0) String(plain, StandardCharsets.UTF_8) else null
    }

    fun sign(data: ByteArray, myPrivateKey: ByteArray): ByteArray {
        val signature = ByteArray(64)
        val outLen = LongArray(1)
        Sodium.crypto_sign_detached(signature, outLen, data, data.size.toLong(), myPrivateKey)
        return signature
    }

    fun verify(data: ByteArray, signature: ByteArray, senderPubKey: String): Boolean {
        val pub = Base64.decode(senderPubKey, Base64.NO_WRAP)
        return Sodium.crypto_sign_verify_detached(signature, data, data.size.toLong(), pub) == 0
    }

    private fun ensureAesKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) return
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build()
        )
        generator.generateKey()
    }

    private fun encryptWithKeystore(raw: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getKeystoreKey())
        return cipher.doFinal(raw) to cipher.iv
    }

    private fun decryptWithKeystore(enc: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = javax.crypto.spec.GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getKeystoreKey(), spec)
        return cipher.doFinal(enc)
    }

    private fun getKeystoreKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return ks.getKey(KEY_ALIAS, null) as SecretKey
    }

    companion object {
        private const val KEY_ALIAS = "iamhere_aes_key"
        private const val PUB_KEY = "identity_pub"
        private const val PRIV_KEY_ENC = "identity_priv"
        private const val PRIV_IV = "identity_iv"
    }
}
