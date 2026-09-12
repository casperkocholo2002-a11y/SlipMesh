package com.slipmesh.android.vpn

class VpnRecoverySession(
    private val store: VpnConnectionIntentStore,
) {

    private var consumed =
        false

    @Synchronized
    fun recover(
        permissionGranted: Boolean,
    ): VpnRecoveryAction {

        if (consumed) {
            return VpnRecoveryAction.NONE
        }

        consumed = true

        return VpnRecoveryReconciler.reconcile(
            intent = store.read(),
            permissionGranted = permissionGranted,
        )
    }
}
