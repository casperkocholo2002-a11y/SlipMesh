package com.slipmesh.android.vpn

fun interface InstalledPackageLookup {

    fun isInstalled(
        packageName: String,
    ): Boolean
}

interface AppRoutingTarget {

    fun allowApplication(
        packageName: String,
    )

    fun disallowApplication(
        packageName: String,
    )
}

sealed interface AppRoutingApplyResult {

    data object Applied :
        AppRoutingApplyResult

    data class Rejected(
        val unavailablePackageNames: List<String>,
    ) : AppRoutingApplyResult
}

class AppRoutingPolicyApplier(
    private val installedPackageLookup: InstalledPackageLookup,
) {

    fun apply(
        policy: AppRoutingPolicy,
        target: AppRoutingTarget,
    ): AppRoutingApplyResult {

        if (policy === AppRoutingPolicy.AllApps) {
            return AppRoutingApplyResult.Applied
        }

        val packageNames =
            when (policy) {
                AppRoutingPolicy.AllApps ->
                    error(
                        "AllApps handled before scoped application"
                    )

                is AppRoutingPolicy.AllowOnly ->
                    policy.packageNames

                is AppRoutingPolicy.Disallow ->
                    policy.packageNames
            }

        val unavailable =
            packageNames.filterNot { packageName ->
                installedPackageLookup.isInstalled(
                    packageName
                )
            }

        if (unavailable.isNotEmpty()) {
            return AppRoutingApplyResult.Rejected(
                unavailablePackageNames =
                    unavailable
            )
        }

        when (policy) {
            AppRoutingPolicy.AllApps ->
                error(
                    "AllApps handled before scoped mutation"
                )

            is AppRoutingPolicy.AllowOnly ->
                packageNames.forEach { packageName ->
                    target.allowApplication(
                        packageName
                    )
                }

            is AppRoutingPolicy.Disallow ->
                packageNames.forEach { packageName ->
                    target.disallowApplication(
                        packageName
                    )
                }
        }

        return AppRoutingApplyResult.Applied
    }
}
