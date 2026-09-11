package com.slipmesh.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class HealthStoreTest {

    @Test
    fun `unknown route returns default health`() {

        val store =
            HealthStore()

        val health =
            store.get("route-a")

        assertEquals(
            HealthState.UNKNOWN,
            health.state
        )

        assertEquals(
            0,
            health.consecutiveFailures
        )

        assertNull(
            health.lastSuccessEpochMs
        )
    }


    @Test
    fun `first failure degrades route`() {

        val store =
            HealthStore()

        val health =
            store.recordFailure(
                routeId = "route-a",
                reason = FailureReason.TLS_TIMEOUT,
                timestampEpochMs = 1000L
            )

        assertEquals(
            HealthState.DEGRADED,
            health.state
        )

        assertEquals(
            1,
            health.consecutiveFailures
        )

        assertEquals(
            FailureReason.TLS_TIMEOUT,
            health.lastFailure
        )
    }


    @Test
    fun `third consecutive failure makes route unreachable`() {

        val store =
            HealthStore()

        store.recordFailure(
            "route-a",
            FailureReason.TCP_TIMEOUT,
            1000L
        )

        store.recordFailure(
            "route-a",
            FailureReason.TCP_TIMEOUT,
            2000L
        )

        val health =
            store.recordFailure(
                "route-a",
                FailureReason.TCP_TIMEOUT,
                3000L
            )

        assertEquals(
            HealthState.UNREACHABLE,
            health.state
        )

        assertEquals(
            3,
            health.consecutiveFailures
        )
    }


    @Test
    fun `success restores healthy state and resets failures`() {

        val store =
            HealthStore()

        store.recordFailure(
            "route-a",
            FailureReason.TLS_TIMEOUT,
            1000L
        )

        store.recordFailure(
            "route-a",
            FailureReason.TLS_TIMEOUT,
            2000L
        )

        val health =
            store.recordSuccess(
                routeId = "route-a",
                timestampEpochMs = 3000L
            )

        assertEquals(
            HealthState.HEALTHY,
            health.state
        )

        assertEquals(
            0,
            health.consecutiveFailures
        )

        assertEquals(
            FailureReason.NONE,
            health.lastFailure
        )

        assertEquals(
            3000L,
            health.lastSuccessEpochMs
        )
    }


    @Test
    fun `network change does not increment route failure count`() {

        val store =
            HealthStore()

        store.recordFailure(
            "route-a",
            FailureReason.TLS_TIMEOUT,
            1000L
        )

        val health =
            store.recordFailure(
                "route-a",
                FailureReason.NETWORK_CHANGE,
                2000L
            )

        assertEquals(
            HealthState.UNKNOWN,
            health.state
        )

        assertEquals(
            1,
            health.consecutiveFailures
        )

        assertEquals(
            FailureReason.NETWORK_CHANGE,
            health.lastFailure
        )
    }


    @Test
    fun `health store never infers suspected block`() {

        val store =
            HealthStore()

        repeat(5) { index ->

            store.recordFailure(
                routeId = "route-a",
                reason = FailureReason.TLS_TIMEOUT,
                timestampEpochMs =
                    (index + 1) * 1000L
            )
        }

        assertFalse(
            store.snapshot()
                .values
                .any {
                    it.state ==
                        HealthState.SUSPECTED_BLOCK
                }
        )
    }


    @Test
    fun `record observation integrates classifier and store`() {

        val store =
            HealthStore()

        val observation =
            ConnectionObservation(
                layer = ObservationLayer.WEBSOCKET,
                outcome = ObservationOutcome.SUCCESS,
                responseCode = 404,
                timestampEpochMs = 4000L
            )

        val health =
            store.recordObservation(
                routeId = "route-a",
                observation = observation
            )

        assertEquals(
            HealthState.DEGRADED,
            health.state
        )

        assertEquals(
            FailureReason.WEBSOCKET_UPGRADE_FAILURE,
            health.lastFailure
        )
    }


    @Test
    fun `snapshot is isolated from future changes`() {

        val store =
            HealthStore()

        store.recordSuccess(
            "route-a",
            1000L
        )

        val snapshot =
            store.snapshot()

        store.recordFailure(
            "route-a",
            FailureReason.TCP_TIMEOUT,
            2000L
        )

        assertEquals(
            HealthState.HEALTHY,
            snapshot["route-a"]?.state
        )
    }

    @Test
    fun `intermediate dns success does not reset failure history`() {

        val store =
            HealthStore()

        store.recordFailure(
            routeId = "route-a",
            reason = FailureReason.TLS_TIMEOUT,
            timestampEpochMs = 1000L
        )

        val health =
            store.recordObservation(
                routeId = "route-a",
                observation =
                    ConnectionObservation(
                        layer = ObservationLayer.DNS,
                        outcome = ObservationOutcome.SUCCESS,
                        timestampEpochMs = 2000L,
                        terminal = false
                    )
            )

        assertEquals(
            HealthState.DEGRADED,
            health.state
        )

        assertEquals(
            1,
            health.consecutiveFailures
        )

        assertEquals(
            FailureReason.TLS_TIMEOUT,
            health.lastFailure
        )
    }


    @Test
    fun `intermediate tcp success does not mark unknown route healthy`() {

        val store =
            HealthStore()

        val health =
            store.recordObservation(
                routeId = "route-a",
                observation =
                    ConnectionObservation(
                        layer = ObservationLayer.TCP,
                        outcome = ObservationOutcome.SUCCESS,
                        timestampEpochMs = 1000L,
                        terminal = false
                    )
            )

        assertEquals(
            HealthState.UNKNOWN,
            health.state
        )

        assertEquals(
            0,
            health.consecutiveFailures
        )
    }


    @Test
    fun `terminal success restores route health`() {

        val store =
            HealthStore()

        store.recordFailure(
            routeId = "route-a",
            reason = FailureReason.TLS_TIMEOUT,
            timestampEpochMs = 1000L
        )

        store.recordFailure(
            routeId = "route-a",
            reason = FailureReason.TLS_TIMEOUT,
            timestampEpochMs = 2000L
        )

        val health =
            store.recordObservation(
                routeId = "route-a",
                observation =
                    ConnectionObservation(
                        layer = ObservationLayer.TRANSPORT,
                        outcome = ObservationOutcome.SUCCESS,
                        timestampEpochMs = 3000L,
                        terminal = true
                    )
            )

        assertEquals(
            HealthState.HEALTHY,
            health.state
        )

        assertEquals(
            0,
            health.consecutiveFailures
        )

        assertEquals(
            FailureReason.NONE,
            health.lastFailure
        )

        assertEquals(
            3000L,
            health.lastSuccessEpochMs
        )
    }


    @Test
    fun `stale failure cannot override newer success`() {

        val store =
            HealthStore()

        store.recordSuccess(
            routeId = "route-a",
            timestampEpochMs = 3000L
        )

        val health =
            store.recordFailure(
                routeId = "route-a",
                reason = FailureReason.TLS_TIMEOUT,
                timestampEpochMs = 2000L
            )

        assertEquals(
            HealthState.HEALTHY,
            health.state
        )

        assertEquals(
            0,
            health.consecutiveFailures
        )

        assertEquals(
            3000L,
            health.lastSuccessEpochMs
        )

        assertNull(
            health.lastFailureEpochMs
        )
    }


    @Test
    fun `stale terminal success cannot erase newer failure`() {

        val store =
            HealthStore()

        store.recordFailure(
            routeId = "route-a",
            reason = FailureReason.TLS_TIMEOUT,
            timestampEpochMs = 3000L
        )

        val health =
            store.recordObservation(
                routeId = "route-a",
                observation =
                    ConnectionObservation(
                        layer = ObservationLayer.TRANSPORT,
                        outcome = ObservationOutcome.SUCCESS,
                        timestampEpochMs = 2000L,
                        terminal = true
                    )
            )

        assertEquals(
            HealthState.DEGRADED,
            health.state
        )

        assertEquals(
            1,
            health.consecutiveFailures
        )

        assertEquals(
            FailureReason.TLS_TIMEOUT,
            health.lastFailure
        )

        assertEquals(
            3000L,
            health.lastFailureEpochMs
        )
    }


    @Test
    fun `stale network change cannot overwrite newer healthy state`() {

        val store =
            HealthStore()

        store.recordSuccess(
            routeId = "route-a",
            timestampEpochMs = 5000L
        )

        val health =
            store.recordFailure(
                routeId = "route-a",
                reason = FailureReason.NETWORK_CHANGE,
                timestampEpochMs = 4000L
            )

        assertEquals(
            HealthState.HEALTHY,
            health.state
        )

        assertEquals(
            FailureReason.NONE,
            health.lastFailure
        )

        assertEquals(
            5000L,
            health.lastSuccessEpochMs
        )
    }


    @Test
    fun `newer failure after success is accepted`() {

        val store =
            HealthStore()

        store.recordSuccess(
            routeId = "route-a",
            timestampEpochMs = 1000L
        )

        val health =
            store.recordFailure(
                routeId = "route-a",
                reason = FailureReason.TCP_TIMEOUT,
                timestampEpochMs = 2000L
            )

        assertEquals(
            HealthState.DEGRADED,
            health.state
        )

        assertEquals(
            1,
            health.consecutiveFailures
        )

        assertEquals(
            FailureReason.TCP_TIMEOUT,
            health.lastFailure
        )

        assertEquals(
            2000L,
            health.lastFailureEpochMs
        )
    }


    @Test
    fun `concurrent mutations preserve newest observation`() {

        val store =
            HealthStore()

        val executor =
            Executors.newFixedThreadPool(8)

        val start =
            CountDownLatch(1)

        val done =
            CountDownLatch(100)

        try {

            for (index in 1..100) {

                executor.submit {

                    try {
                        start.await()

                        if (index == 100) {

                            store.recordSuccess(
                                routeId = "route-a",
                                timestampEpochMs = 100_000L
                            )

                        } else {

                            store.recordFailure(
                                routeId = "route-a",
                                reason = FailureReason.TCP_TIMEOUT,
                                timestampEpochMs =
                                    index * 1000L
                            )
                        }

                    } finally {
                        done.countDown()
                    }
                }
            }

            start.countDown()

            assertTrue(
                done.await(
                    5,
                    TimeUnit.SECONDS
                )
            )

        } finally {

            executor.shutdown()

            assertTrue(
                executor.awaitTermination(
                    5,
                    TimeUnit.SECONDS
                )
            )
        }

        val health =
            store.get("route-a")

        /*
         * Regardless of thread execution order, the newest timestamp
         * belongs to the successful observation at 100000.
         */
        assertEquals(
            HealthState.HEALTHY,
            health.state
        )

        assertEquals(
            0,
            health.consecutiveFailures
        )

        assertEquals(
            FailureReason.NONE,
            health.lastFailure
        )

        assertEquals(
            100_000L,
            health.lastSuccessEpochMs
        )
    }

}
