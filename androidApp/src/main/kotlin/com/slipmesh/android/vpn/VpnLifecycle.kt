package com.slipmesh.android.vpn

enum class VpnSessionState {
    IDLE,
    WAITING_FOR_PERMISSION,
    STARTING,
    RUNNING,
    STOPPING,
}

enum class VpnLifecycleCommand {
    REQUEST_PERMISSION,
    START_SERVICE,
    STOP_SERVICE,
}

sealed interface VpnLifecycleEvent {

    data class ConnectRequested(
        val permissionAlreadyGranted: Boolean,
    ) : VpnLifecycleEvent

    data class PermissionResult(
        val granted: Boolean,
    ) : VpnLifecycleEvent

    object ServiceStarted : VpnLifecycleEvent

    object DisconnectRequested : VpnLifecycleEvent

    object ServiceStopped : VpnLifecycleEvent

    object PermissionRevoked : VpnLifecycleEvent
}

data class VpnLifecycleTransition(
    val state: VpnSessionState,
    val commands: List<VpnLifecycleCommand> = emptyList(),
)

object VpnLifecycleReducer {

    fun reduce(
        current: VpnSessionState,
        event: VpnLifecycleEvent,
    ): VpnLifecycleTransition {

        if (event === VpnLifecycleEvent.PermissionRevoked) {
            val commands =
                if (
                    current == VpnSessionState.STARTING ||
                    current == VpnSessionState.RUNNING ||
                    current == VpnSessionState.STOPPING
                ) {
                    listOf(
                        VpnLifecycleCommand.STOP_SERVICE
                    )
                } else {
                    emptyList()
                }

            return VpnLifecycleTransition(
                state = VpnSessionState.IDLE,
                commands = commands,
            )
        }

        return when (current) {

            VpnSessionState.IDLE ->
                reduceIdle(event)

            VpnSessionState.WAITING_FOR_PERMISSION ->
                reduceWaitingForPermission(event)

            VpnSessionState.STARTING ->
                reduceStarting(event)

            VpnSessionState.RUNNING ->
                reduceRunning(event)

            VpnSessionState.STOPPING ->
                reduceStopping(event)
        }
    }

    private fun reduceIdle(
        event: VpnLifecycleEvent,
    ): VpnLifecycleTransition {

        return when (event) {

            is VpnLifecycleEvent.ConnectRequested -> {
                if (event.permissionAlreadyGranted) {
                    VpnLifecycleTransition(
                        state = VpnSessionState.STARTING,
                        commands = listOf(
                            VpnLifecycleCommand.START_SERVICE
                        ),
                    )
                } else {
                    VpnLifecycleTransition(
                        state =
                            VpnSessionState.WAITING_FOR_PERMISSION,
                        commands = listOf(
                            VpnLifecycleCommand.REQUEST_PERMISSION
                        ),
                    )
                }
            }

            else ->
                unchanged(VpnSessionState.IDLE)
        }
    }

    private fun reduceWaitingForPermission(
        event: VpnLifecycleEvent,
    ): VpnLifecycleTransition {

        return when (event) {

            is VpnLifecycleEvent.PermissionResult -> {
                if (event.granted) {
                    VpnLifecycleTransition(
                        state = VpnSessionState.STARTING,
                        commands = listOf(
                            VpnLifecycleCommand.START_SERVICE
                        ),
                    )
                } else {
                    VpnLifecycleTransition(
                        state = VpnSessionState.IDLE,
                    )
                }
            }

            VpnLifecycleEvent.DisconnectRequested ->
                VpnLifecycleTransition(
                    state = VpnSessionState.IDLE,
                )

            else ->
                unchanged(
                    VpnSessionState.WAITING_FOR_PERMISSION
                )
        }
    }

    private fun reduceStarting(
        event: VpnLifecycleEvent,
    ): VpnLifecycleTransition {

        return when (event) {

            VpnLifecycleEvent.ServiceStarted ->
                VpnLifecycleTransition(
                    state = VpnSessionState.RUNNING,
                )

            VpnLifecycleEvent.DisconnectRequested ->
                VpnLifecycleTransition(
                    state = VpnSessionState.STOPPING,
                    commands = listOf(
                        VpnLifecycleCommand.STOP_SERVICE
                    ),
                )

            VpnLifecycleEvent.ServiceStopped ->
                VpnLifecycleTransition(
                    state = VpnSessionState.IDLE,
                )

            else ->
                unchanged(VpnSessionState.STARTING)
        }
    }

    private fun reduceRunning(
        event: VpnLifecycleEvent,
    ): VpnLifecycleTransition {

        return when (event) {

            VpnLifecycleEvent.DisconnectRequested ->
                VpnLifecycleTransition(
                    state = VpnSessionState.STOPPING,
                    commands = listOf(
                        VpnLifecycleCommand.STOP_SERVICE
                    ),
                )

            VpnLifecycleEvent.ServiceStopped ->
                VpnLifecycleTransition(
                    state = VpnSessionState.IDLE,
                )

            else ->
                unchanged(VpnSessionState.RUNNING)
        }
    }

    private fun reduceStopping(
        event: VpnLifecycleEvent,
    ): VpnLifecycleTransition {

        return when (event) {

            VpnLifecycleEvent.ServiceStopped ->
                VpnLifecycleTransition(
                    state = VpnSessionState.IDLE,
                )

            else ->
                unchanged(VpnSessionState.STOPPING)
        }
    }

    private fun unchanged(
        state: VpnSessionState,
    ): VpnLifecycleTransition {

        return VpnLifecycleTransition(
            state = state,
        )
    }
}

/**
 * Process-local coordination between the Activity and VpnService.
 *
 * This is intentionally not persistent state. Persistent VPN recovery
 * belongs to a later lifecycle/resilience work package.
 */
object VpnLifecycleRuntime {

    @Volatile
    private var currentState: VpnSessionState =
        VpnSessionState.IDLE

    fun state(): VpnSessionState =
        currentState

    @Synchronized
    fun dispatch(
        event: VpnLifecycleEvent,
    ): VpnLifecycleTransition {

        val transition =
            VpnLifecycleReducer.reduce(
                current = currentState,
                event = event,
            )

        currentState = transition.state

        return transition
    }
}
