package com.slipmesh.android.vpn

class VpnRecoveryIntentRecorder(
    private val store: VpnConnectionIntentStore,
) {

    fun recordConnectRequested(): Boolean =
        store.write(
            VpnConnectionIntent.CONNECTED
        )

    fun recordExplicitDisconnect(): Boolean =
        store.write(
            VpnConnectionIntent.DISCONNECTED
        )

    fun recordPermissionDenied(): Boolean =
        store.write(
            VpnConnectionIntent.DISCONNECTED
        )

    fun recordPermissionRevoked(): Boolean =
        store.write(
            VpnConnectionIntent.DISCONNECTED
        )
}
