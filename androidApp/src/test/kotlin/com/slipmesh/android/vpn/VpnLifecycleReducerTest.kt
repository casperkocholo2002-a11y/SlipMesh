package com.slipmesh.android.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnLifecycleReducerTest {

    @Test
    fun unpreparedConnectRequestsPermission() {
        val result = VpnLifecycleReducer.reduce(
            VpnSessionState.IDLE,
            VpnLifecycleEvent.ConnectRequested(
                permissionAlreadyGranted = false,
            ),
        )

        assertEquals(
            VpnSessionState.WAITING_FOR_PERMISSION,
            result.state,
        )

        assertEquals(
            listOf(
                VpnLifecycleCommand.REQUEST_PERMISSION
            ),
            result.commands,
        )
    }

    @Test
    fun preparedConnectStartsService() {
        val result = VpnLifecycleReducer.reduce(
            VpnSessionState.IDLE,
            VpnLifecycleEvent.ConnectRequested(
                permissionAlreadyGranted = true,
            ),
        )

        assertEquals(
            VpnSessionState.STARTING,
            result.state,
        )

        assertEquals(
            listOf(
                VpnLifecycleCommand.START_SERVICE
            ),
            result.commands,
        )
    }

    @Test
    fun grantedPermissionStartsService() {
        val result = VpnLifecycleReducer.reduce(
            VpnSessionState.WAITING_FOR_PERMISSION,
            VpnLifecycleEvent.PermissionResult(
                granted = true,
            ),
        )

        assertEquals(
            VpnSessionState.STARTING,
            result.state,
        )

        assertEquals(
            listOf(
                VpnLifecycleCommand.START_SERVICE
            ),
            result.commands,
        )
    }

    @Test
    fun deniedPermissionReturnsIdleWithoutStart() {
        val result = VpnLifecycleReducer.reduce(
            VpnSessionState.WAITING_FOR_PERMISSION,
            VpnLifecycleEvent.PermissionResult(
                granted = false,
            ),
        )

        assertEquals(
            VpnSessionState.IDLE,
            result.state,
        )

        assertEquals(
            emptyList<VpnLifecycleCommand>(),
            result.commands,
        )
    }

    @Test
    fun cancelledPermissionWaitReturnsIdle() {
        val result = VpnLifecycleReducer.reduce(
            VpnSessionState.WAITING_FOR_PERMISSION,
            VpnLifecycleEvent.DisconnectRequested,
        )

        assertEquals(
            VpnSessionState.IDLE,
            result.state,
        )
    }

    @Test
    fun serviceStartedMovesToRunning() {
        val result = VpnLifecycleReducer.reduce(
            VpnSessionState.STARTING,
            VpnLifecycleEvent.ServiceStarted,
        )

        assertEquals(
            VpnSessionState.RUNNING,
            result.state,
        )
    }

    @Test
    fun runningDisconnectRequestsStop() {
        val result = VpnLifecycleReducer.reduce(
            VpnSessionState.RUNNING,
            VpnLifecycleEvent.DisconnectRequested,
        )

        assertEquals(
            VpnSessionState.STOPPING,
            result.state,
        )

        assertEquals(
            listOf(
                VpnLifecycleCommand.STOP_SERVICE
            ),
            result.commands,
        )
    }

    @Test
    fun startingDisconnectRequestsStop() {
        val result = VpnLifecycleReducer.reduce(
            VpnSessionState.STARTING,
            VpnLifecycleEvent.DisconnectRequested,
        )

        assertEquals(
            VpnSessionState.STOPPING,
            result.state,
        )

        assertEquals(
            listOf(
                VpnLifecycleCommand.STOP_SERVICE
            ),
            result.commands,
        )
    }

    @Test
    fun serviceStoppedReturnsIdle() {
        val result = VpnLifecycleReducer.reduce(
            VpnSessionState.STOPPING,
            VpnLifecycleEvent.ServiceStopped,
        )

        assertEquals(
            VpnSessionState.IDLE,
            result.state,
        )
    }

    @Test
    fun revokeRunningReturnsIdleAndStopsService() {
        val result = VpnLifecycleReducer.reduce(
            VpnSessionState.RUNNING,
            VpnLifecycleEvent.PermissionRevoked,
        )

        assertEquals(
            VpnSessionState.IDLE,
            result.state,
        )

        assertEquals(
            listOf(
                VpnLifecycleCommand.STOP_SERVICE
            ),
            result.commands,
        )
    }

    @Test
    fun irrelevantEventDoesNotChangeIdleState() {
        val result = VpnLifecycleReducer.reduce(
            VpnSessionState.IDLE,
            VpnLifecycleEvent.ServiceStarted,
        )

        assertEquals(
            VpnSessionState.IDLE,
            result.state,
        )

        assertEquals(
            emptyList<VpnLifecycleCommand>(),
            result.commands,
        )
    }
    @Test
    fun duplicateServiceStoppedWhileIdleIsIdempotent() {
        val result = VpnLifecycleReducer.reduce(
            VpnSessionState.IDLE,
            VpnLifecycleEvent.ServiceStopped,
        )

        assertEquals(
            VpnSessionState.IDLE,
            result.state,
        )

        assertEquals(
            emptyList<VpnLifecycleCommand>(),
            result.commands,
        )
    }

    @Test
    fun duplicatePermissionRevokedWhileIdleIsIdempotent() {
        val result = VpnLifecycleReducer.reduce(
            VpnSessionState.IDLE,
            VpnLifecycleEvent.PermissionRevoked,
        )

        assertEquals(
            VpnSessionState.IDLE,
            result.state,
        )

        assertEquals(
            emptyList<VpnLifecycleCommand>(),
            result.commands,
        )
    }

}
