package com.tubex.app.data.remote

import com.tubex.app.data.model.ChannelRef

import org.json.JSONArray
import org.json.JSONObject

/**
 * কোলাব ভিডিও (একই ভিডিও একাধিক চ্যানেলের): এক্সট্র্যাক্টর শুধু একটি আপলোডার দেয়, তাই ভিডিও পেজের
 * ytInitialData-র `showDialogCommand`-এর ভেতরের চ্যানেল তালিকা থেকে সব চ্যানেল নেওয়া হয়।
 * পার্সিং ব্যর্থ হলে বা ২টির কম পেলে খালি তালিকা — মানে সাধারণ ভিডিওর মতোই একটি লোগো।
 */
object Collab {
    /** পেজ আনা লাগবে কি না: চ্যানেল URL নেই, অথবা নামে "A and B" / "A & B" / "A, B" ধরন (ভুল হলেও ক্ষতি নেই — পার্সে কিছু মেলে না) */
    fun mayBeCollab(channel: String, channelUrl: String?): Boolean {
        if (channel.isBlank()) return false
        if (channelUrl.isNullOrBlank()) return true
        return Regex("""\s(and|&|এবং)\s|,\s|\s\+\s\d+""", RegexOption.IGNORE_CASE).containsMatchIn(channel)
    }

    fun parse(html: String): List<ChannelRef> {
        val root = extractInitialData(html) ?: return emptyList()
        return parseData(root)
    }

    fun parseData(root: Any): List<ChannelRef> {
        val found = LinkedHashMap<String, ChannelRef>()
        walk(root) { key, node ->
            if (key == "showDialogCommand") collectItems(node, found)
        }
        return if (found.size >= 2) found.values.toList() else emptyList()
    }

    private fun collectItems(node: Any?, out: MutableMap<String, ChannelRef>) {
        walk(node) { key, v ->
            if (key == "listItemViewModel" && v is JSONObject) toRef(v)?.let { out.putIfAbsent(it.url, it) }
        }
    }

    private fun toRef(o: JSONObject): ChannelRef? {
        val name = o.optJSONObject("title")?.optString("content").orEmpty()
        val id = findString(o, "browseId") ?: return null
        if (name.isBlank() || !id.startsWith("UC")) return null
        val sources = o.optJSONObject("leadingAccessory")?.optJSONObject("avatarViewModel")
            ?.optJSONObject("image")?.optJSONArray("sources")
        val avatar = sources?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it)?.optString("url") }.lastOrNull { it.isNotBlank() } }
        return ChannelRef(name, "https://www.youtube.com/channel/$id", avatar)
    }

    private fun findString(node: Any?, key: String): String? {
        var res: String? = null
        walk(node) { k, v -> if (res == null && k == key && v is String && v.isNotBlank()) res = v }
        return res
    }

    /** JSON-এর সব (কী, মান) জোড়ায় যায়; অ্যারোর আইটেমের কী = null */
    private fun walk(node: Any?, visit: (String?, Any?) -> Unit) {
        when (node) {
            is JSONObject -> node.keys().forEach { k ->
                val v = node.opt(k)
                visit(k, v)
                walk(v, visit)
            }
            is JSONArray -> for (i in 0 until node.length()) walk(node.opt(i), visit)
        }
    }

    /** HTML থেকে ytInitialData-র JSON অবজেক্ট (ব্রেস মিলিয়ে, স্ট্রিংয়ের ভেতরের ব্রেস বাদ দিয়ে) */
    fun extractInitialData(html: String): JSONObject? {
        val at = html.indexOf("ytInitialData")
        if (at < 0) return null
        val start = html.indexOf('{', at)
        if (start < 0) return null
        var depth = 0; var inStr = false; var esc = false
        for (i in start until html.length) {
            val c = html[i]
            if (inStr) {
                if (esc) esc = false else if (c == '\\') esc = true else if (c == '"') inStr = false
                continue
            }
            when (c) {
                '"' -> inStr = true
                '{' -> depth++
                '}' -> if (--depth == 0) return runCatching { JSONObject(html.substring(start, i + 1)) }.getOrNull()
            }
        }
        return null
    }
}
