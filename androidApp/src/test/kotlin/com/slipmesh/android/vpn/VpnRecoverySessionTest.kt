package com.slipmesh.android.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnRecoverySessionTest {

    @Test
    fun connectedIntentWithPermissionRecoversOnlyOnce() {
        val store =
            InMemoryVpnConnectionIntentStore(
                VpnConnectionIntent.CONNECTED
            )

        val session =
            VpnRecoverySession(store)

        assertEquals(
            VpnRecoveryAction.REQUEST_CONNECT,
            session.recover(
                permissionGranted = true
            ),
        )

        assertEquals(
            VpnRecoveryAction.NONE,
            session.recover(
                permissionGranted = true
            ),
        )
    }

    @Test
    fun connectedIntentWithoutPermissionRequestsPermissionOnlyOnce() {
        val store =
            InMemoryVpnConnectionIntentStore(
                VpnConnectionIntent.CONNECTED
            )

        val session =
            VpnRecoverySession(store)

        assertEquals(
            VpnRecoveryAction.REQUEST_PERMISSION,
            session.recover(
                permissionGranted = false
            ),
        )

        assertEquals(
            VpnRecoveryAction.NONE,
            session.recover(
                permissionGranted = false
            ),
        )
    }

    @Test
    fun disconnectedIntentRemainsInert() {
        val store =
            InMemoryVpnConnectionIntentStore(
                VpnConnectionIntent.DISCONNECTED
            )

        val session =
            VpnRecoverySession(store)

        assertEquals(
            VpnRecoveryAction.NONE,
            session.recover(
                permissionGranted = true
            ),
        )

        assertEquals(
            VpnRecoveryAction.NONE,
            session.recover(
                permissionGranted = false
            ),
        )
    }

    @Test
    fun storeChangeAfterConsumptionCannotTriggerSameSessionRecovery() {
        val store =
            InMemoryVpnConnectionIntentStore(
                VpnConnectionIntent.DISCONNECTED
            )

        val session =
            VpnRecoverySession(store)

        assertEquals(
            VpnRecoveryAction.NONE,
            session.recover(
                permissionGranted = true
            ),
        )

        store.write(
            VpnConnectionIntent.CONNECTED
        )

        assertEquals(
            VpnRecoveryAction.NONE,
            session.recover(
                permissionGranted = true
            ),
        )
    }

    @Test
    fun newSessionMayRecoverPersistedConnectedIntentAgain() {
        val store =
            InMemoryVpnConnectionIntentStore(
                VpnConnectionIntent.CONNECTED
            )

        val firstSession =
            VpnRecoverySession(store)

        val secondSession =
            VpnRecoverySession(store)

        assertEquals(
            VpnRecoveryAction.REQUEST_CONNECT,
            firstSession.recover(
                permissionGranted = true
            ),
        )

        assertEquals(
            VpnRecoveryAction.REQUEST_CONNECT,
            secondSession.recover(
                permissionGranted = true
            ),
        )
    }
}
