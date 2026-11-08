package com.tubex.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * প্লে কিউ: তালিকা, বর্তমান পজিশন, শাফল ও রিপিট। প্লেয়ার বা নেটওয়ার্কের কিছু জানে না —
 * কোন আইটেম চলবে তা ঠিক করে ফেরত দেয়, চালানোর কাজ MainViewModel-এর।
 */
class PlayQueue {
    var items by mutableStateOf<List<QItem>>(emptyList()); private set
    var index by mutableStateOf(0); private set
    var shuffle by mutableStateOf(false); private set
    /** কিউ কোনো প্লেলিস্ট থেকে এলে তার নাম (অফিসিয়াল YouTube-এর "Playlist: …" প্যানেলের মতো), নইলে null */
    var title by mutableStateOf<String?>(null); private set
    /** 0 বন্ধ, 1 কিউ, 2 একটি ভিডিও */
    var repeat by mutableStateOf(0); private set
    private val played = HashSet<String>()

    /**
     * চলতি ভিডিওটি কিউ থেকে মুছে ফেলা হলে true (ভিডিও চলতেই থাকে)। তখন [index] মানে "মুছে যাওয়া জায়গার ঠিক আগের আইটেম",
     * তাই advance() পরের আইটেম (index+1) থেকেই চালায় এবং UI-তে কোনো সারি "চলছে" দেখায় না।
     */
    var detached by mutableStateOf(false); private set

    /** UI-তে হাইলাইটের জন্য: চলতি আইটেমের পজিশন, না থাকলে -1 */
    val currentIndex get() = if (detached) -1 else index

    /** আগের আইটেমে ফেরা যাবে কি না */
    val canBack get() = if (detached) index in items.indices else index > 0

    /** [remove]-এর ফলাফল: আনডুর জন্য আইটেম, আগের পজিশন ও সেটি চলতি ছিল কি না */
    data class Removed(val item: QItem, val at: Int, val wasCurrent: Boolean)

    val isEmpty get() = items.isEmpty()

    /** নতুন কিউ বসিয়ে প্রথম চালানোর আইটেম ফেরত দেয় */
    fun start(list: List<QItem>, startIndex: Int, shuffleStart: Boolean = false, title: String? = null): QItem {
        items = list; played.clear(); this.title = title; detached = false
        if (shuffleStart) { shuffle = true; index = list.indices.random() }
        else index = startIndex.coerceIn(0, list.lastIndex)
        return list[index]
    }

    fun clear() { items = emptyList(); index = 0; played.clear(); title = null; detached = false }

    // @JvmName: `private set` সেটারের সাথে JVM সিগনেচার সংঘর্ষ এড়াতে (Kotlin থেকে নাম একই থাকে)
    @JvmName("applyShuffle") fun setShuffle(v: Boolean) { shuffle = v; played.clear() }
    @JvmName("applyRepeat") fun setRepeat(mode: Int) { repeat = mode }

    /** ভিডিও শেষে পরের আইটেম (শাফল/রিপিট মেনে); না থাকলে null। পজিশন নিজে এগোয়। */
    fun advance(): QItem? {
        if (items.isEmpty()) return null
        val n = items.size
        val cur = if (detached) -1 else index
        items.getOrNull(cur)?.let { played.add(it.url) }
        val next: Int = if (shuffle) {
            val remaining = items.indices.filter { items[it].url !in played && it != cur }
            if (remaining.isNotEmpty()) remaining.random()
            else if (repeat == 1) {
                played.clear()
                items.indices.filter { it != cur }.randomOrNull() ?: index
            } else -1
        } else if (index + 1 < n) index + 1
        else if (repeat == 1) 0
        else -1
        if (next < 0) return null
        index = next; detached = false
        return items[next]
    }

    /** আগের আইটেমে ফেরা; কিউর শুরুতে থাকলে null */
    fun back(): QItem? {
        if (items.isEmpty() || !canBack) return null
        if (detached) { detached = false; return items[index] }   // মুছে যাওয়া জায়গার আগের আইটেম
        index -= 1
        return items[index]
    }

    fun select(i: Int): QItem? {
        val q = items.getOrNull(i) ?: return null
        index = i; detached = false
        return q
    }

    /** কিউ খালি থাকলে [seed] (বর্তমানে চলা ভিডিও) প্রথমে বসে */
    fun add(item: QItem, playNext: Boolean, seed: QItem?) {
        if (items.isEmpty()) {
            items = if (seed != null) listOf(seed) else emptyList()
            index = 0
        }
        val at = if (playNext) (index + 1).coerceIn(0, items.size) else items.size
        items = items.toMutableList().also { it.add(at, item) }
    }

    /** আইটেম মোছে। চলতি আইটেম মুছলে ভিডিও চলতেই থাকে, পরের ভিডিও হবে মুছে যাওয়া জায়গার আইটেম। */
    fun remove(i: Int): Removed? {
        val item = items.getOrNull(i) ?: return null
        val wasCurrent = !detached && i == index
        items = items.toMutableList().also { it.removeAt(i) }
        when {
            wasCurrent -> { detached = true; index = i - 1 }
            i <= index -> index -= 1
        }
        if (items.isEmpty()) { index = 0; detached = false }
        return Removed(item, i, wasCurrent)
    }

    /** [remove] আনডু: আইটেম আগের জায়গায় ফেরে; চলতি ছিল হলে আবার চলতি হিসেবে চিহ্নিত হয় */
    fun restore(r: Removed) {
        val at = r.at.coerceIn(0, items.size)
        items = items.toMutableList().also { it.add(at, r.item) }
        if (r.wasCurrent && detached && at == index + 1) { index = at; detached = false }
        else if (at <= index) index += 1
    }

    fun move(i: Int, delta: Int) = moveTo(i, i + delta)

    /** [from] থেকে [to] পজিশনে সরানো (ড্র্যাগ-অ্যান্ড-ড্রপের জন্য); চলতি আইটেমের pointer ঠিক থাকে */
    fun moveTo(from: Int, to: Int) {
        if (from !in items.indices || to !in items.indices || from == to) return
        items = items.toMutableList().also { it.add(to, it.removeAt(from)) }
        if (detached) {
            // index = "মুছে যাওয়া জায়গার আগের আইটেম": ফাঁক পার হলে সংখ্যা বদলায়
            if (from <= index && to > index) index -= 1
            else if (from > index && to <= index) index += 1
        } else when {
            from == index -> index = to
            from < index && to >= index -> index -= 1
            from > index && to <= index -> index += 1
        }
    }

    /** প্লেলিস্টের বাকি পেজ আসার পর কিউ বাড়ানো (পজিশন অপরিবর্তিত) */
    fun replaceItems(list: List<QItem>) { items = list }
}
