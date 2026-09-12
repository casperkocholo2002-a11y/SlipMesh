package com.slipmesh.android.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VpnRecoveryLifecycleBridgeTest {

    @Test
    fun noneIsInert() {
        assertNull(
            VpnRecoveryLifecycleBridge.transitionFor(
                VpnRecoveryAction.NONE
            )
        )
    }

    @Test
    fun requestConnectUsesNormalStartingPath() {
        val transition =
            VpnRecoveryLifecycleBridge.transitionFor(
                VpnRecoveryAction.REQUEST_CONNECT
            )

        assertEquals(
            VpnSessionState.STARTING,
            transition?.state,
        )

        assertEquals(
            listOf(
                VpnLifecycleCommand.START_SERVICE
            ),
            transition?.commands,
        )
    }

    @Test
    fun requestPermissionUsesWaitingPath() {
        val transition =
            VpnRecoveryLifecycleBridge.transitionFor(
                VpnRecoveryAction.REQUEST_PERMISSION
            )

        assertEquals(
            VpnSessionState.WAITING_FOR_PERMISSION,
            transition?.state,
        )

        assertEquals(
            listOf(
                VpnLifecycleCommand.REQUEST_PERMISSION
            ),
            transition?.commands,
        )
    }

    @Test
    fun recoveredPermissionGrantStartsService() {
        val waiting =
            VpnRecoveryLifecycleBridge.transitionFor(
                VpnRecoveryAction.REQUEST_PERMISSION
            )

        val granted =
            VpnLifecycleReducer.reduce(
                current =
                    waiting!!.state,
                event =
                    VpnLifecycleEvent.PermissionResult(
                        granted = true
                    ),
            )

        assertEquals(
            VpnSessionState.STARTING,
            granted.state,
        )

        assertEquals(
            listOf(
                VpnLifecycleCommand.START_SERVICE
            ),
            granted.commands,
        )
    }

    @Test
    fun recoveredPermissionDenialReturnsIdle() {
        val waiting =
            VpnRecoveryLifecycleBridge.transitionFor(
                VpnRecoveryAction.REQUEST_PERMISSION
            )

        val denied =
            VpnLifecycleReducer.reduce(
                current =
                    waiting!!.state,
                event =
                    VpnLifecycleEvent.PermissionResult(
                        granted = false
                    ),
            )

        assertEquals(
            VpnSessionState.IDLE,
            denied.state,
        )

        assertEquals(
            emptyList<VpnLifecycleCommand>(),
            denied.commands,
        )
    }

    @Test
    fun denialRecorderClearsDurableIntent() {
        val store =
            InMemoryVpnConnectionIntentStore(
                VpnConnectionIntent.CONNECTED
            )

        val recorder =
            VpnRecoveryIntentRecorder(
                store
            )

        recorder.recordPermissionDenied()

        assertEquals(
            VpnConnectionIntent.DISCONNECTED,
            store.read(),
        )
    }
}
