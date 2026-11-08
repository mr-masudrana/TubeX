package com.tubex.app.data.recommend

import com.tubex.app.data.model.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeMixTest {
    private fun v(id: String, views: Long = 100, live: Boolean = false) =
        VideoItem(id, id, "c", null, views, 60, null, null, isLive = live)

    @Test fun channelPicksNewIsFirstAndPopularIsTopViewedOfRest() {
        val list = listOf(v("n1", 5), v("n2", 5), v("a", 10), v("b", 900), v("c", 500), v("d", 50))
        val p = HomeMix.channelPicks(list)
        assertEquals(listOf("n1", "n2"), p.new.map { it.url })
        assertEquals(listOf("b", "c"), p.popular.map { it.url })
    }

    @Test fun channelPicksSkipsLiveAndUnknownViews() {
        val list = listOf(v("n1"), v("n2"), v("live", 9999, live = true), v("unk", -1), v("ok", 10))
        assertEquals(listOf("ok"), HomeMix.channelPicks(list).popular.map { it.url })
    }

    @Test fun composeGuaranteesQuotasEvenWhenRankedLow() {
        val ranked = (1..30).map { v("r$it") } + listOf(v("t1"), v("t2"), v("sn"), v("sp"))
        val out = HomeMix.compose(ranked, setOf("t1", "t2"), setOf("sn"), setOf("sp"))
        val urls = out.map { it.url }
        assertEquals(ranked.size, out.size)
        assertEquals(ranked.map { it.url }.toSet(), urls.toSet())
        // চারটি বিশেষ ভিডিও প্রথম ১৪টির মধ্যে উঠে আসে (২য়, ৫ম, ৮ম, ১১শ জায়গা)
        val first = urls.take(14)
        listOf("t1", "t2", "sn", "sp").forEach { assertTrue(it in first) }
        assertEquals("r1", urls[0])     // সাধারণ ক্রম অক্ষুণ্ণ
        assertEquals("sn", urls[1])     // নতুন-সাবস্ক্রিপশন আগে, তারপর ট্রেন্ডিং, তারপর জনপ্রিয়
        assertEquals("t1", urls[4])
    }

    @Test fun composeRespectsMaxAndPriority() {
        val ranked = (1..10).map { v("t$it") } + listOf(v("both"))
        val trending = (1..10).map { "t$it" }.toSet() + "both"
        val out = HomeMix.compose(ranked, trending, subNew = setOf("both"), subPopular = emptySet(), maxTrending = 2)
        val tagged = HashMap<String, HomeMix.Kind>()
        HomeMix.compose(ranked, trending, setOf("both"), emptySet(), maxTrending = 2) { x, k -> tagged[x.url] = k; x }
        assertEquals(HomeMix.Kind.SUB_NEW, tagged["both"])      // একাধিক গ্রুপে থাকলে সাবস্ক্রিপশন-নতুন
        assertEquals(2, tagged.values.count { it == HomeMix.Kind.TRENDING })
        assertEquals(ranked.size, out.size)
    }

    @Test fun composeWithNothingSpecialReturnsRankedAsIs() {
        val ranked = (1..5).map { v("r$it") }
        assertEquals(ranked, HomeMix.compose(ranked, emptySet(), emptySet(), emptySet()))
    }
}
