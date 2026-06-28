package com.charles.nutrisnap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * A floating speech bubble that displays Pip's reaction text.
 *
 * Appears with an entrance animation driven by the caller (e.g. [AnimatedVisibility]).
 * Auto-dismisses after 4 seconds unless [persistent] is true.
 */
@Composable
fun PipSpeechBubble(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    persistent: Boolean = false,
) {
    LaunchedEffect(text) {
        if (!persistent) {
            delay(4000)
            onDismiss()
        }
    }

    val bubbleShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .widthIn(max = 220.dp)
            .background(color = Color(0x1F5BC0EB), shape = bubbleShape)
            .border(width = 1.5.dp, color = Color(0xFF5BC0EB), shape = bubbleShape)
            .padding(start = 12.dp, end = 32.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Text(
            text = text,
            color = Color(0xFF3A2A21),
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(20.dp)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Dismiss",
                tint = Color(0xFF3A2A21),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
