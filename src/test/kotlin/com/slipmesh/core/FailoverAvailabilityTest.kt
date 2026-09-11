package com.slipmesh.core

import kotlin.test.Test
import kotlin.test.assertEquals


class FailoverAvailabilityTest {

    private val config =
        FailoverPolicyConfig(
            failoverFailureThreshold = 2,
            recoverySuccessThreshold = 2,
            switchCooldownMs = 10_000L
        )


    @Test
    fun `unavailable active route immediately fails over`() {

        val result =
            FailoverStateMachine.transition(
                snapshot =
                    FailoverSnapshot(
                        state =
                            FailoverState.STABLE,
                        activeRouteId =
                            "route-a"
                    ),
                event =
                    FailoverEvent.Evaluate(
                        activeHealth =
                            RouteHealth(
                                state =
                                    HealthState.HEALTHY
                            ),
                        alternativeRouteId =
                            "route-b",
                        activeRouteAvailable =
                            false,
                        timestampEpochMs =
                            1000L
                    ),
                config = config
            )

        assertEquals(
            FailoverState.FAILING_OVER,
            result.snapshot.state
        )

        assertEquals(
            FailoverAction.SWITCH_TO_TARGET,
            result.action
        )

        assertEquals(
            FailoverCause.ACTIVE_ROUTE_UNAVAILABLE,
            result.cause
        )
    }


    @Test
    fun `unavailable active route without alternative degrades`() {

        val result =
            FailoverStateMachine.transition(
                snapshot =
                    FailoverSnapshot(
                        activeRouteId =
                            "route-a"
                    ),
                event =
                    FailoverEvent.Evaluate(
                        activeHealth =
                            RouteHealth(
                                state =
                                    HealthState.HEALTHY
                            ),
                        alternativeRouteId =
                            null,
                        activeRouteAvailable =
                            false,
                        timestampEpochMs =
                            1000L
                    ),
                config = config
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
}
