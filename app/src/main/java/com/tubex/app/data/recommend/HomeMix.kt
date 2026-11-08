package com.tubex.app.data.recommend

import com.tubex.app.data.model.VideoItem

/**
 * হোম ফিডের "মিশ্রণ" (নেটওয়ার্ক ছাড়া, ইউনিট টেস্টযোগ্য): র‍্যাংক করা ফিডে প্রতিবার কিছু ট্রেন্ডিং ভিডিও এবং
 * সাবস্ক্রাইব করা চ্যানেলের নতুন ও জনপ্রিয় ভিডিও নিশ্চিতভাবে রাখা — র‍্যাংকিংয়ে নিচে পড়ে গেলেও।
 */
object HomeMix {
    const val MAX_TRENDING = 4
    const val MAX_NEW = 4
    const val MAX_POPULAR = 3

    enum class Kind { SUB_NEW, SUB_POPULAR, TRENDING }

    class ChannelPicks(val new: List<VideoItem>, val popular: List<VideoItem>)

    /**
     * একটি চ্যানেলের সাম্প্রতিক ভিডিও (নতুন থেকে পুরোনো ক্রমে) থেকে: নতুন = প্রথম [newCount]টি;
     * জনপ্রিয় = বাকিগুলোর মধ্যে ভিউ সবচেয়ে বেশি [popCount]টি (লাইভ ও ভিউ অজানা বাদ)।
     * এক্সট্র্যাক্টর "জনপ্রিয়" সর্টিং দেয় না, তাই এটা চ্যানেলের সাম্প্রতিক পেজের মধ্যে সবচেয়ে বেশি দেখা।
     */
    fun channelPicks(videos: List<VideoItem>, newCount: Int = 2, popCount: Int = 2): ChannelPicks {
        val fresh = videos.take(newCount)
        val popular = videos.drop(newCount).filter { it.views > 0 && !it.isLive }
            .sortedByDescending { it.views }.take(popCount)
        return ChannelPicks(fresh, popular)
    }

    /**
     * [ranked]-এর ক্রম বজায় রেখে ট্রেন্ডিং/নতুন/জনপ্রিয় থেকে সর্বোচ্চ কোটা বাছে (প্রতিটি গ্রুপে র‍্যাংকে সেরাগুলো),
     * তারপর সেগুলো পালাক্রমে প্রতি ৩টির মধ্যে ১টি জায়গায় (২য়, ৫ম, ৮ম…) বসায়। [tag] প্রতিটি বাছাই-করা ভিডিওতে কারণ বসাতে পারে।
     * একটি ভিডিও একাধিক গ্রুপে থাকলে অগ্রাধিকার: সাবস্ক্রাইব-নতুন, সাবস্ক্রাইব-জনপ্রিয়, ট্রেন্ডিং।
     */
    fun compose(
        ranked: List<VideoItem>, trending: Set<String>, subNew: Set<String>, subPopular: Set<String>,
        maxTrending: Int = MAX_TRENDING, maxNew: Int = MAX_NEW, maxPopular: Int = MAX_POPULAR,
        tag: (VideoItem, Kind) -> VideoItem = { v, _ -> v }
    ): List<VideoItem> {
        val newPicks = ranked.filter { it.url in subNew }.take(maxNew)
        val popPicks = ranked.filter { it.url in subPopular && it.url !in subNew }.take(maxPopular)
        val trendPicks = ranked.filter { it.url in trending && it.url !in subNew && it.url !in subPopular }.take(maxTrending)
        val picked = (newPicks + popPicks + trendPicks).mapTo(HashSet()) { it.url }
        if (picked.isEmpty()) return ranked

        val groups = listOf(
            newPicks.map { tag(it, Kind.SUB_NEW) },
            trendPicks.map { tag(it, Kind.TRENDING) },
            popPicks.map { tag(it, Kind.SUB_POPULAR) }
        )
        val specials = ArrayList<VideoItem>()
        for (i in 0 until (groups.maxOfOrNull { it.size } ?: 0)) groups.forEach { g -> g.getOrNull(i)?.let { specials += it } }

        val base = ranked.filter { it.url !in picked }
        val out = ArrayList<VideoItem>(ranked.size)
        var s = 0; var b = 0; var i = 0
        while (s < specials.size || b < base.size) {
            val special = s < specials.size && (i % 3 == 1 || b >= base.size)
            out += if (special) specials[s++] else base[b++]
            i++
        }
        return out
    }
}
