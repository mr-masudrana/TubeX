package com.tubex.app.ui.components

import com.tubex.app.data.cache.AvatarCache
import com.tubex.app.data.model.ChannelRef
import com.tubex.app.util.L
import com.tubex.app.ui.theme.Bg
import com.tubex.app.ui.theme.Sub
import com.tubex.app.data.local.entity.HistoryEntity
import com.tubex.app.ui.theme.Chip
import com.tubex.app.ui.theme.YtRed
import com.tubex.app.util.fixUrl

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage

/** লোগো না থাকলে (কার্ড/কমেন্ট) চ্যানেল থেকে লোগো এনে দেখায়; না পেলে নামের প্রথম অক্ষর */
@Composable
fun ChannelAvatar(avatar: String?, channelUrl: String?, name: String, size: Dp, live: Boolean = false) {
    val missing = avatar.isNullOrBlank()
    if (missing && !channelUrl.isNullOrBlank()) LaunchedEffect(channelUrl) { AvatarCache.ensure(channelUrl) }
    val resolved = if (!missing) avatar else channelUrl?.let { AvatarCache.map[it] }?.takeIf { it.isNotBlank() }
    Avatar(resolved, name, size, live)
}

@Composable
fun Avatar(url: String?, name: String, size: Dp, live: Boolean = false) {
    // লাইভ হলে YouTube-এর মতো লাল রিং (বাইরে ২dp লাল, ভেতরে ২dp ফাঁক); সাইজ বদলায় না — লোগো ভেতরে ছোট হয়
    val ring = if (live) Modifier.border(2.dp, YtRed, CircleShape).padding(4.dp) else Modifier
    Box(Modifier.size(size).then(ring).clip(CircleShape).background(Chip), Alignment.Center) {
        if (!url.isNullOrBlank()) AsyncImage(fixUrl(url), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(name.take(1).uppercase(), fontWeight = FontWeight.Bold)
    }
}

/** কোলাব ভিডিও: একাধিক চ্যানেলের ছোট ছোট লোগো গায়ে গায়ে (সর্বোচ্চ ৩টি; বেশি হলে "+N")। লাইভ হলে প্রতিটিতে লাল রিং */
@Composable
fun ChannelAvatarStack(channels: List<ChannelRef>, live: Boolean = false, d: Dp = 26.dp, step: Dp = 16.dp) {
    val shown = channels.take(3)
    val extra = channels.size - shown.size
    val slots = shown.size + if (extra > 0) 1 else 0
    Box(Modifier.width(d + step * (slots - 1)).height(36.dp)) {
        shown.forEachIndexed { i, c ->
            Box(
                Modifier.align(Alignment.CenterStart).offset(x = step * i).size(d)
                    .then(if (live) Modifier.border(2.dp, YtRed, CircleShape) else Modifier)
                    .clip(CircleShape).background(Bg).padding(if (live) 4.dp else 2.dp)
            ) { Avatar(c.avatar, c.name, d) }
        }
        if (extra > 0) Box(
            Modifier.align(Alignment.CenterStart).offset(x = step * shown.size).size(d)
                .clip(CircleShape).background(Bg).padding(2.dp).clip(CircleShape).background(Chip),
            Alignment.Center
        ) { Text("+$extra", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
    }
}

/** কোলাব লোগোয় ট্যাপ করলে: ভিডিওর সব চ্যানেলের তালিকা; একটিতে ট্যাপ করলে সেই চ্যানেল খোলে */
@Composable
fun ChannelListDialog(channels: List<ChannelRef>, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L.t("এই ভিডিওর চ্যানেল")) },
        text = {
            LazyColumn {
                items(channels) { c ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onPick(c.url) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(c.avatar, c.name, 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(c.name, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(L.t("বন্ধ করুন")) } }
    )
}

@Composable
fun HistoryThumb(h: HistoryEntity, modifier: Modifier) {
    Box(modifier.background(Chip)) {
        AsyncImage(h.thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (h.durationSec > 0) LinearProgressIndicator(
            progress = { (h.positionMs / (h.durationSec * 1000f)).coerceIn(0f, 1f) },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
            color = YtRed, trackColor = Color(0x66000000)
        )
    }
}
