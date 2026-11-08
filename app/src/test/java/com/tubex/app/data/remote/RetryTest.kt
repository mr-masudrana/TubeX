package com.tubex.app.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import java.io.IOException
import java.net.UnknownHostException

class RetryTest {
    @Test fun succeedsAfterTransientFailures() = runBlocking {
        var calls = 0
        val r = Retry.withBackoff(attempts = 3, baseDelayMs = 1) {
            calls++
            if (calls < 3) throw IOException("flaky")
            "ok"
        }
        assertEquals("ok", r)
        assertEquals(3, calls)
    }

    @Test fun givesUpAfterAttempts() = runBlocking {
        var calls = 0
        try {
            Retry.withBackoff(attempts = 3, baseDelayMs = 1) { calls++; throw ParsingException("bad page") }
            fail("should throw")
        } catch (e: ParsingException) {
            assertEquals(3, calls)
        }
    }

    @Test fun permanentErrorsAreNotRetried() = runBlocking {
        var calls = 0
        try {
            Retry.withBackoff(attempts = 3, baseDelayMs = 1) { calls++; throw ContentNotAvailableException("gone") }
            fail("should throw")
        } catch (e: ContentNotAvailableException) {
            assertEquals(1, calls)
        }
    }

    @Test fun noInternetIsNotRetried() {
        assertTrue(!Retry.isTransient(UnknownHostException("x")))
        assertTrue(Retry.isTransient(IOException("x")))
    }
}
