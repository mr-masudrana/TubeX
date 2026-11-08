package com.tubex.app.data.cache

import com.tubex.app.data.model.ChannelRef
import com.tubex.app.data.remote.Collab

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe

/** ভিডিও URL -> কোলাব চ্যানেলের তালিকা। খালি তালিকা = দেখা হয়েছে, কোলাব নয়। কার্ড দেখা গেলে একবার আনে। */
object CollabCache {
    val map = mutableStateMapOf<String, List<ChannelRef>>()
    private val inFlight = HashSet<String>()
    private val failedAt = HashMap<String, Long>()     // নেটওয়ার্ক ব্যর্থ হলে ১ মিনিট পরে আবার চেষ্টা
    private const val RETRY_MS = 60_000L
    private val gate = Semaphore(2)

    suspend fun ensure(videoUrl: String) {
        if (videoUrl.isBlank() || videoUrl in map || videoUrl in inFlight) return
        val failed = failedAt[videoUrl]
        if (failed != null && System.currentTimeMillis() - failed < RETRY_MS) return
        inFlight.add(videoUrl)
        try {
            val html = gate.withPermit {
                withContext(Dispatchers.IO) {
                    runCatching {
                        NewPipe.getDownloader().get(
                            videoUrl,
                            mapOf("Accept-Language" to listOf("en-US,en;q=0.9"), "Cookie" to listOf("SOCS=CAI"))
                        ).responseBody()
                    }.getOrNull()
                }
            }
            if (html == null) { failedAt[videoUrl] = System.currentTimeMillis(); return }
            map[videoUrl] = withContext(Dispatchers.Default) { runCatching { Collab.parse(html) }.getOrDefault(emptyList()) }
            failedAt.remove(videoUrl)
        } catch (e: CancellationException) {
            throw e      // স্ক্রল করে সরে গেলে মার্কার থাকে না, পরে আবার চেষ্টা হবে
        } finally {
            inFlight.remove(videoUrl)
        }
    }
}
