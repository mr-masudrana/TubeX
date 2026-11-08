package com.tubex.app.data.recommend

/**
 * রিকমেন্ডেশন ড্যাশবোর্ডের জন্য প্রোফাইলের সারসংক্ষেপ (সব স্কোর সময়ের সাথে কমার হিসাব ধরে)।
 * channels/keywords-এর নামে "ch:"/"kw:" উপসর্গ নেই।
 */
data class ProfileSummary(
    val channels: List<Pair<String, Double>>,
    val keywords: List<Pair<String, Double>>,
    /** "আগ্রহ নেই" ইত্যাদিতে নেগেটিভ স্কোর পাওয়া বিষয়/চ্যানেল ("kw:…" / "ch:…"), সবচেয়ে অপছন্দেরটা আগে */
    val avoided: List<Pair<String, Double>>,
    /** ৮টি সময়-বাকেটের (wd/we × m/a/e/n) মোট আগ্রহ-স্কোর; সব ৮টি কী-ই থাকে */
    val bucketTotals: Map<String, Double>,
    /** প্রতি বাকেটের শীর্ষ কিওয়ার্ড */
    val bucketTops: Map<String, List<String>>,
    val totalChannels: Int,
    val totalKeywords: Int
) {
    val isEmpty get() = channels.isEmpty() && keywords.isEmpty()

    companion object {
        val BUCKETS = listOf("wd-m", "wd-a", "wd-e", "wd-n", "we-m", "we-a", "we-e", "we-n")
        val EMPTY = ProfileSummary(
            emptyList(), emptyList(), emptyList(), BUCKETS.associateWith { 0.0 },
            BUCKETS.associateWith { emptyList<String>() }, 0, 0
        )
    }
}
