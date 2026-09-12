package com.slipmesh.android.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRoutingPolicyApplierTest {

    private class FakeLookup(
        private val installed: Set<String>,
        private val events: MutableList<String>,
    ) : InstalledPackageLookup {

        override fun isInstalled(
            packageName: String,
        ): Boolean {

            events +=
                "check:$packageName"

            return packageName in installed
        }
    }

    private class FakeTarget(
        private val events: MutableList<String>,
    ) : AppRoutingTarget {

        val allowed =
            mutableListOf<String>()

        val disallowed =
            mutableListOf<String>()

        override fun allowApplication(
            packageName: String,
        ) {
            events +=
                "allow:$packageName"

            allowed += packageName
        }

        override fun disallowApplication(
            packageName: String,
        ) {
            events +=
                "disallow:$packageName"

            disallowed += packageName
        }
    }

    @Test
    fun allAppsIsNoOpWithoutPackageLookup() {
        val events =
            mutableListOf<String>()

        val applier =
            AppRoutingPolicyApplier(
                FakeLookup(
                    installed = emptySet(),
                    events = events,
                )
            )

        val target =
            FakeTarget(events)

        val result =
            applier.apply(
                AppRoutingPolicy.AllApps,
                target,
            )

        assertEquals(
            AppRoutingApplyResult.Applied,
            result,
        )

        assertTrue(events.isEmpty())
        assertTrue(target.allowed.isEmpty())
        assertTrue(target.disallowed.isEmpty())
    }

    @Test
    fun allowOnlyValidatesEntireSetBeforeMutation() {
        val events =
            mutableListOf<String>()

        val packages =
            setOf(
                "com.example.alpha",
                "com.example.beta",
            )

        val applier =
            AppRoutingPolicyApplier(
                FakeLookup(
                    installed = packages,
                    events = events,
                )
            )

        val target =
            FakeTarget(events)

        val result =
            applier.apply(
                AppRoutingPolicy.AllowOnly.of(
                    packages
                ),
                target,
            )

        assertEquals(
            AppRoutingApplyResult.Applied,
            result,
        )

        assertEquals(
            listOf(
                "check:com.example.alpha",
                "check:com.example.beta",
                "allow:com.example.alpha",
                "allow:com.example.beta",
            ),
            events,
        )
    }

    @Test
    fun disallowValidatesEntireSetBeforeMutation() {
        val events =
            mutableListOf<String>()

        val packages =
            setOf(
                "com.example.alpha",
                "com.example.beta",
            )

        val applier =
            AppRoutingPolicyApplier(
                FakeLookup(
                    installed = packages,
                    events = events,
                )
            )

        val target =
            FakeTarget(events)

        val result =
            applier.apply(
                AppRoutingPolicy.Disallow.of(
                    packages
                ),
                target,
            )

        assertEquals(
            AppRoutingApplyResult.Applied,
            result,
        )

        assertEquals(
            listOf(
                "check:com.example.alpha",
                "check:com.example.beta",
                "disallow:com.example.alpha",
                "disallow:com.example.beta",
            ),
            events,
        )
    }

    @Test
    fun unavailableAllowPackageRejectsWithZeroMutation() {
        val events =
            mutableListOf<String>()

        val applier =
            AppRoutingPolicyApplier(
                FakeLookup(
                    installed =
                        setOf(
                            "com.example.beta"
                        ),
                    events = events,
                )
            )

        val target =
            FakeTarget(events)

        val result =
            applier.apply(
                AppRoutingPolicy.AllowOnly.of(
                    listOf(
                        "com.example.alpha",
                        "com.example.beta",
                    )
                ),
                target,
            )

        assertEquals(
            AppRoutingApplyResult.Rejected(
                listOf(
                    "com.example.alpha"
                )
            ),
            result,
        )

        assertEquals(
            listOf(
                "check:com.example.alpha",
                "check:com.example.beta",
            ),
            events,
        )

        assertTrue(target.allowed.isEmpty())
        assertTrue(target.disallowed.isEmpty())
    }

    @Test
    fun unavailableDisallowPackageRejectsWithZeroMutation() {
        val events =
            mutableListOf<String>()

        val applier =
            AppRoutingPolicyApplier(
                FakeLookup(
                    installed =
                        setOf(
                            "com.example.alpha"
                        ),
                    events = events,
                )
            )

        val target =
            FakeTarget(events)

        val result =
            applier.apply(
                AppRoutingPolicy.Disallow.of(
                    listOf(
                        "com.example.alpha",
                        "com.example.beta",
                    )
                ),
                target,
            )

        assertEquals(
            AppRoutingApplyResult.Rejected(
                listOf(
                    "com.example.beta"
                )
            ),
            result,
        )

        assertEquals(
            listOf(
                "check:com.example.alpha",
                "check:com.example.beta",
            ),
            events,
        )

        assertTrue(target.allowed.isEmpty())
        assertTrue(target.disallowed.isEmpty())
    }
}
