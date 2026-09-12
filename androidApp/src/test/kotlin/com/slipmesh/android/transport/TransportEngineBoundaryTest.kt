package com.slipmesh.android.transport

import com.slipmesh.core.ConnectionObservation
import com.slipmesh.core.ObservationLayer
import com.slipmesh.core.ObservationOutcome
import com.slipmesh.core.TransportKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TransportEngineBoundaryTest {

    @Test
    fun legacyProfileUsesQualifiedLegacyTransportKind() {
        assertEquals(
            TransportKind.LEGACY_VLESS_WS_TLS,
            validProfile().transportKind,
        )
    }

    @Test
    fun validProfileStartsDeterministically() {
        val engine =
            InMemoryLegacyTransportEngine()

        val result =
            engine.start(
                validProfile(),
                TransportObservationSink { },
            )

        assertEquals(
            TransportStartResult.Started(
                sessionId =
                    "legacy:test-route"
            ),
            result,
        )
    }

    @Test
    fun blankRouteIdFailsClosed() {
        val engine =
            InMemoryLegacyTransportEngine()

        val result =
            engine.start(
                validProfile().copy(
                    routeId = ""
                ),
                TransportObservationSink { },
            )

        assertEquals(
            TransportStartResult.Rejected(
                TransportStartRejection
                    .INVALID_CONFIGURATION
            ),
            result,
        )
    }

    @Test
    fun blankHostFailsClosed() {
        val engine =
            InMemoryLegacyTransportEngine()

        val result =
            engine.start(
                validProfile().copy(
                    host = ""
                ),
                TransportObservationSink { },
            )

        assertEquals(
            TransportStartResult.Rejected(
                TransportStartRejection
                    .INVALID_CONFIGURATION
            ),
            result,
        )
    }

    @Test
    fun invalidWebSocketPathFailsClosed() {
        val engine =
            InMemoryLegacyTransportEngine()

        val result =
            engine.start(
                validProfile().copy(
                    websocketPath = "ws"
                ),
                TransportObservationSink { },
            )

        assertEquals(
            TransportStartResult.Rejected(
                TransportStartRejection
                    .INVALID_CONFIGURATION
            ),
            result,
        )
    }

    @Test
    fun secondStartCannotReplaceRunningSession() {
        val engine =
            InMemoryLegacyTransportEngine()

        engine.start(
            validProfile(),
            TransportObservationSink { },
        )

        val second =
            engine.start(
                validProfile().copy(
                    routeId = "second-route"
                ),
                TransportObservationSink { },
            )

        assertEquals(
            TransportStartResult.Rejected(
                TransportStartRejection
                    .ALREADY_RUNNING
            ),
            second,
        )
    }

    @Test
    fun stopIsIdempotent() {
        val engine =
            InMemoryLegacyTransportEngine()

        engine.start(
            validProfile(),
            TransportObservationSink { },
        )

        assertEquals(
            TransportStopResult.STOPPED,
            engine.stop(),
        )

        assertEquals(
            TransportStopResult.ALREADY_STOPPED,
            engine.stop(),
        )
    }

    @Test
    fun rawObservationIsForwardedWithoutClassification() {
        var received:
            ConnectionObservation? = null

        val engine =
            InMemoryLegacyTransportEngine()

        engine.start(
            validProfile(),
            TransportObservationSink {
                received = it
            },
        )

        val observation =
            ConnectionObservation(
                layer =
                    ObservationLayer.TLS,
                outcome =
                    ObservationOutcome.TIMEOUT,
                timestampEpochMs = 1000L,
                terminal = false,
            )

        engine.emitObservation(
            observation
        )

        assertSame(
            observation,
            received,
        )
    }

    private fun validProfile():
        LegacyTransportProfile =
        LegacyTransportProfile(
            routeId = "test-route",
            host = "relay.example.invalid",
            port = 443,
            websocketPath = "/ws",
            tlsServerName =
                "relay.example.invalid",
        )

    /**
     * Test-only deterministic adapter.
     *
     * It performs no DNS, socket, TLS, WebSocket or VLESS activity.
     */
    private class InMemoryLegacyTransportEngine :
        TransportEngineBoundary {

        private var running = false

        private var sink:
            TransportObservationSink? = null

        override fun start(
            profile: LegacyTransportProfile,
            observationSink: TransportObservationSink,
        ): TransportStartResult {

            if (
                !LegacyTransportProfileValidator
                    .isValid(profile)
            ) {
                return TransportStartResult.Rejected(
                    TransportStartRejection
                        .INVALID_CONFIGURATION
                )
            }

            if (running) {
                return TransportStartResult.Rejected(
                    TransportStartRejection
                        .ALREADY_RUNNING
                )
            }

            running = true
            sink = observationSink

            return TransportStartResult.Started(
                sessionId =
                    "legacy:${profile.routeId}"
            )
        }

        override fun stop():
            TransportStopResult {

            if (!running) {
                return TransportStopResult
                    .ALREADY_STOPPED
            }

            running = false
            sink = null

            return TransportStopResult.STOPPED
        }

        fun emitObservation(
            observation: ConnectionObservation,
        ) {
            if (running) {
                sink?.onObservation(
                    observation
                )
            }
        }
    }
}
