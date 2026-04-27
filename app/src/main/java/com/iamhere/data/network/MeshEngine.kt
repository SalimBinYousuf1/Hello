package com.iamhere.data.network

import android.content.Context
import android.os.PowerManager
import android.util.Base64
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.iamhere.data.local.MessageDao
import com.iamhere.data.local.MessageEntity
import com.iamhere.data.local.PacketDao
import com.iamhere.data.local.PacketEntity
import com.iamhere.data.proto.KeyExchange
import com.iamhere.data.proto.MeshPacket
import com.iamhere.data.proto.SyncRequest
import com.iamhere.data.proto.SyncResponse
import com.iamhere.data.proto.TextMessage
import com.iamhere.data.security.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packetDao: PacketDao,
    private val messageDao: MessageDao,
    private val cryptoManager: CryptoManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client: ConnectionsClient = Nearby.getConnectionsClient(context)

    private val identity = cryptoManager.getOrCreateIdentityKeyPair()
    private val myPublicKey = identity.first
    private val myPrivateKey = identity.second
    private val myPubString = Base64.encodeToString(myPublicKey, Base64.NO_WRAP)

    private val _connectedPeers = MutableStateFlow<Map<String, String>>(emptyMap())
    val connectedPeers: StateFlow<Map<String, String>> = _connectedPeers

    private val _networkStatus = MutableStateFlow("Searching")
    val networkStatus: StateFlow<String> = _networkStatus

    private val serviceId = "com.iamhere.mesh"

    init {
        startDiscoveryAndAdvertising()
        scope.launch { periodicSyncLoop() }
    }

    fun startDiscoveryAndAdvertising() {
        val strategy = Strategy.P2P_CLUSTER
        client.startAdvertising(
            UUID.randomUUID().toString(),
            serviceId,
            connectionLifecycle,
            AdvertisingOptions.Builder().setStrategy(strategy).build()
        )
        client.startDiscovery(
            serviceId,
            endpointDiscovery,
            DiscoveryOptions.Builder().setStrategy(strategy).build()
        )
        _networkStatus.value = if (_connectedPeers.value.isEmpty()) "Searching" else "Connected to ${_connectedPeers.value.size} peers"
    }

    fun myPublicKeyBase64(): String = myPubString

    fun resetKeys() {
        cryptoManager.resetIdentity()
    }

    private val endpointDiscovery = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            client.requestConnection(UUID.randomUUID().toString(), endpointId, connectionLifecycle)
        }

        override fun onEndpointLost(endpointId: String) {
            _connectedPeers.value = _connectedPeers.value - endpointId
            _networkStatus.value = if (_connectedPeers.value.isEmpty()) "Offline" else "Connected to ${_connectedPeers.value.size} peers"
        }
    }

    private val connectionLifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            client.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            if (resolution.status.isSuccess) {
                performHandshake(endpointId)
                scope.launch { sendSyncRequest(endpointId) }
            } else {
                _networkStatus.value = "Searching"
            }
        }

        override fun onDisconnected(endpointId: String) {
            _connectedPeers.value = _connectedPeers.value - endpointId
            _networkStatus.value = if (_connectedPeers.value.isEmpty()) "Offline" else "Connected to ${_connectedPeers.value.size} peers"
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes ->
                scope.launch { processIncomingPacket(bytes, endpointId) }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private fun performHandshake(endpointId: String) {
        val signature = cryptoManager.sign(myPublicKey, myPrivateKey)
        val packet = MeshPacket.newBuilder()
            .setPacketId(UUID.randomUUID().toString())
            .setSenderPubKey(myPubString)
            .setTimestamp(System.currentTimeMillis())
            .setTtl(5)
            .setKeyExchange(
                KeyExchange.newBuilder()
                    .setPublicKey(com.google.protobuf.ByteString.copyFrom(myPublicKey))
                    .setSignature(com.google.protobuf.ByteString.copyFrom(signature))
                    .build()
            )
            .build()
        client.sendPayload(endpointId, Payload.fromBytes(packet.toByteArray()))
    }

    suspend fun sendMessage(text: String, recipientKey: String) {
        val target = recipientKey.ifBlank { myPubString }
        val encrypted = cryptoManager.seal(text, target)
        val packet = MeshPacket.newBuilder()
            .setPacketId(UUID.randomUUID().toString())
            .setSenderPubKey(myPubString)
            .setTimestamp(System.currentTimeMillis())
            .setTtl(5)
            .setText(
                TextMessage.newBuilder()
                    .setEncryptedContent(com.google.protobuf.ByteString.copyFrom(encrypted))
                    .setRecipientPubKey(recipientKey)
                    .build()
            )
            .build()

        packetDao.insert(PacketEntity(packet.packetId, packet.toByteArray(), packet.ttl, packet.timestamp))
        messageDao.insert(
            MessageEntity(
                senderId = "me",
                content = text,
                timestamp = packet.timestamp,
                isRead = true,
                isVerified = true
            )
        )

        if (recipientKey.isBlank() || recipientKey == myPubString) {
            cryptoManager.open(encrypted, myPublicKey, myPrivateKey)?.let { decrypted ->
                messageDao.insert(
                    MessageEntity(
                        senderId = myPubString,
                        content = decrypted,
                        timestamp = System.currentTimeMillis(),
                        isRead = false,
                        isVerified = true
                    )
                )
            }
        }

        broadcastPacket(packet.toByteArray(), null)
    }

    private suspend fun processIncomingPacket(bytes: ByteArray, fromEndpoint: String) {
        try {
            val packet = MeshPacket.parseFrom(bytes)

            if (packetDao.getById(packet.packetId) != null) return
            if (packet.ttl <= 0) return

            packetDao.insert(PacketEntity(packet.packetId, bytes, packet.ttl, packet.timestamp))

            when (packet.payloadTypeCase) {
                MeshPacket.PayloadTypeCase.KEY_EXCHANGE -> {
                    val valid = cryptoManager.verify(
                        packet.keyExchange.publicKey.toByteArray(),
                        packet.keyExchange.signature.toByteArray(),
                        packet.senderPubKey
                    )
                    if (valid) {
                        _connectedPeers.value = _connectedPeers.value + (fromEndpoint to packet.senderPubKey)
                        _networkStatus.value = "Connected to ${_connectedPeers.value.size} peers"
                    }
                }

                MeshPacket.PayloadTypeCase.TEXT -> {
                    val recipient = packet.text.recipientPubKey
                    if (recipient.isBlank() || recipient == myPubString) {
                        val clear = cryptoManager.open(
                            packet.text.encryptedContent.toByteArray(),
                            myPublicKey,
                            myPrivateKey
                        )
                        if (!clear.isNullOrBlank()) {
                            messageDao.insert(
                                MessageEntity(
                                    senderId = packet.senderPubKey,
                                    content = clear,
                                    timestamp = packet.timestamp,
                                    isRead = false,
                                    isVerified = true
                                )
                            )
                        }
                    }
                }

                MeshPacket.PayloadTypeCase.SYNC_REQ -> {
                    handleSyncRequest(fromEndpoint, packet.syncReq)
                }

                MeshPacket.PayloadTypeCase.SYNC_RES -> {
                    packet.syncRes.missingPacketsList.forEach { missingPacket ->
                        processIncomingPacket(missingPacket.toByteArray(), fromEndpoint)
                    }
                }

                else -> Unit
            }

            val forwarded = packet.toBuilder().setTtl(packet.ttl - 1).build()
            broadcastPacket(forwarded.toByteArray(), fromEndpoint)
        } catch (_: Exception) {
            // drop malformed or undecryptable packet silently
        }
    }

    private suspend fun handleSyncRequest(endpointId: String, req: SyncRequest) {
        val known = req.knownPacketIdsList.toSet()
        val mine = packetDao.getAllIds()
        val missingIds = mine.filterNot { known.contains(it) }
        val missingPackets = packetDao.getByIds(missingIds)
            .mapNotNull { runCatching { MeshPacket.parseFrom(it.rawBytes) }.getOrNull() }

        val responsePacket = MeshPacket.newBuilder()
            .setPacketId(UUID.randomUUID().toString())
            .setSenderPubKey(myPubString)
            .setTimestamp(System.currentTimeMillis())
            .setTtl(2)
            .setSyncRes(SyncResponse.newBuilder().addAllMissingPackets(missingPackets))
            .build()

        client.sendPayload(endpointId, Payload.fromBytes(responsePacket.toByteArray()))
    }

    private suspend fun sendSyncRequest(endpointId: String) {
        val sync = MeshPacket.newBuilder()
            .setPacketId(UUID.randomUUID().toString())
            .setSenderPubKey(myPubString)
            .setTimestamp(System.currentTimeMillis())
            .setTtl(2)
            .setSyncReq(SyncRequest.newBuilder().addAllKnownPacketIds(packetDao.getAllIds()))
            .build()

        client.sendPayload(endpointId, Payload.fromBytes(sync.toByteArray()))
    }

    private suspend fun periodicSyncLoop() {
        while (true) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val intervalMs = if (powerManager.isInteractive) 5_000L else 30_000L
            delay(intervalMs)
            if (_connectedPeers.value.isEmpty()) {
                startDiscoveryAndAdvertising()
            } else {
                _connectedPeers.value.keys.forEach { endpointId -> sendSyncRequest(endpointId) }
            }
            packetDao.deleteOld(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        }
    }

    private fun broadcastPacket(bytes: ByteArray, excludeEndpoint: String?) {
        _connectedPeers.value.keys
            .filter { it != excludeEndpoint }
            .forEach { endpointId ->
                client.sendPayload(endpointId, Payload.fromBytes(bytes))
            }
    }
}
