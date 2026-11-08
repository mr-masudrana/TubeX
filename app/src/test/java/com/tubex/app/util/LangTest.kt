package com.tubex.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class LangTest {
    @Test fun withoutContextReturnsBanglaKey() {
        L.apply(1)   // English চাইলেও Context নেই (JVM টেস্ট) — কী-ই ফেরত আসা উচিত
        assertEquals("সব", L.t("সব"))
        L.apply(0)
    }

    @Test fun formatReplacesPlaceholders() {
        L.apply(0)
        assertEquals("3টি ভিডিও, এক", L.f("{0}টি ভিডিও, {1}", 3 as Any?, "এক"))
    }

    @Test fun unknownModeFallsBackToBangla() {
        L.apply(99)
        assertTrue(!L.en)
    }

    private fun names(path: String): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(java.io.File(path))
        val list = doc.getElementsByTagName("string")
        return (0 until list.length).map { list.item(it) as Element }
            .filter { it.getAttribute("translatable") != "false" }.map { it.getAttribute("name") }.toSet()
    }

    /** ইংরেজি ও বাংলা রিসোর্সের নামের তালিকা একই থাকতে হবে (Gradle টেস্টের ওয়ার্কিং ডিরেক্টরি = app/) */
    @Test fun englishAndBanglaResourcesHaveSameNames() {
        val en = names("src/main/res/values/strings.xml")
        val bn = names("src/main/res/values-bn/strings.xml")
        assertEquals(emptySet<String>(), en - bn)
        assertEquals(emptySet<String>(), bn - en)
        assertTrue(en.size > 300)
    }
}
