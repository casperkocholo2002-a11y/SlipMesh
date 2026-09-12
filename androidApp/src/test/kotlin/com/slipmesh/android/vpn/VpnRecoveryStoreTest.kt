package com.slipmesh.android.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnRecoveryStoreTest {

    @Test
    fun codecRoundTripsConnectedIntent() {
        assertEquals(
            VpnConnectionIntent.CONNECTED,
            VpnConnectionIntentCodec.decode(
                VpnConnectionIntentCodec.encode(
                    VpnConnectionIntent.CONNECTED
                )
            ),
        )
    }

    @Test
    fun codecRoundTripsDisconnectedIntent() {
        assertEquals(
            VpnConnectionIntent.DISCONNECTED,
            VpnConnectionIntentCodec.decode(
                VpnConnectionIntentCodec.encode(
                    VpnConnectionIntent.DISCONNECTED
                )
            ),
        )
    }

    @Test
    fun missingValueFailsClosedToDisconnected() {
        assertEquals(
            VpnConnectionIntent.DISCONNECTED,
            VpnConnectionIntentCodec.decode(
                null
            ),
        )
    }

    @Test
    fun unknownValueFailsClosedToDisconnected() {
        assertEquals(
            VpnConnectionIntent.DISCONNECTED,
            VpnConnectionIntentCodec.decode(
                "corrupt-value"
            ),
        )
    }

    @Test
    fun inMemoryStoreDefaultsToDisconnected() {
        val store =
            InMemoryVpnConnectionIntentStore()

        assertEquals(
            VpnConnectionIntent.DISCONNECTED,
            store.read(),
        )
    }

    @Test
    fun inMemoryStorePersistsConnectedIntent() {
        val store =
            InMemoryVpnConnectionIntentStore()

        store.write(
            VpnConnectionIntent.CONNECTED
        )

        assertEquals(
            VpnConnectionIntent.CONNECTED,
            store.read(),
        )
    }

    @Test
    fun explicitDisconnectOverwritesConnectedIntent() {
        val store =
            InMemoryVpnConnectionIntentStore(
                initialIntent =
                    VpnConnectionIntent.CONNECTED
            )

        store.write(
            VpnConnectionIntent.DISCONNECTED
        )

        assertEquals(
            VpnConnectionIntent.DISCONNECTED,
            store.read(),
        )
    }
}

// STEP 15J durable-write regression proof.
class VpnRecoveryStoreWriteResultTest {

    @Test
    fun inMemoryWriteReportsSuccess() {
        val store =
            InMemoryVpnConnectionIntentStore()

        assertEquals(
            true,
            store.write(
                VpnConnectionIntent.CONNECTED
            ),
        )

        assertEquals(
            VpnConnectionIntent.CONNECTED,
            store.read(),
        )
    }
}
