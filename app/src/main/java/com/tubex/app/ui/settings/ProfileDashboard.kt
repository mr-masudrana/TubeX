package com.tubex.app.ui.settings

import com.tubex.app.data.recommend.ProfileSummary
import com.tubex.app.data.recommend.Recommender
import com.tubex.app.ui.theme.Bg
import com.tubex.app.ui.theme.Chip
import com.tubex.app.ui.theme.Sub
import com.tubex.app.ui.theme.YtRed
import com.tubex.app.util.L
import com.tubex.app.viewmodel.MainViewModel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * রিকমেন্ডেশন ট্রান্সপারেন্সি ড্যাশবোর্ড: অ্যাপ আপনার সম্পর্কে কী শিখেছে, কখন কী দেখেন,
 * কোনটা এখন কম দেখানো হচ্ছে — আর প্রতিটি আগ্রহ আলাদাভাবে কমানো/ভুলিয়ে দেওয়া/ব্লক করার নিয়ন্ত্রণ।
 */
@Composable
internal fun ProfileDashboard(vm: MainViewModel) {
    val p = vm.profile
    val history by vm.history.collectAsState()
    val blocks by vm.blocks.collectAsState()
    var showHow by remember { mutableStateOf(false) }

    Text(L.t("সব ডেটা শুধু এই ফোনে থাকে। এখানে দেখুন অ্যাপ আপনার সম্পর্কে কী শিখেছে — আর চাইলে বদলে দিন।"), color = Sub, fontSize = 12.sp)
    Spacer(Modifier.height(10.dp))

    // ---- সারসংক্ষেপ ----
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(p.totalKeywords.toString(), L.t("বিষয়"), Modifier.weight(1f))
        StatCard(p.totalChannels.toString(), L.t("চ্যানেল"), Modifier.weight(1f))
        StatCard(history.size.toString(), L.t("ইতিহাস"), Modifier.weight(1f))
        StatCard(blocks.size.toString(), L.t("ব্লক"), Modifier.weight(1f))
    }

    if (p.isEmpty) {
        Text(
            L.t("এখনো কিছু শেখেনি — কয়েকটি ভিডিও দেখুন বা সার্চ করুন।"),
            color = Sub, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp)
        )
    }

    // ---- কখন কী দেখেন ----
    DashTitle(L.t("কখন কী দেখেন"), L.t("কর্মদিবস/ছুটির দিন ও দিনের সময় অনুযায়ী আপনার আলাদা পছন্দ। ঘরে ট্যাপ করুন।"))
    var sel by remember { mutableStateOf(Recommender.bucket()) }
    BucketGrid(p, sel) { sel = it }
    val tops = p.bucketTops[sel].orEmpty()
    Text(
        if (tops.isEmpty()) L.f("{0}: এই সময়ের জন্য এখনো যথেষ্ট ডেটা নেই।", Recommender.bucketLabel(sel))
        else L.f("{0}: {1}", Recommender.bucketLabel(sel), tops.joinToString(", ")),
        fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)
    )

    // ---- প্রিয় চ্যানেল / বিষয় ----
    if (p.channels.isNotEmpty()) {
        DashTitle(L.t("প্রিয় চ্যানেল"), null)
        val max = p.channels.maxOf { it.second }
        p.channels.forEach { (name, score) -> InterestControlRow("ch:$name", name, score / max, vm) }
    }
    if (p.keywords.isNotEmpty()) {
        DashTitle(L.t("প্রিয় বিষয়"), null)
        val max = p.keywords.maxOf { it.second }
        p.keywords.forEach { (word, score) -> InterestControlRow("kw:$word", word, score / max, vm) }
    }

    // ---- এখন কম দেখানো হচ্ছে ----
    if (p.avoided.isNotEmpty() || vm.boredNow.isNotEmpty()) {
        DashTitle(L.t("এখন কম দেখানো হচ্ছে"), null)
        if (vm.boredNow.isNotEmpty()) {
            Text(L.t("বিরক্তি: সাম্প্রতিক ১০টি ভিডিওর মধ্যে ৪+ বার এসেছে, তাই সাময়িক কম আসবে:"), color = Sub, fontSize = 12.sp)
            Text(vm.boredNow.joinToString(", ") { keyLabel(it) }, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
        }
        p.avoided.forEach { (key, _) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(L.f("আগ্রহ নেই: {0}", keyLabel(key)), fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                TextButton(onClick = { vm.forgetInterest(key) }) { Text(L.t("আবার স্বাভাবিক করুন")) }
            }
        }
    }

    // ---- অ্যাপ কীভাবে শেখে ----
    TextButton(onClick = { showHow = !showHow }, modifier = Modifier.padding(top = 8.dp)) {
        Text(if (showHow) L.t("লুকান") else L.t("অ্যাপ কীভাবে শেখে?"))
    }
    if (showHow) {
        listOf(
            L.t("• ভিডিও ৫০%/৯০% দেখলে স্কোর বাড়ে, দ্রুত স্কিপ করলে কমে"),
            L.t("• ফেভারিট, সাবস্ক্রাইব ও সার্চ থেকেও শেখে"),
            L.t("• \"আগ্রহ নেই\" দিলে ওই বিষয়/চ্যানেলের স্কোর কমে"),
            L.t("• স্কোর প্রতি ১৪ দিনে অর্ধেক হয়, তাই পুরোনো আগ্রহ ধীরে মিলিয়ে যায়"),
            L.t("• একই বিষয় বারবার দেখলে সাময়িক কম দেখানো হয়, যাতে ফিড একঘেয়ে না হয়"),
            L.t("• ভিডিও কার্ডের ⋮ → \"কেন সাজেস্ট হলো?\" থেকে প্রতিটি সাজেশনের কারণ দেখা যায়")
        ).forEach { Text(it, fontSize = 12.sp, color = Sub, modifier = Modifier.padding(vertical = 1.dp)) }
    }
}

private fun keyLabel(key: String): String =
    if (key.startsWith("ch:")) L.f("চ্যানেল {0}", key.removePrefix("ch:")) else key.removePrefix("kw:")

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).background(Chip).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Sub, fontSize = 11.sp)
    }
}

@Composable
private fun DashTitle(title: String, sub: String?) {
    Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.padding(top = 18.dp))
    if (sub != null) Text(sub, color = Sub, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
}

/** ২×৪ গ্রিড: কর্মদিবস/ছুটির দিন × সকাল/দুপুর/সন্ধ্যা/রাত; রং যত গাঢ়, আগ্রহ তত বেশি */
@Composable
private fun BucketGrid(p: ProfileSummary, selected: String, onPick: (String) -> Unit) {
    val maxV = (p.bucketTotals.values.maxOrNull() ?: 0.0).coerceAtLeast(0.0001)
    val parts = listOf(L.t("সকাল"), L.t("দুপুর"), L.t("সন্ধ্যা"), L.t("রাত"))
    val now = Recommender.bucket()
    listOf("wd" to L.t("কর্মদিবস"), "we" to L.t("ছুটির দিন")).forEach { (day, dayLabel) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(dayLabel, fontSize = 11.sp, color = Sub, modifier = Modifier.width(62.dp))
            listOf("m", "a", "e", "n").forEachIndexed { i, part ->
                val b = "$day-$part"
                val frac = ((p.bucketTotals[b] ?: 0.0) / maxV).toFloat().coerceIn(0f, 1f)
                Box(
                    Modifier.weight(1f).padding(horizontal = 2.dp).height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Chip)
                        .background(YtRed.copy(alpha = 0.12f + 0.6f * frac))
                        .then(if (b == selected) Modifier.border(BorderStroke(2.dp, YtRed), RoundedCornerShape(8.dp)) else Modifier)
                        .clickable { onPick(b) },
                    contentAlignment = Alignment.Center
                ) { Text(parts[i] + if (b == now) " •" else "", fontSize = 11.sp, fontWeight = if (b == selected) FontWeight.Bold else FontWeight.Normal) }
            }
        }
    }
    Text(L.t("• = এখনকার সময়"), color = Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
}

/** একটি আগ্রহের সারি: নাম, তুলনামূলক বার, আর ⋮ মেনু (কম দেখান / ভুলে যান / ব্লক) */
@Composable
private fun InterestControlRow(key: String, name: String, frac: Double, vm: MainViewModel) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 13.sp, maxLines = 1)
            LinearProgressIndicator(
                progress = { frac.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 3.dp).height(4.dp),
                color = YtRed, trackColor = Chip
            )
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, null, modifier = Modifier.size(20.dp)) }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text(L.t("কম দেখান")) }, onClick = { menu = false; vm.dampInterest(key) })
                DropdownMenuItem(text = { Text(L.t("ভুলে যান")) }, onClick = { menu = false; vm.forgetInterest(key) })
                DropdownMenuItem(
                    text = { Text(L.t("ব্লক করুন")) },
                    onClick = {
                        menu = false
                        if (key.startsWith("ch:")) vm.blockChannel(name) else vm.blockKeyword(name)
                        vm.forgetInterest(key)
                    }
                )
            }
        }
    }
}
