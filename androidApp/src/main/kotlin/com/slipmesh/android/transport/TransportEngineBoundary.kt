package com.slipmesh.android.transport

/**
 * Android-to-transport lifecycle seam.
 *
 * Concrete VLESS/ECH/other transports are deliberately outside P2-WP01.
 */
interface TransportEngineBoundary {

    fun start()

    fun stop()
}
