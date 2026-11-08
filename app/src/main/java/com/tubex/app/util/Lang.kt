package com.tubex.app.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * UI-র ভাষা। কোডে `L.t(<বাংলা লেখা>)` — বাংলা লেখাটাই কী।
 * বাংলা মোডে লেখাটা সরাসরি ফেরত আসে; অন্য ভাষায় [BN_STRING_IDS] (জেনারেটেড) দিয়ে
 * `res/values[-xx]/strings.xml` থেকে অনুবাদ আনা হয়। রিসোর্স বা কী না পেলে বাংলা লেখাই দেখায়।
 *
 * নতুন লেখা/ভাষা যোগ করার নিয়ম: `tools/gen_string_ids.py` ও CONTRIBUTING.md দেখুন।
 */
object L {
    private var appCtx: Context? = null
    private var res: Resources? = null
    private val cache = ConcurrentHashMap<String, String>()

    /** বর্তমান UI ভাষার কোড ("bn", "en", …); স্টেট, তাই বদলালে UI নিজে রিকম্পোজ হয় */
    private var lang by mutableStateOf("bn")

    /** UI বাংলা নয় — NewPipe-এর লোকালাইজেশন ও তারিখ ফরম্যাটে ব্যবহৃত */
    val en: Boolean get() = lang != "bn"

    /**
     * বাংলা, English ও সিস্টেম ছাড়া অতিরিক্ত ভাষা: (ভাষার কোড, নিজের ভাষায় নাম)।
     * মোড নম্বর = ৩ + তালিকার ইনডেক্স। যেমন হিন্দি যোগ করতে `res/values-hi/strings.xml` বানিয়ে এখানে `"hi" to "हिन्दी"` লিখুন।
     */
    val extra: List<Pair<String, String>> = emptyList()

    fun init(ctx: Context) { appCtx = ctx.applicationContext }

    /** mode: 0 বাংলা, 1 English, 2 সিস্টেম, 3+ = [extra] */
    fun apply(mode: Int) {
        val tag = when (mode) {
            0 -> "bn"
            1 -> "en"
            2 -> Locale.getDefault().language.ifBlank { "en" }
            else -> extra.getOrNull(mode - 3)?.first ?: "bn"
        }
        cache.clear()
        res = if (tag == "bn") null else localized(tag)
        lang = tag
    }

    private fun localized(tag: String): Resources? {
        val c = appCtx ?: return null
        val cfg = Configuration(c.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
        return c.createConfigurationContext(cfg).resources
    }

    fun t(bn: String): String {
        if (lang == "bn") return bn
        val r = res ?: return bn
        return cache.getOrPut(bn) {
            val id = BN_STRING_IDS[bn] ?: return bn
            try { r.getString(id) } catch (_: Resources.NotFoundException) { bn }
        }
    }

    fun f(bn: String, vararg args: Any?): String {
        var s = t(bn)
        for (i in args.indices) s = s.replace("{$i}", args[i].toString())
        return s
    }
}
