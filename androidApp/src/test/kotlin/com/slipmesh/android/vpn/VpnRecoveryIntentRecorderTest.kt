package com.slipmesh.android.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnRecoveryIntentRecorderTest {

    @Test
    fun connectRequestRecordsConnectedIntent() {
        val store =
            InMemoryVpnConnectionIntentStore()

        val recorder =
            VpnRecoveryIntentRecorder(store)

        recorder.recordConnectRequested()

        assertEquals(
            VpnConnectionIntent.CONNECTED,
            store.read(),
        )
    }

    @Test
    fun explicitDisconnectRecordsDisconnectedIntent() {
        val store =
            InMemoryVpnConnectionIntentStore(
                VpnConnectionIntent.CONNECTED
            )

        val recorder =
            VpnRecoveryIntentRecorder(store)

        recorder.recordExplicitDisconnect()

        assertEquals(
            VpnConnectionIntent.DISCONNECTED,
            store.read(),
        )
    }

    @Test
    fun permissionDenialRecordsDisconnectedIntent() {
        val store =
            InMemoryVpnConnectionIntentStore(
                VpnConnectionIntent.CONNECTED
            )

        val recorder =
            VpnRecoveryIntentRecorder(store)

        recorder.recordPermissionDenied()

        assertEquals(
            VpnConnectionIntent.DISCONNECTED,
            store.read(),
        )
    }

    @Test
    fun permissionRevocationRecordsDisconnectedIntent() {
        val store =
            InMemoryVpnConnectionIntentStore(
                VpnConnectionIntent.CONNECTED
            )

        val recorder =
            VpnRecoveryIntentRecorder(store)

        recorder.recordPermissionRevoked()

        assertEquals(
            VpnConnectionIntent.DISCONNECTED,
            store.read(),
        )
    }
}

// STEP 15J recorder failure propagation proof.
class VpnRecoveryIntentRecorderFailureTest {

    private class FailingStore :
        VpnConnectionIntentStore {

        override fun read():
            VpnConnectionIntent =
            VpnConnectionIntent.CONNECTED

        override fun write(
            intent: VpnConnectionIntent,
        ): Boolean =
            false
    }

    @Test
    fun connectWriteFailureIsObservable() {
        val recorder =
            VpnRecoveryIntentRecorder(
                FailingStore()
            )

        assertEquals(
            false,
            recorder.recordConnectRequested(),
        )
    }

    @Test
    fun disconnectWriteFailureIsObservable() {
        val recorder =
            VpnRecoveryIntentRecorder(
                FailingStore()
            )

        assertEquals(
            false,
            recorder.recordExplicitDisconnect(),
        )
    }
}
