package com.slipmesh.android.vpn

object VpnRecoveryLifecycleBridge {

    fun transitionFor(
        action: VpnRecoveryAction,
    ): VpnLifecycleTransition? =
        when (action) {

            VpnRecoveryAction.NONE ->
                null

            VpnRecoveryAction.REQUEST_PERMISSION ->
                VpnLifecycleReducer.reduce(
                    current =
                        VpnSessionState.IDLE,
                    event =
                        VpnLifecycleEvent.ConnectRequested(
                            permissionAlreadyGranted =
                                false,
                        ),
                )

            VpnRecoveryAction.REQUEST_CONNECT ->
                VpnLifecycleReducer.reduce(
                    current =
                        VpnSessionState.IDLE,
                    event =
                        VpnLifecycleEvent.ConnectRequested(
                            permissionAlreadyGranted =
                                true,
                        ),
                )
        }
}
