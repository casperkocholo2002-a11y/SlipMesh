package com.slipmesh.core

/**
 * Deterministic route selector.
 *
 * Phase-1 goals:
 *
 * 1. Never select disabled routes.
 * 2. Never select routes currently marked UNREACHABLE or COOLDOWN.
 * 3. Prefer healthy routes.
 * 4. Penalize repeated failures.
 * 5. When leaving a route, prefer diversity across:
 *      provider
 *      account
 *      hostname
 *      transport
 *
 * This is intentionally policy-only code.
 * It performs no networking.
 */
object RouteSelector {

    data class ScoredRoute(
        val route: RouteCandidate,
        val score: Int
    )

    data class Decision(
        val selected: RouteCandidate?,
        val ranked: List<ScoredRoute>
    )

    fun select(
        routes: List<RouteCandidate>,
        health: Map<String, RouteHealth>,
        currentRouteId: String? = null
    ): Decision {

        val current =
            currentRouteId?.let { id ->
                routes.firstOrNull { it.id == id }
            }

        val ranked =
            routes
                .asSequence()

                .filter { it.enabled }

                .filter { route ->
                    when (health[route.id]?.state ?: HealthState.UNKNOWN) {
                        HealthState.UNREACHABLE,
                        HealthState.COOLDOWN -> false

                        else -> true
                    }
                }

                .map { route ->

                    val routeHealth =
                        health[route.id] ?: RouteHealth()

                    val score =
                        route.basePriority +
                            healthScore(routeHealth) +
                            diversityScore(
                                current = current,
                                candidate = route
                            ) -
                            failurePenalty(routeHealth)

                    ScoredRoute(
                        route = route,
                        score = score
                    )
                }

                .sortedWith(
                    compareByDescending<ScoredRoute> { it.score }
                        .thenBy { it.route.id }
                )

                .toList()

        return Decision(
            selected = ranked.firstOrNull()?.route,
            ranked = ranked
        )
    }


    private fun healthScore(
        health: RouteHealth
    ): Int =
        when (health.state) {

            HealthState.HEALTHY ->
                100

            HealthState.UNKNOWN ->
                40

            HealthState.DEGRADED ->
                0

            HealthState.SUSPECTED_BLOCK ->
                -100

            HealthState.UNREACHABLE,
            HealthState.COOLDOWN ->
                -10_000
        }


    private fun failurePenalty(
        health: RouteHealth
    ): Int {

        val cappedFailures =
            health.consecutiveFailures
                .coerceAtMost(20)

        return cappedFailures * 15
    }


    /**
     * Diversity is evaluated relative to the route we are leaving.
     *
     * Provider diversity receives the largest bonus because switching
     * only hostname/account may still leave us inside the same provider
     * failure domain.
     */
    private fun diversityScore(
        current: RouteCandidate?,
        candidate: RouteCandidate
    ): Int {

        if (current == null)
            return 0

        if (current.id == candidate.id)
            return 0

        var score = 0

        if (
            candidate.failureDomain.providerId !=
            current.failureDomain.providerId
        ) {
            score += 160
        }

        if (
            candidate.failureDomain.accountId !=
            current.failureDomain.accountId
        ) {
            score += 40
        }

        if (
            candidate.failureDomain.hostname !=
            current.failureDomain.hostname
        ) {
            score += 20
        }

        if (
            candidate.transport !=
            current.transport
        ) {
            score += 80
        }

        return score
    }
}
