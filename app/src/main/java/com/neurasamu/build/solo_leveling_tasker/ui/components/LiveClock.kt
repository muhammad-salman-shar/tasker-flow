package com.neurasamu.build.solo_leveling_tasker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LiveClock(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEE, dd MMM", Locale.getDefault()) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        Text(
            timeFmt.format(Date(now)),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            dateFmt.format(Date(now)),
            color = Color(0xFF79829C),
            fontSize = 11.sp
        )
    }
}
