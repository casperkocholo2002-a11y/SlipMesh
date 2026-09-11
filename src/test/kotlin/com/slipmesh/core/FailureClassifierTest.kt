package com.slipmesh.core

import kotlin.test.Test
import kotlin.test.assertEquals


class FailureClassifierTest {

    private fun observation(
        layer: ObservationLayer,
        outcome: ObservationOutcome,
        responseCode: Int? = null
    ) =
        ConnectionObservation(
            layer = layer,
            outcome = outcome,
            responseCode = responseCode,
            timestampEpochMs = 1000L
        )


    @Test
    fun `dns timeout maps to dns failure`() {

        assertEquals(
            FailureReason.DNS_FAILURE,
            FailureClassifier.classify(
                observation(
                    ObservationLayer.DNS,
                    ObservationOutcome.TIMEOUT
                )
            )
        )
    }


    @Test
    fun `tcp timeout maps correctly`() {

        assertEquals(
            FailureReason.TCP_TIMEOUT,
            FailureClassifier.classify(
                observation(
                    ObservationLayer.TCP,
                    ObservationOutcome.TIMEOUT
                )
            )
        )
    }


    @Test
    fun `tcp refused maps correctly`() {

        assertEquals(
            FailureReason.TCP_REFUSED,
            FailureClassifier.classify(
                observation(
                    ObservationLayer.TCP,
                    ObservationOutcome.REFUSED
                )
            )
        )
    }


    @Test
    fun `tls timeout remains distinct from tcp timeout`() {

        assertEquals(
            FailureReason.TLS_TIMEOUT,
            FailureClassifier.classify(
                observation(
                    ObservationLayer.TLS,
                    ObservationOutcome.TIMEOUT
                )
            )
        )
    }


    @Test
    fun `tls alert maps correctly`() {

        assertEquals(
            FailureReason.TLS_ALERT,
            FailureClassifier.classify(
                observation(
                    ObservationLayer.TLS,
                    ObservationOutcome.ALERT
                )
            )
        )
    }


    @Test
    fun `http failure maps correctly`() {

        assertEquals(
            FailureReason.HTTP_FAILURE,
            FailureClassifier.classify(
                observation(
                    ObservationLayer.HTTP,
                    ObservationOutcome.FAILURE
                )
            )
        )
    }


    @Test
    fun `http error status maps correctly`() {

        assertEquals(
            FailureReason.HTTP_FAILURE,
            FailureClassifier.classify(
                observation(
                    ObservationLayer.HTTP,
                    ObservationOutcome.SUCCESS,
                    responseCode = 503
                )
            )
        )
    }


    @Test
    fun `websocket 101 is success`() {

        assertEquals(
            FailureReason.NONE,
            FailureClassifier.classify(
                observation(
                    ObservationLayer.WEBSOCKET,
                    ObservationOutcome.SUCCESS,
                    responseCode = 101
                )
            )
        )
    }


    @Test
    fun `websocket non 101 is upgrade failure`() {

        assertEquals(
            FailureReason.WEBSOCKET_UPGRADE_FAILURE,
            FailureClassifier.classify(
                observation(
                    ObservationLayer.WEBSOCKET,
                    ObservationOutcome.SUCCESS,
                    responseCode = 404
                )
            )
        )
    }


    @Test
    fun `transport stall maps correctly`() {

        assertEquals(
            FailureReason.TRANSPORT_STALL,
            FailureClassifier.classify(
                observation(
                    ObservationLayer.TRANSPORT,
                    ObservationOutcome.STALL
                )
            )
        )
    }


    @Test
    fun `network change remains distinct`() {

        assertEquals(
            FailureReason.NETWORK_CHANGE,
            FailureClassifier.classify(
                observation(
                    ObservationLayer.NETWORK,
                    ObservationOutcome.CHANGED
                )
            )
        )
    }


    @Test
    fun `unsupported combination maps to unknown`() {

        assertEquals(
            FailureReason.UNKNOWN,
            FailureClassifier.classify(
                observation(
                    ObservationLayer.TLS,
                    ObservationOutcome.REFUSED
                )
            )
        )
    }
}
