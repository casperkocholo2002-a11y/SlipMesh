package com.slipmesh.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


class FailoverStateMachineTest {

    private val config =
        FailoverPolicyConfig(
            failoverFailureThreshold = 2,
            recoverySuccessThreshold = 2,
            switchCooldownMs = 10_000L
        )


    private fun snapshot(
        state: FailoverState =
            FailoverState.STABLE,
        active: String? = "route-a",
        previous: String? = null,
        target: String? = null,
        recoverySuccesses: Int = 0,
        cooldownUntil: Long? = null,
        lastEvent: Long = 0L
    ) =
        FailoverSnapshot(
            state = state,
            activeRouteId = active,
            previousRouteId = previous,
            targetRouteId = target,
            recoverySuccesses =
                recoverySuccesses,
            cooldownUntilEpochMs =
                cooldownUntil,
            lastEventEpochMs =
                lastEvent
        )


    private fun health(
        state: HealthState,
        failures: Int = 0
    ) =
        RouteHealth(
            state = state,
            consecutiveFailures = failures
        )


    @Test
    fun `healthy active route remains stable`() {

        val result =
            FailoverStateMachine.transition(
                snapshot = snapshot(),
                event =
                    FailoverEvent.Evaluate(
                        activeHealth =
                            health(
                                HealthState.HEALTHY
                            ),
                        alternativeRouteId =
                            "route-b",
                        timestampEpochMs =
                            1000L
                    ),
                config = config
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
    fun `first degraded failure enters probing`() {

        val result =
            FailoverStateMachine.transition(
                snapshot(),
                FailoverEvent.Evaluate(
                    activeHealth =
                        health(
                            HealthState.DEGRADED,
                            failures = 1
                        ),
                    alternativeRouteId =
                        "route-b",
                    timestampEpochMs =
                        1000L
                ),
                config
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
    fun `failure threshold triggers failover`() {

        val result =
            FailoverStateMachine.transition(
                snapshot(),
                FailoverEvent.Evaluate(
                    activeHealth =
                        health(
                            HealthState.DEGRADED,
                            failures = 2
                        ),
                    alternativeRouteId =
                        "route-b",
                    timestampEpochMs =
                        2000L
                ),
                config
            )

        assertEquals(
            FailoverState.FAILING_OVER,
            result.snapshot.state
        )

        assertEquals(
            "route-b",
            result.snapshot.targetRouteId
        )

        assertEquals(
            FailoverAction.SWITCH_TO_TARGET,
            result.action
        )
    }


    @Test
    fun `degraded route without alternative enters degraded state`() {

        val result =
            FailoverStateMachine.transition(
                snapshot(),
                FailoverEvent.Evaluate(
                    activeHealth =
                        health(
                            HealthState.DEGRADED,
                            failures = 2
                        ),
                    alternativeRouteId = null,
                    timestampEpochMs =
                        2000L
                ),
                config
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
    fun `unreachable route triggers immediate failover`() {

        val result =
            FailoverStateMachine.transition(
                snapshot(),
                FailoverEvent.Evaluate(
                    activeHealth =
                        health(
                            HealthState.UNREACHABLE
                        ),
                    alternativeRouteId =
                        "route-b",
                    timestampEpochMs =
                        1000L
                ),
                config
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
    fun `suspected block triggers immediate failover`() {

        val result =
            FailoverStateMachine.transition(
                snapshot(),
                FailoverEvent.Evaluate(
                    activeHealth =
                        health(
                            HealthState.SUSPECTED_BLOCK
                        ),
                    alternativeRouteId =
                        "route-b",
                    timestampEpochMs =
                        1000L
                ),
                config
            )

        assertEquals(
            FailoverState.FAILING_OVER,
            result.snapshot.state
        )

        assertEquals(
            FailoverCause.SUSPECTED_BLOCK,
            result.cause
        )
    }


    @Test
    fun `network change probes without switching`() {

        val result =
            FailoverStateMachine.transition(
                snapshot(),
                FailoverEvent.Evaluate(
                    activeHealth =
                        health(
                            HealthState.UNKNOWN
                        ),
                    alternativeRouteId =
                        "route-b",
                    networkChanged = true,
                    timestampEpochMs =
                        1000L
                ),
                config
            )

        assertEquals(
            FailoverState.PROBING,
            result.snapshot.state
        )

        assertEquals(
            FailoverAction.PROBE_CURRENT,
            result.action
        )

        assertNull(
            result.snapshot.targetRouteId
        )
    }


    @Test
    fun `unknown health requests probe`() {

        val result =
            FailoverStateMachine.transition(
                snapshot(),
                FailoverEvent.Evaluate(
                    activeHealth =
                        health(
                            HealthState.UNKNOWN
                        ),
                    alternativeRouteId =
                        "route-b",
                    timestampEpochMs =
                        1000L
                ),
                config
            )

        assertEquals(
            FailoverState.PROBING,
            result.snapshot.state
        )
    }


    @Test
    fun `no active route switches to available alternative`() {

        val result =
            FailoverStateMachine.transition(
                snapshot(
                    active = null
                ),
                FailoverEvent.Evaluate(
                    activeHealth =
                        health(
                            HealthState.UNKNOWN
                        ),
                    alternativeRouteId =
                        "route-b",
                    timestampEpochMs =
                        1000L
                ),
                config
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
    fun `no active route and no alternative is degraded`() {

        val result =
            FailoverStateMachine.transition(
                snapshot(
                    active = null
                ),
                FailoverEvent.Evaluate(
                    activeHealth =
                        health(
                            HealthState.UNKNOWN
                        ),
                    alternativeRouteId = null,
                    timestampEpochMs =
                        1000L
                ),
                config
            )

        assertEquals(
            FailoverState.DEGRADED,
            result.snapshot.state
        )
    }


    @Test
    fun `successful switch enters recovering`() {

        val start =
            snapshot(
                state =
                    FailoverState.FAILING_OVER,
                active = "route-a",
                target = "route-b"
            )

        val result =
            FailoverStateMachine.transition(
                start,
                FailoverEvent.SwitchSucceeded(
                    routeId = "route-b",
                    timestampEpochMs =
                        5000L
                ),
                config
            )

        assertEquals(
            FailoverState.RECOVERING,
            result.snapshot.state
        )

        assertEquals(
            "route-b",
            result.snapshot.activeRouteId
        )

        assertEquals(
            "route-a",
            result.snapshot.previousRouteId
        )

        assertEquals(
            15_000L,
            result.snapshot.cooldownUntilEpochMs
        )
    }


    @Test
    fun `recovery requires configured terminal successes`() {

        val recovering =
            snapshot(
                state =
                    FailoverState.RECOVERING,
                active = "route-b",
                previous = "route-a",
                cooldownUntil = 15_000L,
                lastEvent = 5000L
            )

        val first =
            FailoverStateMachine.transition(
                recovering,
                FailoverEvent.TerminalSuccess(
                    routeId = "route-b",
                    timestampEpochMs =
                        6000L
                ),
                config
            )

        assertEquals(
            FailoverState.RECOVERING,
            first.snapshot.state
        )

        assertEquals(
            1,
            first.snapshot.recoverySuccesses
        )

        val second =
            FailoverStateMachine.transition(
                first.snapshot,
                FailoverEvent.TerminalSuccess(
                    routeId = "route-b",
                    timestampEpochMs =
                        7000L
                ),
                config
            )

        assertEquals(
            FailoverState.STABLE,
            second.snapshot.state
        )

        assertEquals(
            FailoverCause.RECOVERY_COMPLETE,
            second.cause
        )
    }


    @Test
    fun `healthy evaluation cannot fake recovery success`() {

        val recovering =
            snapshot(
                state =
                    FailoverState.RECOVERING,
                active = "route-b",
                previous = "route-a",
                cooldownUntil = 15_000L
            )

        val result =
            FailoverStateMachine.transition(
                recovering,
                FailoverEvent.Evaluate(
                    activeHealth =
                        health(
                            HealthState.HEALTHY
                        ),
                    alternativeRouteId =
                        "route-a",
                    timestampEpochMs =
                        6000L
                ),
                config
            )

        assertEquals(
            FailoverState.RECOVERING,
            result.snapshot.state
        )

        assertEquals(
            0,
            result.snapshot.recoverySuccesses
        )
    }


    @Test
    fun `non active terminal success is ignored`() {

        val recovering =
            snapshot(
                state =
                    FailoverState.RECOVERING,
                active = "route-b"
            )

        val result =
            FailoverStateMachine.transition(
                recovering,
                FailoverEvent.TerminalSuccess(
                    routeId = "route-a",
                    timestampEpochMs =
                        6000L
                ),
                config
            )

        assertEquals(
            0,
            result.snapshot.recoverySuccesses
        )

        assertEquals(
            FailoverCause.NON_ACTIVE_SUCCESS,
            result.cause
        )
    }


    @Test
    fun `cooldown prevents normal switch back`() {

        val current =
            snapshot(
                state =
                    FailoverState.RECOVERING,
                active = "route-b",
                previous = "route-a",
                cooldownUntil = 15_000L,
                lastEvent = 5000L
            )

        val result =
            FailoverStateMachine.transition(
                current,
                FailoverEvent.Evaluate(
                    activeHealth =
                        health(
                            HealthState.DEGRADED,
                            failures = 2
                        ),
                    alternativeRouteId =
                        "route-a",
                    timestampEpochMs =
                        8000L
                ),
                config
            )

        assertEquals(
            FailoverState.PROBING,
            result.snapshot.state
        )

        assertEquals(
            FailoverAction.WAIT_COOLDOWN,
            result.action
        )
    }


    @Test
    fun `critical failure may override cooldown`() {

        val current =
            snapshot(
                state =
                    FailoverState.RECOVERING,
                active = "route-b",
                previous = "route-a",
                cooldownUntil = 15_000L,
                lastEvent = 5000L
            )

        val result =
            FailoverStateMachine.transition(
                current,
                FailoverEvent.Evaluate(
                    activeHealth =
                        health(
                            HealthState.UNREACHABLE
                        ),
                    alternativeRouteId =
                        "route-a",
                    timestampEpochMs =
                        8000L
                ),
                config
            )

        assertEquals(
            FailoverState.FAILING_OVER,
            result.snapshot.state
        )

        assertEquals(
            "route-a",
            result.snapshot.targetRouteId
        )
    }


    @Test
    fun `failed switch enters degraded state`() {

        val current =
            snapshot(
                state =
                    FailoverState.FAILING_OVER,
                active = "route-a",
                target = "route-b"
            )

        val result =
            FailoverStateMachine.transition(
                current,
                FailoverEvent.SwitchFailed(
                    routeId = "route-b",
                    timestampEpochMs =
                        3000L
                ),
                config
            )

        assertEquals(
            FailoverState.DEGRADED,
            result.snapshot.state
        )

        assertEquals(
            FailoverCause.SWITCH_FAILED,
            result.cause
        )
    }


    @Test
    fun `unexpected switch result cannot change active route`() {

        val current =
            snapshot(
                state =
                    FailoverState.FAILING_OVER,
                active = "route-a",
                target = "route-b"
            )

        val result =
            FailoverStateMachine.transition(
                current,
                FailoverEvent.SwitchSucceeded(
                    routeId = "route-c",
                    timestampEpochMs =
                        3000L
                ),
                config
            )

        assertEquals(
            "route-a",
            result.snapshot.activeRouteId
        )

        assertEquals(
            FailoverCause.UNEXPECTED_SWITCH_RESULT,
            result.cause
        )
    }


    @Test
    fun `stale event cannot overwrite newer state`() {

        val current =
            snapshot(
                state =
                    FailoverState.STABLE,
                active = "route-a",
                lastEvent = 5000L
            )

        val result =
            FailoverStateMachine.transition(
                current,
                FailoverEvent.Evaluate(
                    activeHealth =
                        health(
                            HealthState.UNREACHABLE
                        ),
                    alternativeRouteId =
                        "route-b",
                    timestampEpochMs =
                        4000L
                ),
                config
            )

        assertEquals(
            current,
            result.snapshot
        )

        assertEquals(
            FailoverCause.STALE_EVENT,
            result.cause
        )
    }

    @Test
    fun `recovery completion preserves unexpired cooldown`() {

        val recovering =
            snapshot(
                state =
                    FailoverState.RECOVERING,
                active = "route-b",
                previous = "route-a",
                recoverySuccesses = 1,
                cooldownUntil = 15_000L,
                lastEvent = 6000L
            )

        val result =
            FailoverStateMachine.transition(
                recovering,
                FailoverEvent.TerminalSuccess(
                    routeId = "route-b",
                    timestampEpochMs = 7000L
                ),
                config
            )

        assertEquals(
            FailoverState.STABLE,
            result.snapshot.state
        )

        assertEquals(
            "route-a",
            result.snapshot.previousRouteId
        )

        assertEquals(
            15_000L,
            result.snapshot.cooldownUntilEpochMs
        )

        assertEquals(
            FailoverCause.RECOVERY_COMPLETE,
            result.cause
        )
    }


    @Test
    fun `switch back remains blocked after recovery until cooldown expires`() {

        val stableAfterRecovery =
            snapshot(
                state =
                    FailoverState.STABLE,
                active = "route-b",
                previous = "route-a",
                cooldownUntil = 15_000L,
                lastEvent = 7000L
            )

        val result =
            FailoverStateMachine.transition(
                stableAfterRecovery,
                FailoverEvent.Evaluate(
                    activeHealth =
                        health(
                            HealthState.DEGRADED,
                            failures = 2
                        ),
                    alternativeRouteId =
                        "route-a",
                    timestampEpochMs =
                        8000L
                ),
                config
            )

        assertEquals(
            FailoverState.PROBING,
            result.snapshot.state
        )

        assertEquals(
            FailoverAction.WAIT_COOLDOWN,
            result.action
        )

        assertEquals(
            FailoverCause.COOLDOWN_ACTIVE,
            result.cause
        )
    }


    @Test
    fun `expired cooldown metadata is cleared and switch back becomes eligible`() {

        val stable =
            snapshot(
                state =
                    FailoverState.STABLE,
                active = "route-b",
                previous = "route-a",
                cooldownUntil = 15_000L,
                lastEvent = 7000L
            )

        val result =
            FailoverStateMachine.transition(
                stable,
                FailoverEvent.Evaluate(
                    activeHealth =
                        health(
                            HealthState.DEGRADED,
                            failures = 2
                        ),
                    alternativeRouteId =
                        "route-a",
                    timestampEpochMs =
                        16_000L
                ),
                config
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
            "route-a",
            result.snapshot.targetRouteId
        )

        assertNull(
            result.snapshot.previousRouteId
        )

        assertNull(
            result.snapshot.cooldownUntilEpochMs
        )
    }


    @Test
    fun `evaluate cannot retarget an in flight switch`() {

        val current =
            snapshot(
                state =
                    FailoverState.FAILING_OVER,
                active = "route-a",
                target = "route-b",
                lastEvent = 3000L
            )

        val result =
            FailoverStateMachine.transition(
                current,
                FailoverEvent.Evaluate(
                    activeHealth =
                        health(
                            HealthState.DEGRADED,
                            failures = 3
                        ),
                    alternativeRouteId =
                        "route-c",
                    timestampEpochMs =
                        5000L
                ),
                config
            )

        assertEquals(
            FailoverState.FAILING_OVER,
            result.snapshot.state
        )

        assertEquals(
            "route-b",
            result.snapshot.targetRouteId
        )

        assertEquals(
            3000L,
            result.snapshot.lastEventEpochMs
        )

        assertEquals(
            FailoverCause.SWITCH_IN_PROGRESS,
            result.cause
        )
    }


    @Test
    fun `terminal success cannot cancel an in flight switch`() {

        val current =
            snapshot(
                state =
                    FailoverState.FAILING_OVER,
                active = "route-a",
                target = "route-b",
                lastEvent = 3000L
            )

        val result =
            FailoverStateMachine.transition(
                current,
                FailoverEvent.TerminalSuccess(
                    routeId = "route-a",
                    timestampEpochMs =
                        6000L
                ),
                config
            )

        assertEquals(
            FailoverState.FAILING_OVER,
            result.snapshot.state
        )

        assertEquals(
            "route-b",
            result.snapshot.targetRouteId
        )

        assertEquals(
            3000L,
            result.snapshot.lastEventEpochMs
        )

        assertEquals(
            FailoverCause.SWITCH_IN_PROGRESS,
            result.cause
        )
    }


    @Test
    fun `unexpected result cannot poison expected switch result`() {

        val current =
            snapshot(
                state =
                    FailoverState.FAILING_OVER,
                active = "route-a",
                target = "route-b",
                lastEvent = 3000L
            )

        val unexpected =
            FailoverStateMachine.transition(
                current,
                FailoverEvent.SwitchSucceeded(
                    routeId = "route-c",
                    timestampEpochMs =
                        5000L
                ),
                config
            )

        assertEquals(
            current,
            unexpected.snapshot
        )

        val expected =
            FailoverStateMachine.transition(
                unexpected.snapshot,
                FailoverEvent.SwitchSucceeded(
                    routeId = "route-b",
                    timestampEpochMs =
                        4000L
                ),
                config
            )

        assertEquals(
            FailoverState.RECOVERING,
            expected.snapshot.state
        )

        assertEquals(
            "route-b",
            expected.snapshot.activeRouteId
        )

        assertEquals(
            FailoverCause.SWITCH_SUCCEEDED,
            expected.cause
        )
    }


    @Test
    fun `failed switch records policy retry deadline`() {

        val current =
            snapshot(
                state =
                    FailoverState.FAILING_OVER,
                active = "route-a",
                target = "route-b",
                lastEvent = 3000L
            )

        val result =
            FailoverStateMachine.transition(
                current,
                FailoverEvent.SwitchFailed(
                    routeId = "route-b",
                    timestampEpochMs =
                        4000L
                ),
                config
            )

        assertEquals(
            FailoverState.DEGRADED,
            result.snapshot.state
        )

        assertEquals(
            14_000L,
            result.snapshot
                .failedTargetRetryUntilEpochMs[
                    "route-b"
                ]
        )

        assertEquals(
            FailoverCause.SWITCH_FAILED,
            result.cause
        )
    }


    @Test
    fun `failed switch preserves other suppressed targets`() {

        val current =
            FailoverSnapshot(
                state =
                    FailoverState.FAILING_OVER,
                activeRouteId =
                    "route-a",
                targetRouteId =
                    "route-c",
                failedTargetRetryUntilEpochMs =
                    mapOf(
                        "route-b" to
                            12_000L
                    ),
                lastEventEpochMs =
                    3000L
            )

        val result =
            FailoverStateMachine.transition(
                current,
                FailoverEvent.SwitchFailed(
                    routeId = "route-c",
                    timestampEpochMs =
                        5000L
                ),
                config
            )

        assertEquals(
            12_000L,
            result.snapshot
                .failedTargetRetryUntilEpochMs[
                    "route-b"
                ]
        )

        assertEquals(
            15_000L,
            result.snapshot
                .failedTargetRetryUntilEpochMs[
                    "route-c"
                ]
        )
    }

}
