package com.slipmesh.android.transport

import com.slipmesh.core.ConnectionObservation

/**
 * Android-to-transport lifecycle seam.
 *
 * Transport implementations report raw connectivity observations only.
 * Failure classification, route selection and failover remain owned by
 * the qualified Phase 1 core.
 */
interface TransportEngineBoundary {

    fun start(
        profile: LegacyTransportProfile,
        observationSink: TransportObservationSink,
    ): TransportStartResult

    fun stop(): TransportStopResult
}

fun interface TransportObservationSink {

    fun onObservation(
        observation: ConnectionObservation,
    )
}

sealed interface TransportStartResult {

    data class Started(
        val sessionId: String,
    ) : TransportStartResult

    data class Rejected(
        val reason: TransportStartRejection,
    ) : TransportStartResult
}

enum class TransportStartRejection {
    INVALID_CONFIGURATION,
    ALREADY_RUNNING,
}

enum class TransportStopResult {
    STOPPED,
    ALREADY_STOPPED,
}
