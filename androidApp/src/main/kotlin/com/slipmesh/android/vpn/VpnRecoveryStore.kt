package com.slipmesh.android.vpn

interface VpnConnectionIntentStore {

    fun read():
        VpnConnectionIntent

    fun write(
        intent: VpnConnectionIntent,
    ): Boolean
}

object VpnConnectionIntentCodec {

    fun encode(
        intent: VpnConnectionIntent,
    ): String =
        when (intent) {
            VpnConnectionIntent.CONNECTED ->
                CONNECTED_VALUE

            VpnConnectionIntent.DISCONNECTED ->
                DISCONNECTED_VALUE
        }

    fun decode(
        rawValue: String?,
    ): VpnConnectionIntent =
        when (rawValue) {
            CONNECTED_VALUE ->
                VpnConnectionIntent.CONNECTED

            DISCONNECTED_VALUE ->
                VpnConnectionIntent.DISCONNECTED

            else ->
                VpnConnectionIntent.DISCONNECTED
        }

    private const val CONNECTED_VALUE =
        "connected"

    private const val DISCONNECTED_VALUE =
        "disconnected"
}

class InMemoryVpnConnectionIntentStore(
    initialIntent: VpnConnectionIntent =
        VpnConnectionIntent.DISCONNECTED,
) : VpnConnectionIntentStore {

    private var currentIntent =
        initialIntent

    override fun read():
        VpnConnectionIntent =
        currentIntent

    override fun write(
        intent: VpnConnectionIntent,
    ): Boolean {
        currentIntent = intent

        return true
    }
}
