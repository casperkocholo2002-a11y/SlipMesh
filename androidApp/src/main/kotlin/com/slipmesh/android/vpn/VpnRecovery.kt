package com.slipmesh.android.vpn

enum class VpnConnectionIntent {
    CONNECTED,
    DISCONNECTED,
}

enum class VpnRecoveryAction {
    NONE,
    REQUEST_PERMISSION,
    REQUEST_CONNECT,
}

object VpnRecoveryReconciler {

    fun reconcile(
        intent: VpnConnectionIntent,
        permissionGranted: Boolean,
    ): VpnRecoveryAction =
        when (intent) {
            VpnConnectionIntent.DISCONNECTED ->
                VpnRecoveryAction.NONE

            VpnConnectionIntent.CONNECTED ->
                if (permissionGranted) {
                    VpnRecoveryAction.REQUEST_CONNECT
                } else {
                    VpnRecoveryAction.REQUEST_PERMISSION
                }
        }
}
