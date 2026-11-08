package com.tubex.app.viewmodel

import com.tubex.app.data.model.CommentItem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.schabi.newpipe.extractor.Page

data class QItem(
    val url: String, val title: String, val channel: String, val thumb: String?,
    val durationSec: Long = 0, val views: Long = -1, val uploaded: String? = null, val live: Boolean = false
)

data class ReplyState(
    val items: List<CommentItem> = emptyList(), val next: Page? = null,
    val loading: Boolean = false, val expanded: Boolean = false
)
