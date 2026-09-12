package com.slipmesh.android.vpn

import android.os.ParcelFileDescriptor

fun interface TunHandle {
    fun close()
}

class ParcelFileDescriptorTunHandle(
    private val descriptor: ParcelFileDescriptor,
) : TunHandle {

    override fun close() {
        descriptor.close()
    }
}

/**
 * Owns at most one TUN handle.
 *
 * Ownership is cleared before close so close failures
 * cannot resurrect or retain stale ownership.
 *
 * No packet IO belongs here.
 */
class TunSession {

    private var handle: TunHandle? = null

    @Synchronized
    fun hasDescriptor(): Boolean =
        handle != null

    @Synchronized
    fun attach(
        newHandle: TunHandle,
    ): Boolean {

        if (handle != null) {
            return false
        }

        handle = newHandle

        return true
    }

    @Synchronized
    fun close() {
        val owned =
            handle
                ?: return

        handle = null

        try {
            owned.close()
        } catch (_: Exception) {
            // Ownership has already been cleared.
        }
    }
}
