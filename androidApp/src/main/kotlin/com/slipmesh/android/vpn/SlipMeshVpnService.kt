package com.slipmesh.android.vpn

import android.net.VpnService

/**
 * Android lifecycle shell only.
 *
 * P2-WP01 must not establish a VPN interface, open a transport,
 * or contact any production endpoint.
 */
class SlipMeshVpnService : VpnService() {

    override fun onRevoke() {
        stopSelf()
        super.onRevoke()
    }
}
