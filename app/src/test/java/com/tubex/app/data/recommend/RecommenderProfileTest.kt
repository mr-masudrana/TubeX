package com.tubex.app.data.recommend

import com.tubex.app.data.local.entity.HistoryEntity
import com.tubex.app.data.local.entity.InterestEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommenderProfileTest {
    private val now = 1_800_000_000_000L
    private fun e(topic: String, score: Double, at: Long = now) = InterestEntity(topic, score, at)
    private fun h(title: String, ch: String) = HistoryEntity("u-$title-${ch}", title, ch, null, null, 100, 0, now)

    @Test fun summary_splitsChannelsAndKeywords_sortedByScore() {
        val s = Recommender.summary(listOf(
            e("ch:alpha", 4.0), e("ch:beta", 9.0), e("kw:cricket", 3.0), e("kw:music", 7.0), e("t:wd-m:kw:cricket", 2.0)
        ), now)
        assertEquals(listOf("beta", "alpha"), s.channels.map { it.first })
        assertEquals(listOf("music", "cricket"), s.keywords.map { it.first })
        assertEquals(2, s.totalChannels)
        assertEquals(2, s.totalKeywords)
    }

    @Test fun summary_negativeScoresGoToAvoided_mostDislikedFirst() {
        val s = Recommender.summary(listOf(e("kw:spam", -1.5), e("ch:bad", -3.0), e("kw:fine", 2.0), e("kw:meh", -0.1)), now)
        assertEquals(listOf("ch:bad", "kw:spam"), s.avoided.map { it.first })   // -0.1 ছোট, ধরা হয়নি
        assertEquals(listOf("fine"), s.keywords.map { it.first })
    }

    @Test fun summary_scoresDecayWithTime() {
        val old = now - 28L * 24 * 3600 * 1000      // দুই half-life আগে
        val s = Recommender.summary(listOf(e("kw:fresh", 1.0), e("kw:old", 1.0, old)), now)
        val m = s.keywords.toMap()
        assertEquals(1.0, m["fresh"]!!, 1e-9)
        assertEquals(0.25, m["old"]!!, 1e-9)
    }

    @Test fun summary_bucketTotalsHaveAllEightKeys_andOnlyCountKeywords() {
        val s = Recommender.summary(listOf(
            e("t:wd-m:kw:news", 2.0), e("t:wd-m:kw:tech", 1.0), e("t:we-n:kw:movie", 4.0), e("t:wd-m:ch:someone", 9.0)
        ), now)
        assertEquals(ProfileSummary.BUCKETS.toSet(), s.bucketTotals.keys)
        assertEquals(3.0, s.bucketTotals["wd-m"]!!, 1e-9)     // চ্যানেলের কপি গোনা হয় না
        assertEquals(4.0, s.bucketTotals["we-n"]!!, 1e-9)
        assertEquals(0.0, s.bucketTotals["we-a"]!!, 1e-9)
        assertEquals(listOf("news", "tech"), s.bucketTops["wd-m"])
    }

    @Test fun summary_empty() {
        val s = Recommender.summary(emptyList(), now)
        assertTrue(s.isEmpty)
        assertEquals(8, s.bucketTotals.size)
    }

    @Test fun relatedKeys_includesBucketCopies_butNotOtherTopics() {
        val all = listOf(e("kw:cricket", 1.0), e("t:wd-m:kw:cricket", 1.0), e("t:we-n:kw:cricket", 1.0),
            e("kw:cricketer", 1.0), e("t:wd-m:kw:football", 1.0), e("ch:cricket", 1.0))
        assertEquals(setOf("kw:cricket", "t:wd-m:kw:cricket", "t:we-n:kw:cricket"),
            Recommender.relatedKeys(all, "kw:cricket").toSet())
    }

    @Test fun relatedKeys_channelNameWithColon() {
        val all = listOf(e("ch:a: b", 1.0), e("t:wd-m:ch:a: b", 1.0), e("t:wd-m:ch:a", 1.0))
        assertEquals(setOf("ch:a: b", "t:wd-m:ch:a: b"), Recommender.relatedKeys(all, "ch:a: b").toSet())
    }

    @Test fun dampen_scalesScoreAndRefreshesTimestamp() {
        val d = Recommender.dampen(e("kw:x", 10.0), 0.3, now)
        assertEquals(3.0, d.score, 1e-9)
        assertEquals(now, d.updatedAt)
        assertEquals("kw:x", d.topic)
    }

    @Test fun boredKeys_needsFourOfLastTen() {
        // সাম্প্রতিক ১০টি: ৪টি ক্রিকেট (একই চ্যানেল), ৩টি রান্না, ৩টি আলাদা
        val recent = List(4) { h("cricket highlights $it", "SportsTV") } +
            List(3) { h("cooking recipe $it", "Chef") } +
            List(3) { h("travel$it", "Guide$it") }
        val bored = Recommender.boredKeys(recent)
        assertTrue("kw:cricket" in bored)
        assertTrue("ch:sportstv" in bored)
        assertFalse("kw:cooking" in bored)      // মাত্র ৩ বার
        assertFalse("ch:chef" in bored)
    }

    @Test fun boredKeys_looksOnlyAtLastTen() {
        val recent = List(10) { h("news$it", "Ch$it") } + List(5) { h("cricket $it", "SportsTV") }
        assertFalse("kw:cricket" in Recommender.boredKeys(recent))   // ১১তম থেকে পরেরগুলো গোনা হয় না
    }
}
