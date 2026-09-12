package com.slipmesh.android.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import com.slipmesh.android.MainActivity
import com.slipmesh.android.R

/**
 * VPN lifecycle service only.
 *
 * P2-WP02 does not establish a TUN interface,
 * forward packets, or open any transport.
 */
class SlipMeshVpnService : VpnService() {

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {

        return when (intent?.action) {

            ACTION_START -> {
                if (tryPromoteToForeground()) {
                    VpnLifecycleRuntime.dispatch(
                        VpnLifecycleEvent.ServiceStarted
                    )
                } else {
                    VpnLifecycleRuntime.dispatch(
                        VpnLifecycleEvent.ServiceStopped
                    )

                    stopSelf()
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
        VpnLifecycleRuntime.dispatch(
            VpnLifecycleEvent.ServiceStopped
        )

        super.onDestroy()
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
