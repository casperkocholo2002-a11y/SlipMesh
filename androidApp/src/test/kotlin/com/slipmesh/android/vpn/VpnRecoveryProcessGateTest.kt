package com.slipmesh.android.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnRecoveryProcessGateTest {

    @Test
    fun suppressionBlocksConnectedIntentRecovery() {
        val store =
            InMemoryVpnConnectionIntentStore(
                VpnConnectionIntent.CONNECTED
            )

        val runtime =
            VpnRecoveryProcessRuntime()

        runtime.suppressRecovery()

        assertEquals(
            VpnRecoveryAction.NONE,
            runtime.recover(
                store = store,
                permissionGranted = true,
            ),
        )

        assertEquals(
            VpnRecoveryAction.NONE,
            runtime.recover(
                store = store,
                permissionGranted = false,
            ),
        )
    }

    @Test
    fun suppressionIsProcessLocal() {
        val store =
            InMemoryVpnConnectionIntentStore(
                VpnConnectionIntent.CONNECTED
            )

        val failedClearProcess =
            VpnRecoveryProcessRuntime()

        failedClearProcess
            .suppressRecovery()

        assertEquals(
            VpnRecoveryAction.NONE,
            failedClearProcess.recover(
                store = store,
                permissionGranted = true,
            ),
        )

        val freshProcess =
            VpnRecoveryProcessRuntime()

        assertEquals(
            VpnRecoveryAction.REQUEST_CONNECT,
            freshProcess.recover(
                store = store,
                permissionGranted = true,
            ),
        )
    }

    @Test
    fun suppressionDoesNotPretendDurableStoreWasCleared() {
        val store =
            InMemoryVpnConnectionIntentStore(
                VpnConnectionIntent.CONNECTED
            )

        val runtime =
            VpnRecoveryProcessRuntime()

        runtime.suppressRecovery()

        assertEquals(
            VpnConnectionIntent.CONNECTED,
            store.read(),
        )
    }
}
