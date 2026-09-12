package com.slipmesh.android.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.system.OsConstants
import com.slipmesh.android.MainActivity
import com.slipmesh.android.R

/**
 * VPN lifecycle and minimal TUN ownership boundary.
 *
 * P2-WP03 establishes an inert TUN interface only.
 * It does not read/write packets, add capture routes,
 * or open any transport.
 */
class SlipMeshVpnService : VpnService() {

    private val tunLock = Any()

    private val tunSession =
        TunSession()

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {

        return when (intent?.action) {

            ACTION_START -> {
                if (
                    tryPromoteToForeground() &&
                    tryEstablishTun()
                ) {
                    VpnLifecycleRuntime.dispatch(
                        VpnLifecycleEvent.ServiceStarted
                    )
                } else {
                    failStart()
                }

                Service.START_NOT_STICKY
            }

            ACTION_STOP -> {
                stopVpnService()

                Service.START_NOT_STICKY
            }

            else ->
                Service.START_NOT_STICKY
        }
    }

    override fun onRevoke() {
        val durableIntentCleared =
            VpnRecoveryIntentRecorder(
                SharedPreferencesVpnConnectionIntentStore.create(
                    this
                )
            ).recordPermissionRevoked()

        if (!durableIntentCleared) {
            VpnRecoveryRuntime
                .suppressRecoveryForProcess()
        }

        closeTun()

        VpnLifecycleRuntime.dispatch(
            VpnLifecycleEvent.PermissionRevoked
        )

        stopForeground(
            STOP_FOREGROUND_REMOVE
        )

        stopSelf()

        super.onRevoke()
    }

    override fun onDestroy() {
        closeTun()

        VpnLifecycleRuntime.dispatch(
            VpnLifecycleEvent.ServiceStopped
        )

        super.onDestroy()
    }

    private fun tryEstablishTun(): Boolean =
        synchronized(tunLock) {

            if (tunSession.hasDescriptor()) {
                return@synchronized true
            }

            try {
                val builder =
                    Builder()
                        .setSession(
                            getString(
                                R.string.app_name
                            )
                        )
                        .addAddress(
                            TUN_IPV4_ADDRESS,
                            TUN_IPV4_PREFIX_LENGTH,
                        )
                        .allowFamily(
                            OsConstants.AF_INET
                        )
                        .allowFamily(
                            OsConstants.AF_INET6
                        )

                val policyResult =
                    AppRoutingPolicyApplier(
                        PackageManagerInstalledPackageLookup(
                            packageManager
                        )
                    ).apply(
                        currentAppRoutingPolicy(),
                        VpnBuilderAppRoutingTarget(
                            builder
                        ),
                    )

                if (
                    policyResult !==
                    AppRoutingApplyResult.Applied
                ) {
                    return@synchronized false
                }

                val descriptor =
                    builder
                        .establish()
                        ?: return@synchronized false

                val handle =
                    ParcelFileDescriptorTunHandle(
                        descriptor
                    )

                if (
                    tunSession.attach(
                        handle
                    )
                ) {
                    true
                } else {
                    try {
                        descriptor.close()
                    } catch (_: Exception) {
                        // Descriptor was not adopted.
                    }

                    false
                }

            } catch (_: PackageManager.NameNotFoundException) {
                false

            } catch (_: IllegalArgumentException) {
                false

            } catch (_: IllegalStateException) {
                false

            } catch (_: SecurityException) {
                false
            }
        }

    private fun currentAppRoutingPolicy():
        AppRoutingPolicy =
        AppRoutingPolicy.AllApps

    private fun closeTun() {
        synchronized(tunLock) {
            tunSession.close()
        }
    }

    private fun failStart() {
        closeTun()

        VpnLifecycleRuntime.dispatch(
            VpnLifecycleEvent.ServiceStopped
        )

        stopForeground(
            STOP_FOREGROUND_REMOVE
        )

        stopSelf()
    }

    private fun tryPromoteToForeground(): Boolean {
        return try {
            promoteToForeground()
            true
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalStateException) {
            false
        }
    }

    private fun promoteToForeground() {
        createNotificationChannel()

        val openAppIntent =
            Intent(
                this,
                MainActivity::class.java,
            )

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            Notification.Builder(
                this,
                NOTIFICATION_CHANNEL_ID,
            )
                .setSmallIcon(
                    R.drawable.ic_slipmesh
                )
                .setContentTitle(
                    getString(
                        R.string.vpn_notification_title
                    )
                )
                .setContentText(
                    getString(
                        R.string.vpn_notification_text
                    )
                )
                .setContentIntent(
                    pendingIntent
                )
                .setOngoing(true)
                .build()

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo
                    .FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED,
            )
        } else {
            startForeground(
                NOTIFICATION_ID,
                notification,
            )
        }
    }

    private fun createNotificationChannel() {
        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        val channel =
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(
                    R.string.vpn_notification_channel
                ),
                NotificationManager.IMPORTANCE_LOW,
            )

        manager.createNotificationChannel(
            channel
        )
    }

    private fun stopVpnService() {
        closeTun()

        VpnLifecycleRuntime.dispatch(
            VpnLifecycleEvent.ServiceStopped
        )

        stopForeground(
            STOP_FOREGROUND_REMOVE
        )

        stopSelf()
    }

    companion object {

        private const val ACTION_START =
            "com.slipmesh.android.vpn.START"

        private const val ACTION_STOP =
            "com.slipmesh.android.vpn.STOP"

        private const val
            NOTIFICATION_CHANNEL_ID =
            "slipmesh_vpn"

        private const val NOTIFICATION_ID =
            1001

        private const val TUN_IPV4_ADDRESS =
            "10.254.0.1"

        private const val TUN_IPV4_PREFIX_LENGTH =
            32

        fun startIntent(
            context: Context,
        ): Intent =
            Intent(
                context,
                SlipMeshVpnService::class.java,
            ).setAction(
                ACTION_START
            )

        fun stopIntent(
            context: Context,
        ): Intent =
            Intent(
                context,
                SlipMeshVpnService::class.java,
            ).setAction(
                ACTION_STOP
            )
    }
}
