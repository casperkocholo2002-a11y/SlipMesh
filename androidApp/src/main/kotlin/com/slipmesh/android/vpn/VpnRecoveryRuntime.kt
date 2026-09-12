package com.slipmesh.android.vpn

class VpnRecoveryProcessRuntime {

    private var session:
        VpnRecoverySession? = null

    private var recoverySuppressed =
        false

    @Synchronized
    fun recover(
        store: VpnConnectionIntentStore,
        permissionGranted: Boolean,
    ): VpnRecoveryAction {

        if (recoverySuppressed) {
            return VpnRecoveryAction.NONE
        }

        val activeSession =
            session
                ?: VpnRecoverySession(store)
                    .also {
                        session = it
                    }

        return activeSession.recover(
            permissionGranted =
                permissionGranted
        )
    }

    @Synchronized
    fun suppressRecovery() {
        recoverySuppressed = true
    }
}

object VpnRecoveryRuntime {

    private val processRuntime =
        VpnRecoveryProcessRuntime()

    fun recover(
        store: VpnConnectionIntentStore,
        permissionGranted: Boolean,
    ): VpnRecoveryAction =
        processRuntime.recover(
            store = store,
            permissionGranted =
                permissionGranted,
        )

    fun suppressRecoveryForProcess() {
        processRuntime.suppressRecovery()
    }
}
