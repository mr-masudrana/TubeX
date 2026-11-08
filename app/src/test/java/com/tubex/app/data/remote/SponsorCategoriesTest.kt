package com.tubex.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SponsorCategoriesTest {
    @Test fun defaultsKeepOldBehaviourWithoutFiller() {
        assertEquals(listOf("sponsor", "selfpromo", "interaction", "intro", "outro"), SponsorCategories.DEFAULT)
        assertFalse("filler" in SponsorCategories.DEFAULT)
        assertTrue("filler" in SponsorCategories.ALL)
    }

    @Test fun parseIgnoresUnknownAndKeepsCanonicalOrder() {
        assertEquals(listOf("sponsor", "filler"), SponsorCategories.parse("filler, bogus ,sponsor"))
        assertEquals(emptyList<String>(), SponsorCategories.parse(""))
        assertEquals(SponsorCategories.DEFAULT, SponsorCategories.parse(null))
    }

    @Test fun toggleAddsAndRemoves() {
        val withFiller = SponsorCategories.toggle(SponsorCategories.DEFAULT, "filler", true)
        assertEquals(SponsorCategories.ALL, withFiller)
        assertEquals(SponsorCategories.DEFAULT, SponsorCategories.toggle(withFiller, "filler", false))
        assertEquals(listOf("intro"), SponsorCategories.toggle(listOf("intro", "outro"), "outro", false))
    }

    @Test fun jsonArrayMatchesApiFormat() {
        assertEquals("[\"sponsor\",\"filler\"]", SponsorCategories.jsonArray(listOf("filler", "sponsor")))
        assertEquals("[]", SponsorCategories.jsonArray(emptyList()))
    }

    @Test fun csvRoundTrip() {
        val cats = listOf("sponsor", "outro", "filler")
        assertEquals(cats, SponsorCategories.parse(SponsorCategories.toCsv(cats)))
    }
}
