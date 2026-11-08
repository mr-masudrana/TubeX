package com.tubex.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollabTest {
    // ইউটিউবের কোলাব ডায়ালগের গঠন (showDialogCommand → listItemViewModel); আসল ইউটিউব JSON-এর সাথে মিলিয়ে দেখা দরকার
    private fun item(name: String, id: String, avatar: String) = """
        {"listItemViewModel":{"title":{"content":"$name"},
         "leadingAccessory":{"avatarViewModel":{"image":{"sources":[{"url":"${avatar}_s"},{"url":"$avatar"}]}}},
         "rendererContext":{"commandContext":{"onTap":{"innertubeCommand":{"browseEndpoint":{"browseId":"$id"}}}}}}}
    """.trimIndent()

    private fun page(vararg items: String) = """
        <html><script>var ytInitialData = {"contents":{"owner":{"title":{"runs":[{"text":"A and B","navigationEndpoint":
        {"showDialogCommand":{"panel":{"listView":{"listItems":[${items.joinToString(",")}]}}}}}]}},"note":"has } and { in string"}};</script></html>
    """.trimIndent()

    @Test fun parsesTwoChannels() {
        val r = Collab.parse(page(item("Alpha", "UCaaa", "https://x/a"), item("Beta", "UCbbb", "https://x/b")))
        assertEquals(listOf("Alpha", "Beta"), r.map { it.name })
        assertEquals("https://www.youtube.com/channel/UCaaa", r[0].url)
        assertEquals("https://x/b", r[1].avatar)   // শেষ (বড়) ছবি
    }

    @Test fun singleChannelIsNotCollab() {
        assertTrue(Collab.parse(page(item("Alpha", "UCaaa", "https://x/a"))).isEmpty())
    }

    @Test fun duplicatesAreMerged() {
        val a = item("Alpha", "UCaaa", "https://x/a")
        assertTrue(Collab.parse(page(a, a)).isEmpty())
    }

    @Test fun ignoresNonChannelIdsAndMissingData() {
        assertTrue(Collab.parse(page(item("Alpha", "PLxxx", "u"), item("Beta", "UCbbb", "u"))).isEmpty())
        assertTrue(Collab.parse("<html>no data</html>").isEmpty())
        assertNull(Collab.extractInitialData("var ytInitialData = {\"a\":"))
    }

    @Test fun detectsCandidates() {
        assertTrue(Collab.mayBeCollab("A and B", "https://youtube.com/channel/UC1"))
        assertTrue(Collab.mayBeCollab("A", null))
        assertFalse(Collab.mayBeCollab("Veritasium", "https://youtube.com/channel/UC1"))
        assertFalse(Collab.mayBeCollab("", null))
    }
}
