package com.tubex.app.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import java.io.IOException
import java.net.UnknownHostException

/**
 * সাময়িক ত্রুটিতে (নেটওয়ার্ক ঝামেলা, YouTube-এর আধা-পাওয়া পেজ) অল্প বিরতিতে আবার চেষ্টা।
 * বিরতি বাড়ে দ্বিগুণ হারে: base, 2×base, 4×base …
 */
object Retry {
    suspend fun <T> withBackoff(
        attempts: Int = 3,
        baseDelayMs: Long = 600,
        retryable: (Throwable) -> Boolean = ::isTransient,
        block: suspend () -> T
    ): T {
        require(attempts >= 1)
        var i = 0
        while (true) {
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                i++
                if (i >= attempts || !retryable(e)) throw e
                delay(baseDelayMs shl (i - 1))
            }
        }
    }

    /** আবার চেষ্টা করলে ঠিক হতে পারে এমন ত্রুটি। কনটেন্ট নেই/বয়সসীমা/অঞ্চল-বাধা বা ইন্টারনেট নেই — এগুলোয় নয় */
    fun isTransient(e: Throwable): Boolean = when (e) {
        is ContentNotAvailableException -> false
        is UnknownHostException -> false
        is IOException -> true
        is ParsingException -> true
        else -> false
    }
}
