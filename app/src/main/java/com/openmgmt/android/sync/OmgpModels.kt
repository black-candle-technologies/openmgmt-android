package com.openmgmt.android.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * OpenMGMT sync protocol (omgp/1) messages, mirroring
 * openmgmt-protocol (crates/openmgmt-protocol/src/{sync,auth,error}.rs).
 */
const val PROTOCOL_VERSION = "omgp/1"

@Serializable
data class ProtocolError(
    val code: String,
    val message: String,
    val retryable: Boolean = false,
)

@Serializable
data class HelloRequest(
    @SerialName("protocol_version") val protocolVersion: String = PROTOCOL_VERSION,
    @SerialName("client_name") val clientName: String = "OpenMGMT Android",
    @SerialName("client_version") val clientVersion: String? = null,
    @SerialName("device_id") val deviceId: String? = null,
)

@Serializable
data class HelloResponse(
    @SerialName("protocol_version") val protocolVersion: String,
    @SerialName("server_name") val serverName: String = "",
    @SerialName("server_version") val serverVersion: String? = null,
    val compatible: Boolean,
    val error: ProtocolError? = null,
)

@Serializable
data class RegisterRequest(
    @SerialName("protocol_version") val protocolVersion: String = PROTOCOL_VERSION,
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String,
    /** Proof of possession when re-registering an existing device id. */
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
data class AuthContext(
    @SerialName("account_id") val accountId: String?,
    @SerialName("user_id") val userId: String?,
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_token") val deviceToken: String?,
)

@Serializable
data class SyncEvent(
    @SerialName("event_id") val eventId: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("actor_user_id") val actorUserId: String? = null,
    @SerialName("target_user_id") val targetUserId: String? = null,
    @SerialName("workspace_id") val workspaceId: String? = null,
    val sequence: Long,
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String,
    val operation: String,
    @SerialName("payload_json") val payloadJson: JsonObject,
    @SerialName("created_at") val createdAt: String,
    @SerialName("synced_at") val syncedAt: String? = null,
)

@Serializable
data class PushRequest(
    @SerialName("protocol_version") val protocolVersion: String = PROTOCOL_VERSION,
    val auth: AuthContext,
    @SerialName("base_checkpoint") val baseCheckpoint: String?,
    val events: List<SyncEvent>,
)

@Serializable
data class RejectedSyncEvent(
    @SerialName("event_id") val eventId: String,
    val error: ProtocolError,
)

@Serializable
data class PushResponse(
    @SerialName("accepted_event_ids") val acceptedEventIds: List<String> = emptyList(),
    @SerialName("rejected_events") val rejectedEvents: List<RejectedSyncEvent> = emptyList(),
    @SerialName("server_checkpoint") val serverCheckpoint: String = "",
    val error: ProtocolError? = null,
)

@Serializable
data class PullRequest(
    @SerialName("protocol_version") val protocolVersion: String = PROTOCOL_VERSION,
    val auth: AuthContext,
    @SerialName("after_checkpoint") val afterCheckpoint: String?,
    val limit: Int? = null,
)

@Serializable
data class PullResponse(
    val events: List<SyncEvent> = emptyList(),
    @SerialName("server_checkpoint") val serverCheckpoint: String = "",
    @SerialName("has_more") val hasMore: Boolean = false,
    val error: ProtocolError? = null,
)
