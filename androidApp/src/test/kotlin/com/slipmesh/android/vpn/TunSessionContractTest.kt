package com.slipmesh.android.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TunSessionContractTest {

    private class FakeHandle(
        private val failOnClose: Boolean = false,
    ) : TunHandle {

        var closeCount: Int = 0
            private set

        override fun close() {
            closeCount += 1

            if (failOnClose) {
                throw IllegalStateException(
                    "synthetic close failure"
                )
            }
        }
    }

    @Test
    fun newSessionStartsWithoutDescriptor() {
        val session = TunSession()

        assertFalse(
            session.hasDescriptor()
        )
    }

    @Test
    fun firstHandleIsAdopted() {
        val session = TunSession()
        val handle = FakeHandle()

        assertTrue(
            session.attach(handle)
        )

        assertTrue(
            session.hasDescriptor()
        )
    }

    @Test
    fun secondHandleIsRejectedWhileOwned() {
        val session = TunSession()
        val first = FakeHandle()
        val second = FakeHandle()

        assertTrue(
            session.attach(first)
        )

        assertFalse(
            session.attach(second)
        )

        assertTrue(
            session.hasDescriptor()
        )

        assertEquals(
            0,
            first.closeCount,
        )

        assertEquals(
            0,
            second.closeCount,
        )
    }

    @Test
    fun closeReleasesOwnedHandleExactlyOnce() {
        val session = TunSession()
        val handle = FakeHandle()

        assertTrue(
            session.attach(handle)
        )

        session.close()
        session.close()

        assertFalse(
            session.hasDescriptor()
        )

        assertEquals(
            1,
            handle.closeCount,
        )
    }

    @Test
    fun closeFailureStillClearsOwnership() {
        val session = TunSession()

        val failingHandle =
            FakeHandle(
                failOnClose = true
            )

        assertTrue(
            session.attach(
                failingHandle
            )
        )

        session.close()

        assertFalse(
            session.hasDescriptor()
        )

        assertEquals(
            1,
            failingHandle.closeCount,
        )
    }

    @Test
    fun newHandleCanBeAdoptedAfterClose() {
        val session = TunSession()
        val first = FakeHandle()
        val second = FakeHandle()

        assertTrue(
            session.attach(first)
        )

        session.close()

        assertTrue(
            session.attach(second)
        )

        assertTrue(
            session.hasDescriptor()
        )

        assertEquals(
            1,
            first.closeCount,
        )

        assertEquals(
            0,
            second.closeCount,
        )
    }

    @Test
    fun repeatedCloseWithoutHandleIsSafe() {
        val session = TunSession()

        session.close()
        session.close()

        assertFalse(
            session.hasDescriptor()
        )
    }
}
