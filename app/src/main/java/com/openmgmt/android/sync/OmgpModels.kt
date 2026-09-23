package com.openmgmt.android.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Sync protocol version spoken by the server. */
const val PROTOCOL_VERSION = "omgp/1"

/** POST /omgp/v1/hello */
@Serializable
data class HelloRequest(
    @SerialName("protocol_version") val protocolVersion: String = PROTOCOL_VERSION,
    @SerialName("client_name") val clientName: String = "OpenMGMT Android",
    @SerialName("client_version") val clientVersion: String? = null,
    @SerialName("device_id") val deviceId: String? = null,
)

/** POST /omgp/v1/devices/register */
@Serializable
data class RegisterRequest(
    @SerialName("protocol_version") val protocolVersion: String = PROTOCOL_VERSION,
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("previous_device_token") val previousDeviceToken: String? = null,
    @SerialName("user_hint") val userHint: String? = null,
)

@Serializable
data class RegisterResponse(
    val accepted: Boolean,
    @SerialName("account_id") val accountId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("device_token") val deviceToken: String? = null,
    val error: ProtocolError? = null,
)

@Serializable
data class ProtocolError(
    val code: String,
    val message: String,
    val retryable: Boolean = false,
)

/** POST /omgp/v1/sync/push and /omgp/v1/sync/pull share the event envelope. */
@Serializable
data class SyncEvent(
    val id: String,
    @SerialName("device_id") val deviceId: String,
    val kind: String,
    val payload: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class PushRequest(
    @SerialName("protocol_version") val protocolVersion: String = PROTOCOL_VERSION,
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_token") val deviceToken: String,
    val events: List<SyncEvent> = emptyList(),
)

@Serializable
data class PullRequest(
    @SerialName("protocol_version") val protocolVersion: String = PROTOCOL_VERSION,
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_token") val deviceToken: String,
    @SerialName("since_cursor") val sinceCursor: Long = 0,
)

@Serializable
data class PullResponse(
    val events: List<SyncEvent> = emptyList(),
    @SerialName("next_cursor") val nextCursor: Long = 0,
)
