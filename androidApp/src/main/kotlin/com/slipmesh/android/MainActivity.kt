package com.slipmesh.android

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.slipmesh.android.vpn.SlipMeshVpnService
import com.slipmesh.android.vpn.VpnLifecycleCommand
import com.slipmesh.android.vpn.VpnLifecycleEvent
import com.slipmesh.android.vpn.SharedPreferencesVpnConnectionIntentStore
import com.slipmesh.android.vpn.VpnLifecycleRuntime
import com.slipmesh.android.vpn.VpnRecoveryAction
import com.slipmesh.android.vpn.VpnRecoveryIntentRecorder
import com.slipmesh.android.vpn.VpnRecoveryLifecycleBridge
import com.slipmesh.android.vpn.VpnRecoveryRuntime
import com.slipmesh.android.vpn.VpnSessionState

class MainActivity : Activity() {

    private lateinit var statusView: TextView

    private var pendingPermissionIntent: Intent? = null

    private val recoveryIntentStore by lazy {
        SharedPreferencesVpnConnectionIntentStore.create(
            this
        )
    }

    private val recoveryIntentRecorder by lazy {
        VpnRecoveryIntentRecorder(
            recoveryIntentStore
        )
    }

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)

        val connectButton =
            Button(this).apply {
                text = getString(
                    R.string.vpn_connect
                )

                setOnClickListener {
                    requestConnect()
                }
            }

        val disconnectButton =
            Button(this).apply {
                text = getString(
                    R.string.vpn_disconnect
                )

                setOnClickListener {
                    requestDisconnect()
                }
            }

        statusView =
            TextView(this).apply {
                textSize = 18f
                gravity = Gravity.CENTER
            }

        setContentView(
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                gravity = Gravity.CENTER

                addView(statusView)
                addView(connectButton)
                addView(disconnectButton)
            }
        )

        renderState()
        recoverConnectionIntentOnce()
    }

    override fun onResume() {
        super.onResume()
        renderState()
    }

    private fun recoverConnectionIntentOnce() {
        val permissionIntent =
            VpnService.prepare(this)

        pendingPermissionIntent =
            permissionIntent

        val recoveryAction =
            VpnRecoveryRuntime.recover(
                store =
                    recoveryIntentStore,
                permissionGranted =
                    permissionIntent == null,
            )

        when (recoveryAction) {
            VpnRecoveryAction.NONE -> {
                pendingPermissionIntent = null
            }

            VpnRecoveryAction.REQUEST_PERMISSION,
            VpnRecoveryAction.REQUEST_CONNECT -> {
                val transition =
                    VpnRecoveryLifecycleBridge.transitionFor(
                        action =
                            recoveryAction
                    ) ?: return

                val runtimeTransition =
                    VpnLifecycleRuntime.dispatch(
                        when (recoveryAction) {
                            VpnRecoveryAction.REQUEST_PERMISSION ->
                                VpnLifecycleEvent.ConnectRequested(
                                    permissionAlreadyGranted =
                                        false,
                                )

                            VpnRecoveryAction.REQUEST_CONNECT ->
                                VpnLifecycleEvent.ConnectRequested(
                                    permissionAlreadyGranted =
                                        true,
                                )

                            VpnRecoveryAction.NONE ->
                                return
                        }
                    )

                check(
                    transition ==
                        runtimeTransition
                )

                executeCommands(
                    runtimeTransition.commands
                )
            }
        }

        renderState()
    }

    private fun requestConnect() {
        if (
            !recoveryIntentRecorder
                .recordConnectRequested()
        ) {
            renderState()
            return
        }

        val permissionIntent =
            VpnService.prepare(this)

        pendingPermissionIntent =
            permissionIntent

        val transition =
            VpnLifecycleRuntime.dispatch(
                VpnLifecycleEvent.ConnectRequested(
                    permissionAlreadyGranted =
                        permissionIntent == null,
                )
            )

        executeCommands(
            transition.commands
        )

        renderState()
    }

    private fun requestDisconnect() {
        val durableIntentCleared =
            recoveryIntentRecorder
                .recordExplicitDisconnect()

        if (!durableIntentCleared) {
            VpnRecoveryRuntime
                .suppressRecoveryForProcess()
        }

        val transition =
            VpnLifecycleRuntime.dispatch(
                VpnLifecycleEvent.DisconnectRequested
            )

        executeCommands(
            transition.commands
        )

        renderState()
    }

    @Deprecated(
        "VpnService.prepare uses the Activity result contract."
    )
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data,
        )

        if (
            requestCode !=
            VPN_PERMISSION_REQUEST
        ) {
            return
        }

        pendingPermissionIntent = null

        val granted =
            resultCode == RESULT_OK

        if (!granted) {
            val durableIntentCleared =
                recoveryIntentRecorder
                    .recordPermissionDenied()

            if (!durableIntentCleared) {
                VpnRecoveryRuntime
                    .suppressRecoveryForProcess()
            }
        }

        val transition =
            VpnLifecycleRuntime.dispatch(
                VpnLifecycleEvent.PermissionResult(
                    granted = granted,
                )
            )

        executeCommands(
            transition.commands
        )

        renderState()
    }

    private fun executeCommands(
        commands: List<VpnLifecycleCommand>,
    ) {
        for (command in commands) {
            when (command) {

                VpnLifecycleCommand.REQUEST_PERMISSION -> {
                    val intent =
                        pendingPermissionIntent
                            ?: continue

                    startActivityForResult(
                        intent,
                        VPN_PERMISSION_REQUEST,
                    )
                }

                VpnLifecycleCommand.START_SERVICE -> {
                    startForegroundService(
                        SlipMeshVpnService.startIntent(
                            this
                        )
                    )
                }

                VpnLifecycleCommand.STOP_SERVICE -> {
                    startService(
                        SlipMeshVpnService.stopIntent(
                            this
                        )
                    )
                }
            }
        }
    }

    private fun renderState() {
        val state: VpnSessionState =
            VpnLifecycleRuntime.state()

        statusView.text =
            getString(
                R.string.vpn_status,
                state.name,
            )
    }

    companion object {
        private const val
            VPN_PERMISSION_REQUEST = 1001
    }
}
