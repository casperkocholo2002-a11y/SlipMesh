package com.slipmesh.android.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnRecoveryTest {

    @Test
    fun disconnectedIntentNeverStartsRecovery() {
        assertEquals(
            VpnRecoveryAction.NONE,
            VpnRecoveryReconciler.reconcile(
                intent =
                    VpnConnectionIntent.DISCONNECTED,
                permissionGranted = true,
            ),
        )

        assertEquals(
            VpnRecoveryAction.NONE,
            VpnRecoveryReconciler.reconcile(
                intent =
                    VpnConnectionIntent.DISCONNECTED,
                permissionGranted = false,
            ),
        )
    }

    @Test
    fun connectedIntentWithPermissionRequestsFreshConnect() {
        assertEquals(
            VpnRecoveryAction.REQUEST_CONNECT,
            VpnRecoveryReconciler.reconcile(
                intent =
                    VpnConnectionIntent.CONNECTED,
                permissionGranted = true,
            ),
        )
    }

    @Test
    fun connectedIntentWithoutPermissionRequestsPermission() {
        assertEquals(
            VpnRecoveryAction.REQUEST_PERMISSION,
            VpnRecoveryReconciler.reconcile(
                intent =
                    VpnConnectionIntent.CONNECTED,
                permissionGranted = false,
            ),
        )
    }

    @Test
    fun repeatedReconciliationIsDeterministic() {
        val first =
            VpnRecoveryReconciler.reconcile(
                intent =
                    VpnConnectionIntent.CONNECTED,
                permissionGranted = true,
            )

        val second =
            VpnRecoveryReconciler.reconcile(
                intent =
                    VpnConnectionIntent.CONNECTED,
                permissionGranted = true,
            )

        assertEquals(first, second)
    }

    @Test
    fun permissionRevocationCannotRestoreRuntimeState() {
        val action =
            VpnRecoveryReconciler.reconcile(
                intent =
                    VpnConnectionIntent.CONNECTED,
                permissionGranted = false,
            )

        assertEquals(
            VpnRecoveryAction.REQUEST_PERMISSION,
            action,
        )
    }
}
