package com.slipmesh.android.transport

import com.slipmesh.core.TransportKind

/**
 * Non-secret legacy transport metadata.
 *
 * Authentication material is deliberately not part of this Phase 3 WP01
 * contract. Production endpoint values are also not committed here.
 */
data class LegacyTransportProfile(
    val routeId: String,
    val host: String,
    val port: Int,
    val websocketPath: String,
    val tlsServerName: String,
) {

    val transportKind:
        TransportKind
        get() =
            TransportKind.LEGACY_VLESS_WS_TLS
}

object LegacyTransportProfileValidator {

    fun isValid(
        profile: LegacyTransportProfile,
    ): Boolean =
        profile.routeId.isNotBlank() &&
            profile.host.isNotBlank() &&
            profile.port in 1..65535 &&
            profile.websocketPath.startsWith("/") &&
            profile.websocketPath.length > 1 &&
            profile.tlsServerName.isNotBlank()
}
