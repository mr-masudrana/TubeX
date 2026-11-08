package com.tubex.app.data.remote

/**
 * SponsorBlock-এর যেসব ক্যাটাগরি নিজে বাদ দেওয়া যায়।
 * ডিফল্টে আগের মতো ৫টি চালু; filler (অপ্রয়োজনীয় কথা/ট্যানজেন্ট) আগ্রহের বিষয়ে আসল কনটেন্টও কেটে ফেলতে পারে,
 * তাই ডিফল্টে বন্ধ — সেটিংসে চালু করা যায়।
 */
object SponsorCategories {
    val ALL = listOf("sponsor", "selfpromo", "interaction", "intro", "outro", "filler")
    val DEFAULT = listOf("sponsor", "selfpromo", "interaction", "intro", "outro")

    /** সংরক্ষণ-স্ট্রিং (কমা-বিভক্ত) → বৈধ ক্যাটাগরি, ALL-এর ক্রমে */
    fun parse(csv: String?): List<String> {
        if (csv == null) return DEFAULT
        val set = csv.split(',').map { it.trim() }.toSet()
        return ALL.filter { it in set }
    }

    fun toCsv(cats: Collection<String>): String = ALL.filter { it in cats }.joinToString(",")

    fun toggle(current: List<String>, cat: String, on: Boolean): List<String> =
        ALL.filter { if (it == cat) on else it in current }

    /** API-র `categories` প্যারামিটার, যেমন ["sponsor","intro"] */
    fun jsonArray(cats: Collection<String>): String =
        ALL.filter { it in cats }.joinToString(",", "[", "]") { "\"$it\"" }
}
