package com.tubex.app.ui.home

import com.tubex.app.data.model.PlaylistEntry
import com.tubex.app.ui.theme.Chip
import com.tubex.app.ui.theme.Sub
import com.tubex.app.util.L
import com.tubex.app.util.fmtCount

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * হোম ফিডের ভিডিওর ফাঁকে একক প্লেলিস্ট কার্ড (অফিসিয়াল YouTube-এর Mix কার্ডের মতো: পেছনে দুই স্তর, রঙিন প্যানেল)।
 * ট্যাপ = পুরো প্লেলিস্ট কিউতে তুলে চালানো; ⋮ থেকে প্লেলিস্ট পেজ খোলা বা শাফল।
 */
@Composable
fun HomePlaylistCard(p: PlaylistEntry, onPlay: () -> Unit, onOpen: () -> Unit, onShuffle: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 20.dp)) {
        // পেছনের দুটি স্তর (স্ট্যাক বোঝাতে)
        Box(
            Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.84f).height(8.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(Sub.copy(alpha = 0.30f))
        )
        Box(
            Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.92f).height(8.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(Sub.copy(alpha = 0.50f))
        )
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Chip).clickable(onClick = onPlay)) {
            Box {
                AsyncImage(
                    model = p.thumb, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Sub.copy(alpha = 0.2f))
                )
                Row(
                    Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC000000)).padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    if (p.count > 0) Text(
                        " " + L.f("{0} টি ভিডিও", fmtCount(p.count)), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium
                    )
                }
            }
            Row(Modifier.padding(start = 12.dp, top = 10.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                    if (!p.uploader.isNullOrBlank()) Text(
                        p.uploader, color = Sub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.MoreVert, null, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text(L.t("এলোমেলো চালান")) }, onClick = { menu = false; onShuffle() })
                        DropdownMenuItem(text = { Text(L.t("প্লেলিস্ট পেজ")) }, onClick = { menu = false; onOpen() })
                    }
                }
            }
        }
    }
}
