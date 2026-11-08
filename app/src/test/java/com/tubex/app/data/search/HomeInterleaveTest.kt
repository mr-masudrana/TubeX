package com.tubex.app.data.search

import com.tubex.app.data.model.PlaylistEntry
import com.tubex.app.data.model.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeInterleaveTest {
    private fun vid(i: Int) = VideoItem("v$i", "u$i", "c", null, 0, 60, null, null)
    private fun pl(i: Int) = PlaylistEntry("p$i", "P$i", null, 10, null)
    private fun HomeRow.tag() = when (this) { is HomeRow.Video -> v.title; is HomeRow.Playlist -> p.name; is HomeRow.Shelf -> "SHELF" }

    @Test fun firstAfterFourThenEveryFive() {
        val rows = HomePlaylists.interleave((1..15).map(::vid), (1..3).map(::pl))
        val tags = rows.map { it.tag() }
        assertEquals("P1", tags[4])      // ৪টি ভিডিওর পর
        assertEquals("P2", tags[10])     // আরও ৫টির পর
        assertEquals("P3", tags[16])     // তার পর আরও ৫টি
        assertEquals(15 + 3, rows.size)
    }

    @Test fun noPlaylistsKeepsVideosOnly() {
        val rows = HomePlaylists.interleave((1..9).map(::vid), emptyList())
        assertEquals(9, rows.size)
        assertTrue(rows.all { it is HomeRow.Video })
    }

    @Test fun fewVideosGetNoDanglingPlaylist() {
        val rows = HomePlaylists.interleave((1..3).map(::vid), (1..2).map(::pl))
        assertEquals(3, rows.size)
    }

    @Test fun playlistsRunOutThenOnlyVideos() {
        val rows = HomePlaylists.interleave((1..30).map(::vid), listOf(pl(1)))
        assertEquals(1, rows.count { it is HomeRow.Playlist })
        assertEquals(31, rows.size)
    }

    @Test fun layoutHasShelfAndSingles() {
        val rows = HomePlaylists.layout((1..20).map(::vid), (1..9).map(::pl))
        val tags = rows.map { it.tag() }
        assertEquals("SHELF", tags[4])                       // ৪টি ভিডিওর পর সেকশন
        assertEquals((1..6).map { "P$it" }, (rows[4] as HomeRow.Shelf).items.map { it.name })
        val singles = rows.filterIsInstance<HomeRow.Playlist>().map { it.p.name }
        assertEquals(listOf("P7", "P8", "P9"), singles)      // সেকশনের বাকিগুলো, পুনরাবৃত্তি নেই
        assertEquals("P7", tags[9])                          // ৮ম ভিডিওর পর (সেকশনও ১টি সারি)
    }

    @Test fun layoutWithFewPlaylistsHasOnlyShelf() {
        val rows = HomePlaylists.layout((1..10).map(::vid), (1..4).map(::pl))
        assertEquals(1, rows.count { it is HomeRow.Shelf })
        assertEquals(0, rows.count { it is HomeRow.Playlist })
    }

    @Test fun layoutShelfShownEvenWithFewVideos() {
        val rows = HomePlaylists.layout((1..2).map(::vid), (1..6).map(::pl))
        assertTrue(rows.last() is HomeRow.Shelf)
    }
}
