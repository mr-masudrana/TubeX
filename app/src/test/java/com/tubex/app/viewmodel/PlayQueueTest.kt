package com.tubex.app.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayQueueTest {
    private fun q(vararg ids: String) = ids.map { QItem(it, it, "c", null) }

    @Test fun start_setsIndexAndReturnsItem() {
        val pq = PlayQueue()
        assertEquals("b", pq.start(q("a", "b", "c"), 1).url)
        assertEquals(1, pq.index)
    }

    @Test fun advance_walksThenStopsWithoutRepeat() {
        val pq = PlayQueue(); pq.start(q("a", "b"), 0)
        assertEquals("b", pq.advance()?.url)
        assertNull(pq.advance())
    }

    @Test fun advance_repeatQueueWraps() {
        val pq = PlayQueue(); pq.start(q("a", "b"), 1); pq.setRepeat(1)
        assertEquals("a", pq.advance()?.url)
    }

    @Test fun advance_shuffleVisitsEachOnceBeforeEnding() {
        val pq = PlayQueue(); pq.start(q("a", "b", "c"), 0); pq.setShuffle(true)
        val seen = mutableSetOf("a")
        repeat(2) { seen.add(pq.advance()!!.url) }
        assertEquals(setOf("a", "b", "c"), seen)
        assertNull(pq.advance())
    }

    @Test fun back_stopsAtStart() {
        val pq = PlayQueue(); pq.start(q("a", "b"), 1)
        assertEquals("a", pq.back()?.url)
        assertNull(pq.back())
    }

    @Test fun add_playNextInsertsAfterCurrent_andSeedsEmptyQueue() {
        val pq = PlayQueue()
        pq.add(QItem("n", "n", "c", null), playNext = true, seed = QItem("cur", "cur", "c", null))
        assertEquals(listOf("cur", "n"), pq.items.map { it.url })
        pq.add(QItem("m", "m", "c", null), playNext = false, seed = null)
        assertEquals(listOf("cur", "n", "m"), pq.items.map { it.url })
    }

    @Test fun remove_andMove_keepCurrentPointer() {
        val pq = PlayQueue(); pq.start(q("a", "b", "c"), 1)
        pq.move(0, 1)                       // b, a, c — বর্তমান b এখন ০
        assertEquals(0, pq.index)
        pq.remove(2)
        assertEquals(listOf("b", "a"), pq.items.map { it.url })
        pq.clear()
        assertTrue(pq.isEmpty)
    }

    @Test fun moveTo_keepsCurrentPointer_acrossAllCases() {
        val pq = PlayQueue(); pq.start(q("a", "b", "c", "d"), 1)     // বর্তমান b
        pq.moveTo(3, 0)                                              // d, a, b, c — b এখন ২
        assertEquals(listOf("d", "a", "b", "c"), pq.items.map { it.url })
        assertEquals(2, pq.index)
        pq.moveTo(2, 3)                                              // বর্তমান নিজেই সরলে pointer সাথে যায়
        assertEquals("b", pq.items[pq.index].url)
        pq.moveTo(0, 3)                                              // d শেষে; বর্তমান এক ধাপ এগোয়নি, পিছিয়েছে
        assertEquals("b", pq.items[pq.index].url)
    }

    @Test fun moveTo_ignoresInvalidOrSameIndex() {
        val pq = PlayQueue(); pq.start(q("a", "b"), 0)
        pq.moveTo(0, 0); pq.moveTo(-1, 1); pq.moveTo(0, 5)
        assertEquals(listOf("a", "b"), pq.items.map { it.url })
    }

    @Test fun remove_currentItem_detachesAndNextIsFollowingItem() {
        val pq = PlayQueue(); pq.start(q("a", "b", "c"), 1)
        val r = pq.remove(1)!!
        assertTrue(r.wasCurrent)
        assertTrue(pq.detached)
        assertEquals(-1, pq.currentIndex)                 // কোনো সারি "চলছে" দেখায় না
        assertEquals("c", pq.advance()?.url)              // মুছে যাওয়া জায়গার আইটেম
        assertEquals(1, pq.currentIndex)
    }

    @Test fun remove_currentFirstItem_nextIsNewFirst() {
        val pq = PlayQueue(); pq.start(q("a", "b"), 0)
        pq.remove(0)
        assertEquals("b", pq.advance()?.url)
    }

    @Test fun remove_beforeCurrent_shiftsPointer() {
        val pq = PlayQueue(); pq.start(q("a", "b", "c"), 2)
        pq.remove(0)
        assertEquals(1, pq.index)
        assertEquals("c", pq.items[pq.index].url)
        assertFalse(pq.detached)
    }

    @Test fun back_afterRemovingCurrent_goesToPreviousItem() {
        val pq = PlayQueue(); pq.start(q("a", "b", "c"), 1)
        pq.remove(1)
        assertTrue(pq.canBack)
        assertEquals("a", pq.back()?.url)
        assertFalse(pq.detached)
    }

    @Test fun restore_undoesRemovalOfCurrent() {
        val pq = PlayQueue(); pq.start(q("a", "b", "c"), 1)
        val r = pq.remove(1)!!
        pq.restore(r)
        assertEquals(listOf("a", "b", "c"), pq.items.map { it.url })
        assertFalse(pq.detached)
        assertEquals(1, pq.currentIndex)
    }

    @Test fun restore_undoesRemovalOfOtherItem_keepsCurrent() {
        val pq = PlayQueue(); pq.start(q("a", "b", "c"), 2)
        val r = pq.remove(0)!!
        pq.restore(r)
        assertEquals(listOf("a", "b", "c"), pq.items.map { it.url })
        assertEquals("c", pq.items[pq.index].url)
    }

    @Test fun remove_lastItem_emptiesQueueCleanly() {
        val pq = PlayQueue(); pq.start(q("a"), 0)
        pq.remove(0)
        assertTrue(pq.isEmpty)
        assertFalse(pq.detached)
        assertNull(pq.advance())
    }
}

