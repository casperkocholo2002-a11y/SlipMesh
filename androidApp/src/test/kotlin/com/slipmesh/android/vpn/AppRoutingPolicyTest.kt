package com.slipmesh.android.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AppRoutingPolicyTest {

    @Test
    fun allAppsIsStableSingleton() {
        assertSame(
            AppRoutingPolicy.AllApps,
            AppRoutingPolicy.AllApps,
        )
    }

    @Test
    fun allowOnlyNormalizesDeterministically() {
        val policy =
            AppRoutingPolicy.AllowOnly.of(
                listOf(
                    " com.example.beta ",
                    "com.example.alpha",
                    "com.example.beta",
                )
            )

        assertEquals(
            listOf(
                "com.example.alpha",
                "com.example.beta",
            ),
            policy.packageNames,
        )
    }

    @Test
    fun disallowNormalizesDeterministically() {
        val policy =
            AppRoutingPolicy.Disallow.of(
                listOf(
                    "com.example.zeta",
                    " com.example.alpha ",
                    "com.example.zeta",
                )
            )

        assertEquals(
            listOf(
                "com.example.alpha",
                "com.example.zeta",
            ),
            policy.packageNames,
        )
    }

    @Test
    fun equivalentAllowOnlyPoliciesAreEqual() {
        assertEquals(
            AppRoutingPolicy.AllowOnly.of(
                listOf(
                    "com.example.beta",
                    "com.example.alpha",
                )
            ),
            AppRoutingPolicy.AllowOnly.of(
                listOf(
                    "com.example.alpha",
                    "com.example.beta",
                )
            ),
        )
    }

    @Test
    fun equivalentDisallowPoliciesAreEqual() {
        assertEquals(
            AppRoutingPolicy.Disallow.of(
                listOf(
                    "com.example.beta",
                    "com.example.alpha",
                )
            ),
            AppRoutingPolicy.Disallow.of(
                listOf(
                    "com.example.alpha",
                    "com.example.beta",
                )
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun allowOnlyRejectsEmptyCollection() {
        AppRoutingPolicy.AllowOnly.of(
            emptyList()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun disallowRejectsEmptyCollection() {
        AppRoutingPolicy.Disallow.of(
            emptyList()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun allowOnlyRejectsBlankPackageName() {
        AppRoutingPolicy.AllowOnly.of(
            listOf(
                "com.example.valid",
                "   ",
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun disallowRejectsBlankPackageName() {
        AppRoutingPolicy.Disallow.of(
            listOf(
                "",
                "com.example.valid",
            )
        )
    }
}
