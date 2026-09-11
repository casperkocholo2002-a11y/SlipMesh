package com.slipmesh.core

import kotlin.test.Test
import kotlin.test.assertEquals


class RouteSelectorAlternativeTest {

    private fun route(
        id: String,
        provider: String,
        priority: Int = 100
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
            basePriority = priority
        )


    @Test
    fun `alternative never returns current route`() {

        val routes =
            listOf(
                route(
                    id = "route-a",
                    provider = "provider-a",
                    priority = 1000
                ),
                route(
                    id = "route-b",
                    provider = "provider-b"
                )
            )

        val selected =
            RouteSelector.selectAlternative(
                routes = routes,
                health =
                    mapOf(
                        "route-a" to
                            RouteHealth(
                                state =
                                    HealthState.HEALTHY
                            ),
                        "route-b" to
                            RouteHealth(
                                state =
                                    HealthState.HEALTHY
                            )
                    ),
                currentRouteId = "route-a"
            )

        assertEquals(
            "route-b",
            selected?.id
        )
    }


    @Test
    fun `alternative preserves provider diversity ranking`() {

        val routes =
            listOf(
                route(
                    id = "route-a",
                    provider = "provider-a"
                ),
                route(
                    id = "route-b",
                    provider = "provider-a"
                ),
                route(
                    id = "route-c",
                    provider = "provider-c"
                )
            )

        val selected =
            RouteSelector.selectAlternative(
                routes = routes,
                health =
                    mapOf(
                        "route-a" to
                            RouteHealth(
                                state =
                                    HealthState.DEGRADED,
                                consecutiveFailures = 2
                            )
                    ),
                currentRouteId = "route-a"
            )

        assertEquals(
            "route-c",
            selected?.id
        )
    }
}
