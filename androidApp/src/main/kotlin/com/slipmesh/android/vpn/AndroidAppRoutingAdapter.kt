package com.slipmesh.android.vpn

import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build

class PackageManagerInstalledPackageLookup(
    private val packageManager: PackageManager,
) : InstalledPackageLookup {

    override fun isInstalled(
        packageName: String,
    ): Boolean =
        try {
            packageManager.applicationInfoCompat(
                packageName
            )

            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
}

class VpnBuilderAppRoutingTarget(
    private val builder: VpnService.Builder,
) : AppRoutingTarget {

    override fun allowApplication(
        packageName: String,
    ) {
        builder.addAllowedApplication(
            packageName
        )
    }

    override fun disallowApplication(
        packageName: String,
    ) {
        builder.addDisallowedApplication(
            packageName
        )
    }
}

private fun PackageManager.applicationInfoCompat(
    packageName: String,
) {
    if (
        Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.TIRAMISU
    ) {
        getApplicationInfo(
            packageName,
            PackageManager.ApplicationInfoFlags.of(0)
        )
    } else {
        getApplicationInfoLegacy(
            packageName
        )
    }
}

@Suppress("DEPRECATION")
private fun PackageManager.getApplicationInfoLegacy(
    packageName: String,
) {
    getApplicationInfo(
        packageName,
        0
    )
}
