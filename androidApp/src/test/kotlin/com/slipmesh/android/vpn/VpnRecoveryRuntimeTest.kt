package com.slipmesh.android.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnRecoveryRuntimeTest {

    @Test
    fun repeatedActivityFacingCallsRecoverOnlyOncePerRuntime() {
        val store =
            InMemoryVpnConnectionIntentStore(
                VpnConnectionIntent.CONNECTED
            )

        assertEquals(
            VpnRecoveryAction.REQUEST_CONNECT,
            VpnRecoveryRuntime.recover(
                store = store,
                permissionGranted = true,
            ),
        )

        assertEquals(
            VpnRecoveryAction.NONE,
            VpnRecoveryRuntime.recover(
                store = store,
                permissionGranted = true,
            ),
        )
    }
}
