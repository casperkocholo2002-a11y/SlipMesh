package com.slipmesh.core

import kotlin.test.Test
import kotlin.test.assertEquals


class FailoverOrchestratorTest {

    private val config =
        FailoverPolicyConfig(
            failoverFailureThreshold = 2,
            recoverySuccessThreshold = 2,
            switchCooldownMs = 10_000L
        )


    private fun route(
        id: String,
        provider: String,
        priority: Int = 100,
        enabled: Boolean = true
    ) =
        RouteCandidate(
            id = id,
            transport =
                TransportKind.LEGACY_VLESS_WS_TLS,
            failureDomain =
                FailureDomain(
                    providerId = provider,
                    accountId =
                        "account-$provider",
                    hostname =
                        "$id.example.test"
                ),
            basePriority = priority,
            enabled = enabled
        )


    private fun orchestrator(
        store: HealthStore,
        active: String? = "route-a"
    ) =
        FailoverOrchestrator(
            healthStore = store,
            config = config,
            initialSnapshot =
                FailoverSnapshot(
                    activeRouteId = active
                )
        )


    @Test
    fun `healthy current route remains stable`() {

        val store =
            HealthStore()

        store.recordSuccess(
            "route-a",
            1000L
        )

        val engine =
            orchestrator(store)

        val result =
            engine.evaluate(
                routes =
                    listOf(
                        route(
                            "route-a",
                            "provider-a"
                        ),
                        route(
                            "route-b",
                            "provider-b"
                        )
                    ),
                timestampEpochMs = 2000L
            )

        assertEquals(
            FailoverState.STABLE,
            result.snapshot.state
        )

        assertEquals(
            FailoverAction.NONE,
            result.action
        )
    }


    @Test
    fun `failure threshold chooses diverse alternative`() {

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

        val engine =
            orchestrator(store)

        val result =
            engine.evaluate(
                routes =
                    listOf(
                        route(
                            "route-a",
                            "provider-a"
                        ),
                        route(
                            "route-b",
                            "provider-a"
                        ),
                        route(
                            "route-c",
                            "provider-c"
                        )
                    ),
                timestampEpochMs = 3000L
            )

        assertEquals(
            FailoverState.FAILING_OVER,
            result.snapshot.state
        )

        assertEquals(
            "route-c",
            result.snapshot.targetRouteId
        )
    }


    @Test
    fun `no active route selects best initial route`() {

        val store =
            HealthStore()

        val engine =
            orchestrator(
                store = store,
                active = null
            )

        val result =
            engine.evaluate(
                routes =
                    listOf(
                        route(
                            "route-a",
                            "provider-a",
                            priority = 100
                        ),
                        route(
                            "route-b",
                            "provider-b",
                            priority = 300
                        )
                    ),
                timestampEpochMs = 1000L
            )

        assertEquals(
            FailoverState.FAILING_OVER,
            result.snapshot.state
        )

        assertEquals(
            "route-b",
            result.snapshot.targetRouteId
        )
    }


    @Test
    fun `disabled active route fails over without fake network failure`() {

        val store =
            HealthStore()

        store.recordSuccess(
            "route-a",
            1000L
        )

        val engine =
            orchestrator(store)

        val result =
            engine.evaluate(
                routes =
                    listOf(
                        route(
                            "route-a",
                            "provider-a",
                            enabled = false
                        ),
                        route(
                            "route-b",
                            "provider-b"
                        )
                    ),
                timestampEpochMs = 2000L
            )

        assertEquals(
            FailoverCause.ACTIVE_ROUTE_UNAVAILABLE,
            result.cause
        )

        assertEquals(
            "route-b",
            result.snapshot.targetRouteId
        )
    }


    @Test
    fun `unavailable alternatives cause degraded wait`() {

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

        repeat(3) { index ->
            store.recordFailure(
                "route-b",
                FailureReason.TCP_TIMEOUT,
                3000L + index
            )
        }

        val engine =
            orchestrator(store)

        val result =
            engine.evaluate(
                routes =
                    listOf(
                        route(
                            "route-a",
                            "provider-a"
                        ),
                        route(
                            "route-b",
                            "provider-b"
                        )
                    ),
                timestampEpochMs = 4000L
            )

        assertEquals(
            FailoverState.DEGRADED,
            result.snapshot.state
        )

        assertEquals(
            FailoverAction.WAIT_FOR_ROUTE,
            result.action
        )
    }


    @Test
    fun `network change probes instead of switching`() {

        val store =
            HealthStore()

        val engine =
            orchestrator(store)

        val result =
            engine.evaluate(
                routes =
                    listOf(
                        route(
                            "route-a",
                            "provider-a"
                        ),
                        route(
                            "route-b",
                            "provider-b"
                        )
                    ),
                timestampEpochMs = 1000L,
                networkChanged = true
            )

        assertEquals(
            FailoverState.PROBING,
            result.snapshot.state
        )

        assertEquals(
            FailoverAction.PROBE_CURRENT,
            result.action
        )
    }


    @Test
    fun `switch and recovery lifecycle stays synchronized with health store`() {

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

        val engine =
            orchestrator(store)

        val decision =
            engine.evaluate(
                routes =
                    listOf(
                        route(
                            "route-a",
                            "provider-a"
                        ),
                        route(
                            "route-b",
                            "provider-b"
                        )
                    ),
                timestampEpochMs = 3000L
            )

        assertEquals(
            "route-b",
            decision.snapshot.targetRouteId
        )

        val switched =
            engine.switchSucceeded(
                routeId = "route-b",
                timestampEpochMs = 4000L
            )

        assertEquals(
            FailoverState.RECOVERING,
            switched.snapshot.state
        )

        engine.terminalSuccess(
            routeId = "route-b",
            timestampEpochMs = 5000L
        )

        val recovered =
            engine.terminalSuccess(
                routeId = "route-b",
                timestampEpochMs = 6000L
            )

        assertEquals(
            FailoverState.STABLE,
            recovered.snapshot.state
        )

        assertEquals(
            HealthState.HEALTHY,
            store.get("route-b").state
        )

        assertEquals(
            6000L,
            store.get("route-b")
                .lastSuccessEpochMs
        )
    }

    @Test
    fun `in flight target remains authoritative until switch result`() {

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

        val engine =
            orchestrator(store)

        val first =
            engine.evaluate(
                routes =
                    listOf(
                        route(
                            "route-a",
                            "provider-a"
                        ),
                        route(
                            "route-b",
                            "provider-b"
                        )
                    ),
                timestampEpochMs = 3000L
            )

        assertEquals(
            "route-b",
            first.snapshot.targetRouteId
        )

        /*
         * A later policy evaluation now strongly prefers route-c,
         * but route-b is already in flight and must remain authoritative.
         */
        val reevaluated =
            engine.evaluate(
                routes =
                    listOf(
                        route(
                            "route-a",
                            "provider-a"
                        ),
                        route(
                            "route-b",
                            "provider-b"
                        ),
                        route(
                            "route-c",
                            "provider-c",
                            priority = 1000
                        )
                    ),
                timestampEpochMs = 5000L
            )

        assertEquals(
            FailoverState.FAILING_OVER,
            reevaluated.snapshot.state
        )

        assertEquals(
            "route-b",
            reevaluated.snapshot.targetRouteId
        )

        /*
         * The old active route may briefly recover while the transport
         * switch is already underway. That evidence updates HealthStore,
         * but must not silently cancel the issued switch.
         */
        val recoveredOldRoute =
            engine.terminalSuccess(
                routeId = "route-a",
                timestampEpochMs = 6000L
            )

        assertEquals(
            FailoverState.FAILING_OVER,
            recoveredOldRoute.snapshot.state
        )

        assertEquals(
            "route-b",
            recoveredOldRoute.snapshot.targetRouteId
        )

        /*
         * The switch completed at t=4000 but its result arrived after
         * the t=5000/t=6000 observations. Since those observations were
         * policy-irrelevant while the switch was outstanding, the
         * legitimate result must still be accepted.
         */
        val switched =
            engine.switchSucceeded(
                routeId = "route-b",
                timestampEpochMs = 4000L
            )

        assertEquals(
            FailoverState.RECOVERING,
            switched.snapshot.state
        )

        assertEquals(
            "route-b",
            switched.snapshot.activeRouteId
        )
    }


    @Test
    fun `failed target is skipped during retry backoff`() {

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

        val engine =
            orchestrator(store)

        val routes =
            listOf(
                route(
                    "route-a",
                    "provider-a"
                ),
                route(
                    "route-b",
                    "provider-b"
                ),
                route(
                    "route-c",
                    "provider-c"
                )
            )

        val first =
            engine.evaluate(
                routes = routes,
                timestampEpochMs = 3000L
            )

        assertEquals(
            "route-b",
            first.snapshot.targetRouteId
        )

        val failed =
            engine.switchFailed(
                routeId = "route-b",
                timestampEpochMs = 4000L
            )

        assertEquals(
            FailoverState.DEGRADED,
            failed.snapshot.state
        )

        /*
         * route-b remains the highest deterministic alternative,
         * but policy suppression must force route-c instead.
         */
        val retry =
            engine.evaluate(
                routes = routes,
                timestampEpochMs = 5000L
            )

        assertEquals(
            FailoverState.FAILING_OVER,
            retry.snapshot.state
        )

        assertEquals(
            "route-c",
            retry.snapshot.targetRouteId
        )
    }


    @Test
    fun `failed target becomes eligible after retry deadline`() {

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

        val engine =
            orchestrator(store)

        val routes =
            listOf(
                route(
                    "route-a",
                    "provider-a"
                ),
                route(
                    "route-b",
                    "provider-b"
                )
            )

        val first =
            engine.evaluate(
                routes = routes,
                timestampEpochMs = 3000L
            )

        assertEquals(
            "route-b",
            first.snapshot.targetRouteId
        )

        engine.switchFailed(
            routeId = "route-b",
            timestampEpochMs = 4000L
        )

        val blocked =
            engine.evaluate(
                routes = routes,
                timestampEpochMs = 5000L
            )

        assertEquals(
            FailoverState.DEGRADED,
            blocked.snapshot.state
        )

        assertEquals(
            FailoverAction.WAIT_FOR_ROUTE,
            blocked.action
        )

        /*
         * Default failedTargetRetryMs is 10 seconds:
         * failed @ 4000 => eligible again at 14000.
         */
        val eligible =
            engine.evaluate(
                routes = routes,
                timestampEpochMs = 14_000L
            )

        assertEquals(
            FailoverState.FAILING_OVER,
            eligible.snapshot.state
        )

        assertEquals(
            "route-b",
            eligible.snapshot.targetRouteId
        )

        assertEquals(
            emptyMap(),
            eligible.snapshot
                .failedTargetRetryUntilEpochMs
        )
    }

}
