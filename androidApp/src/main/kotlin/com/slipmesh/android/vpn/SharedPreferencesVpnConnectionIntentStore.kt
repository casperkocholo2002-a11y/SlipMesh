package com.slipmesh.android.vpn

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesVpnConnectionIntentStore(
    private val preferences: SharedPreferences,
) : VpnConnectionIntentStore {

    override fun read():
        VpnConnectionIntent =
        VpnConnectionIntentCodec.decode(
            preferences.getString(
                KEY_CONNECTION_INTENT,
                null,
            )
        )

    override fun write(
        intent: VpnConnectionIntent,
    ): Boolean =
        preferences
            .edit()
            .putString(
                KEY_CONNECTION_INTENT,
                VpnConnectionIntentCodec.encode(
                    intent
                ),
            )
            .commit()

    companion object {

        private const val PREFERENCES_NAME =
            "slipmesh_vpn_recovery"

        private const val KEY_CONNECTION_INTENT =
            "connection_intent"

        fun create(
            context: Context,
        ): SharedPreferencesVpnConnectionIntentStore =
            SharedPreferencesVpnConnectionIntentStore(
                context.getSharedPreferences(
                    PREFERENCES_NAME,
                    Context.MODE_PRIVATE,
                )
            )
    }
}
