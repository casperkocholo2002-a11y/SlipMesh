package com.slipmesh.android.vpn

sealed interface AppRoutingPolicy {

    data object AllApps :
        AppRoutingPolicy

    class AllowOnly private constructor(
        val packageNames: List<String>,
    ) : AppRoutingPolicy {

        companion object {

            fun of(
                packageNames: Collection<String>,
            ): AllowOnly =
                AllowOnly(
                    normalizeScopedPackages(
                        packageNames
                    )
                )
        }

        override fun equals(
            other: Any?,
        ): Boolean =
            other is AllowOnly &&
                packageNames == other.packageNames

        override fun hashCode(): Int =
            packageNames.hashCode()

        override fun toString(): String =
            "AllowOnly(packageNames=$packageNames)"
    }

    class Disallow private constructor(
        val packageNames: List<String>,
    ) : AppRoutingPolicy {

        companion object {

            fun of(
                packageNames: Collection<String>,
            ): Disallow =
                Disallow(
                    normalizeScopedPackages(
                        packageNames
                    )
                )
        }

        override fun equals(
            other: Any?,
        ): Boolean =
            other is Disallow &&
                packageNames == other.packageNames

        override fun hashCode(): Int =
            packageNames.hashCode()

        override fun toString(): String =
            "Disallow(packageNames=$packageNames)"
    }
}

private fun normalizeScopedPackages(
    packageNames: Collection<String>,
): List<String> {

    require(
        packageNames.isNotEmpty()
    ) {
        "Scoped package set must not be empty"
    }

    val normalized =
        packageNames
            .map { packageName ->
                packageName.trim()
            }
            .also { names ->
                require(
                    names.none {
                        it.isEmpty()
                    }
                ) {
                    "Package name must not be blank"
                }
            }
            .distinct()
            .sorted()

    require(
        normalized.isNotEmpty()
    ) {
        "Scoped package set must not be empty"
    }

    return normalized
}
