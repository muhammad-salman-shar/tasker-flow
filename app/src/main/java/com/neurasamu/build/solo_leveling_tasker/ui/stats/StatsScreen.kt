package com.neurasamu.build.solo_leveling_tasker.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.solo_leveling_tasker.domain.GamificationEngine
import com.neurasamu.build.solo_leveling_tasker.ui.MainViewModel

@Composable
fun StatsScreen(vm: MainViewModel) {
    val state by vm.homeState.collectAsState()
    val s = state.stats

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text("STATISTICS", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        Spacer(Modifier.height(16.dp))

        // Hero card: Health + EP ring charts
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF17171E)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RingStat(
                    value = s.health.toFloat(),
                    max = 100f,
                    center = "${s.health}%",
                    label = "HEALTH",
                    color = Color(0xFFEF5350)
                )
                RingStat(
                    value = GamificationEngine.levelProgress(s.ep).first.toFloat(),
                    max = 90f,
                    center = "${s.ep}",
                    label = "EP",
                    color = Color(0xFFFFC107)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Key stats row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallStat(Modifier.weight(1f), "🏆", "Level", "${s.level}", Color(0xFFFFC107))
            SmallStat(Modifier.weight(1f), "🔥", "Streak", "${s.streak}d", Color(0xFFFF9800))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallStat(Modifier.weight(1f), "✅", "Done", "${s.totalCompleted}", Color(0xFF66BB6A))
            SmallStat(Modifier.weight(1f), "❌", "Missed", "${s.totalMissed}", Color(0xFFEF5350))
        }

        Spacer(Modifier.height(20.dp))

        // Bar chart
        Text("LAST 7 DAYS", color = Color(0xFF9E9E9E), fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF17171E)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Demo data — real data needs historical occurrences query
            val weekly = listOf(0.3f, 0.6f, 0.45f, 0.8f, 0.55f, 0.9f, 0.7f)
            BarChart(weekly, Modifier.padding(16.dp).fillMaxWidth().height(160.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Activity placeholders
        Text("ACTIVITY", color = Color(0xFF9E9E9E), fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF17171E)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                StatLine("Active Debts", "${state.activeDebts.size}", Color(0xFFEF5350))
                HorizontalDivider(color = Color(0xFF23232B), modifier = Modifier.padding(vertical = 8.dp))
                StatLine("Active Recoveries", "${state.activeRecoveries.size}", Color(0xFFFFAB91))
                HorizontalDivider(color = Color(0xFF23232B), modifier = Modifier.padding(vertical = 8.dp))
                StatLine("Total Recoveries", "${s.totalRecoveries}", Color(0xFF66BB6A))
            }
        }

        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun RingStat(value: Float, max: Float, center: String, label: String, color: Color) {
    val animated by animateFloatAsState(
        targetValue = (value / max).coerceIn(0f, 1f),
        animationSpec = tween(800),
        label = "ring"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(110.dp)) {
                val stroke = 12.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                // track
                drawArc(
                    color = Color(0xFF23232B),
                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                // progress
                drawArc(
                    brush = Brush.sweepGradient(listOf(color, color.copy(alpha = 0.6f), color)),
                    startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Text(center, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color(0xFF9E9E9E), fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmallStat(modifier: Modifier, icon: String, label: String, value: String, tint: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF17171E)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Text(label, color = Color(0xFF9E9E9E), fontSize = 11.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(value, color = tint, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun BarChart(values: List<Float>, modifier: Modifier = Modifier) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val count = values.size
            if (count == 0) return@Canvas
            val gap = size.width * 0.03f
            val barWidth = (size.width - gap * (count + 1)) / count
            values.forEachIndexed { i, v ->
                val h = size.height * v
                val x = gap + i * (barWidth + gap)
                val y = size.height - h
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFFD54F), Color(0xFFFF6F00))
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach {
                Text(it, color = Color(0xFF9E9E9E), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF9E9E9E), fontSize = 13.sp)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
