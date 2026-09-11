package com.slipmesh.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


class RouteSelectorTest {

    private fun route(
        id: String,
        provider: String,
        account: String,
        hostname: String,
        transport: TransportKind =
            TransportKind.LEGACY_VLESS_WS_TLS,
        priority: Int = 100,
        enabled: Boolean = true
    ) =
        RouteCandidate(
            id = id,
            transport = transport,
            failureDomain =
                FailureDomain(
                    providerId = provider,
                    accountId = account,
                    hostname = hostname
                ),
            basePriority = priority,
            enabled = enabled
        )


    @Test
    fun `provider diversity wins during failover`() {

        val current =
            route(
                id = "primary",
                provider = "cloudflare",
                account = "account-a",
                hostname = "worker-a.example"
            )

        val sameProviderBackup =
            route(
                id = "backup-cf",
                provider = "cloudflare",
                account = "account-b",
                hostname = "worker-b.example"
            )

        val independentProvider =
            route(
                id = "backup-independent",
                provider = "provider-b",
                account = "account-x",
                hostname = "front-x.example"
            )

        val routes =
            listOf(
                current,
                sameProviderBackup,
                independentProvider
            )

        val health =
            mapOf(
                "primary" to
                    RouteHealth(
                        state = HealthState.SUSPECTED_BLOCK,
                        consecutiveFailures = 3
                    ),

                "backup-cf" to
                    RouteHealth(
                        state = HealthState.HEALTHY
                    ),

                "backup-independent" to
                    RouteHealth(
                        state = HealthState.HEALTHY
                    )
            )

        val decision =
            RouteSelector.select(
                routes = routes,
                health = health,
                currentRouteId = "primary"
            )

        assertEquals(
            "backup-independent",
            decision.selected?.id
        )
    }


    @Test
    fun `unreachable route is never selected`() {

        val unreachable =
            route(
                id = "dead",
                provider = "provider-b",
                account = "b",
                hostname = "dead.example"
            )

        val degraded =
            route(
                id = "degraded",
                provider = "provider-c",
                account = "c",
                hostname = "degraded.example"
            )

        val health =
            mapOf(
                "dead" to
                    RouteHealth(
                        state = HealthState.UNREACHABLE
                    ),

                "degraded" to
                    RouteHealth(
                        state = HealthState.DEGRADED
                    )
            )

        val decision =
            RouteSelector.select(
                routes =
                    listOf(
                        unreachable,
                        degraded
                    ),
                health = health
            )

        assertEquals(
            "degraded",
            decision.selected?.id
        )
    }


    @Test
    fun `all unavailable routes returns no route`() {

        val a =
            route(
                id = "a",
                provider = "a",
                account = "a",
                hostname = "a.example"
            )

        val b =
            route(
                id = "b",
                provider = "b",
                account = "b",
                hostname = "b.example"
            )

        val health =
            mapOf(
                "a" to
                    RouteHealth(
                        state = HealthState.UNREACHABLE
                    ),

                "b" to
                    RouteHealth(
                        state = HealthState.COOLDOWN
                    )
            )

        val decision =
            RouteSelector.select(
                routes = listOf(a, b),
                health = health
            )

        assertNull(
            decision.selected
        )
    }


    @Test
    fun `disabled route is ignored`() {

        val disabled =
            route(
                id = "disabled",
                provider = "provider-x",
                account = "x",
                hostname = "x.example",
                priority = 1000,
                enabled = false
            )

        val usable =
            route(
                id = "usable",
                provider = "provider-y",
                account = "y",
                hostname = "y.example"
            )

        val decision =
            RouteSelector.select(
                routes =
                    listOf(
                        disabled,
                        usable
                    ),
                health = emptyMap()
            )

        assertEquals(
            "usable",
            decision.selected?.id
        )
    }


    @Test
    fun `selection is deterministic when scores tie`() {

        val routeB =
            route(
                id = "route-b",
                provider = "provider",
                account = "account",
                hostname = "b.example"
            )

        val routeA =
            route(
                id = "route-a",
                provider = "provider",
                account = "account",
                hostname = "a.example"
            )

        val decision =
            RouteSelector.select(
                routes =
                    listOf(
                        routeB,
                        routeA
                    ),
                health = emptyMap()
            )

        assertEquals(
            "route-a",
            decision.selected?.id
        )
    }


    @Test
    fun `repeated failures reduce route preference`() {

        val unstable =
            route(
                id = "unstable",
                provider = "provider-a",
                account = "a",
                hostname = "unstable.example"
            )

        val stable =
            route(
                id = "stable",
                provider = "provider-a",
                account = "a",
                hostname = "stable.example"
            )

        val health =
            mapOf(
                "unstable" to
                    RouteHealth(
                        state = HealthState.HEALTHY,
                        consecutiveFailures = 10
                    ),

                "stable" to
                    RouteHealth(
                        state = HealthState.HEALTHY,
                        consecutiveFailures = 0
                    )
            )

        val decision =
            RouteSelector.select(
                routes =
                    listOf(
                        unstable,
                        stable
                    ),
                health = health
            )

        assertEquals(
            "stable",
            decision.selected?.id
        )
    }
}
