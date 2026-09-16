package com.taskerflow.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskerflow.app.data.model.DeadlineScale
import com.taskerflow.app.data.model.OccurrenceEntity
import com.taskerflow.app.data.model.OccurrenceStatus
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

// ---- Data tree ----

private data class DayNode(
    val index: Int,
    val occ: OccurrenceEntity,
    val dateLabel: String
)

private data class WeekNode(
    val label: String,
    val days: List<DayNode>
) {
    val doneCount get() = days.count { it.occ.status.isDone() }
    val total get() = days.size
    val complete get() = doneCount == total && total > 0
}

private data class MonthNode(
    val label: String,
    val weeks: List<WeekNode>
) {
    val doneCount get() = weeks.sumOf { it.doneCount }
    val total get() = weeks.sumOf { it.total }
    val complete get() = doneCount == total && total > 0
}

private data class YearNode(
    val label: String,
    val months: List<MonthNode>
) {
    val doneCount get() = months.sumOf { it.doneCount }
    val total get() = months.sumOf { it.total }
    val complete get() = doneCount == total && total > 0
}

private fun OccurrenceStatus.isDone(): Boolean =
    this == OccurrenceStatus.COMPLETED ||
    this == OccurrenceStatus.LATE ||
    this == OccurrenceStatus.RECOVERED

// ---- Colors per scale ----

private data class ScaleColors(
    val accent: Color,
    val bg: Color,
    val border: Color,
    val tag: String
)

private fun colorsFor(scale: DeadlineScale): ScaleColors = when (scale) {
    DeadlineScale.SHORT -> ScaleColors(
        accent = Color(0xFFFFB300),
        bg = Color(0xFF1E1A0F),
        border = Color(0xFF5C4600),
        tag = "WEEKLY"
    )
    DeadlineScale.MEDIUM -> ScaleColors(
        accent = Color(0xFF00E5FF),
        bg = Color(0xFF0E1F26),
        border = Color(0xFF00515C),
        tag = "MONTHLY"
    )
    DeadlineScale.LONG -> ScaleColors(
        accent = Color(0xFFFF2A6D),
        bg = Color(0xFF2A0F1A),
        border = Color(0xFF5C1A33),
        tag = "YEARLY"
    )
}

// ---- Main composable ----

@Composable
fun DeadlineTaskCard(
    title: String,
    category: String,
    occurrences: List<OccurrenceEntity>,
    epReward: Int,
    onCompleteDay: (Long) -> Unit,
    onDayTap: (Long) -> Unit = {}
) {
    val sorted = remember(occurrences) { occurrences.sortedBy { it.scheduledAt } }
    var expanded by remember { mutableStateOf(false) }

    val scale = remember(sorted) { computeScale(sorted.size) }
    val colors = colorsFor(scale)

    val doneCount = sorted.count { it.occ().status.isDone() }
    val total = sorted.size
    val allDone = doneCount == total && total > 0
    val progress = if (total == 0) 0f else doneCount.toFloat() / total
    val animProgress by animateFloatAsState(progress, tween(500), label = "p")

    // Build tree based on scale
    val root = remember(sorted, scale) { buildTree(sorted, scale) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (allDone) Color(0xFF0F1F14) else colors.bg
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .border(
                1.dp,
                if (allDone) Color(0xFF1E4A25) else colors.border,
                RoundedCornerShape(16.dp)
            )
    ) {
        Column {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scale badge
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.accent.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        colors.tag,
                        color = colors.accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        color = if (allDone) Color(0xFF9E9E9E) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "$category • ${doneCount} / $total days",
                        color = Color(0xFF9E9E9E),
                        fontSize = 11.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "+${epReward}/day",
                        color = if (allDone) Color(0xFF66BB6A) else colors.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        null, tint = Color(0xFF9E9E9E), modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Progress bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF23232B))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animProgress)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (allDone) Color(0xFF66BB6A) else colors.accent)
                )
            }
            Spacer(Modifier.height(4.dp))

            // Expanded content
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 12.dp)) {
                    when (root) {
                        is RootTree.Yearly -> {
                            root.years.forEach { year ->
                                GroupBlock(
                                    label = year.label,
                                    done = year.doneCount,
                                    total = year.total,
                                    complete = year.complete,
                                    accent = colors.accent,
                                    level = 0
                                ) {
                                    year.months.forEach { month ->
                                        GroupBlock(
                                            label = month.label,
                                            done = month.doneCount,
                                            total = month.total,
                                            complete = month.complete,
                                            accent = colors.accent,
                                            level = 1
                                        ) {
                                            month.weeks.forEach { week ->
                                                WeekBlock(week, colors, onCompleteDay, onDayTap)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is RootTree.Monthly -> {
                            root.months.forEach { month ->
                                GroupBlock(
                                    label = month.label,
                                    done = month.doneCount,
                                    total = month.total,
                                    complete = month.complete,
                                    accent = colors.accent,
                                    level = 0
                                ) {
                                    month.weeks.forEach { week ->
                                        WeekBlock(week, colors, onCompleteDay, onDayTap)
                                    }
                                }
                            }
                        }
                        is RootTree.Weekly -> {
                            root.weeks.forEach { week ->
                                WeekBlock(week, colors, onCompleteDay, onDayTap)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekBlock(
    week: WeekNode,
    colors: ScaleColors,
    onCompleteDay: (Long) -> Unit,
    onDayTap: (Long) -> Unit
) {
    Column(Modifier.padding(top = 4.dp)) {
        Text(
            "${week.label}  •  ${week.doneCount}/${week.total}",
            color = Color(0xFF79829C),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
        )
        week.days.forEach { day ->
            DayRow(
                index = day.index,
                dateLabel = day.dateLabel,
                occ = day.occ,
                accent = colors.accent,
                onComplete = { onCompleteDay(day.occ.id) },
                onTap = { onDayTap(day.occ.id) }
            )
        }
    }
}

@Composable
private fun GroupBlock(
    label: String,
    done: Int,
    total: Int,
    complete: Boolean,
    accent: Color,
    level: Int,
    content: @Composable () -> Unit
) {
    var open by remember { mutableStateOf(!complete) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = if (level == 0) 8.dp else 4.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF14141A))
                .clickable { open = !open }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = if (complete) Color(0xFF66BB6A) else Color.White,
                fontSize = if (level == 0) 12.sp else 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                "$done/$total",
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                null, tint = Color(0xFF9E9E9E), modifier = Modifier.size(16.dp)
            )
        }
        AnimatedVisibility(visible = open) {
            Box(Modifier.padding(start = if (level == 0) 8.dp else 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun DayRow(
    index: Int,
    dateLabel: String,
    occ: OccurrenceEntity,
    accent: Color,
    onComplete: () -> Unit,
    onTap: () -> Unit
) {
    val done = occ.status.isDone()
    val locked = occ.status == OccurrenceStatus.SKIPPED

    val bg = when {
        done -> Color(0xFF132A18)
        locked -> Color(0xFF121216)
        else -> Color(0xFF14141A)
    }
    val rowAccent = when {
        done -> Color(0xFF66BB6A)
        locked -> Color(0xFF444444)
        else -> accent
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 4.dp, top = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(enabled = !locked) { onTap() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .clickable(enabled = !locked && !done) { onComplete() },
            contentAlignment = Alignment.Center
        ) {
            when {
                locked -> Icon(Icons.Filled.Lock, null, tint = Color(0xFF666666), modifier = Modifier.size(13.dp))
                done -> Box(
                    Modifier.size(20.dp).clip(CircleShape).background(rowAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(12.dp))
                }
                else -> Box(
                    Modifier.size(20.dp).clip(CircleShape).border(2.dp, rowAccent, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Day $index",
                color = if (locked) Color(0xFF666666) else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                dateLabel,
                color = if (locked) Color(0xFF444444) else Color(0xFF9E9E9E),
                fontSize = 10.sp
            )
        }
    }
}

// ---- Tree building ----

private sealed class RootTree {
    data class Weekly(val weeks: List<WeekNode>) : RootTree()
    data class Monthly(val months: List<MonthNode>) : RootTree()
    data class Yearly(val years: List<YearNode>) : RootTree()
}

private fun computeScale(days: Int): DeadlineScale = when {
    days <= 31 -> DeadlineScale.SHORT
    days <= 365 -> DeadlineScale.MEDIUM
    else -> DeadlineScale.LONG
}

private fun buildTree(sorted: List<OccurrenceEntity>, scale: DeadlineScale): RootTree {
    val zone = ZoneId.systemDefault()
    // Build day nodes with stable index
    val dayNodes = sorted.mapIndexed { i, occ ->
        val date = Instant.ofEpochMilli(occ.scheduledAt).atZone(zone).toLocalDate()
        val label = "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}"
        DayNode(index = i + 1, occ = occ, dateLabel = label)
    }

    // Helper: group into weeks
    fun groupWeeks(days: List<DayNode>): List<WeekNode> {
        val grouped = linkedMapOf<String, MutableList<DayNode>>()
        days.forEach { d ->
            val date = Instant.ofEpochMilli(d.occ.scheduledAt).atZone(zone).toLocalDate()
            // Use ISO week start (Monday) as key
            val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
            val key = "${monday.dayOfMonth} ${monday.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}"
            grouped.getOrPut(key) { mutableListOf() }.add(d)
        }
        return grouped.map { (k, v) -> WeekNode("Week of $k", v) }
    }

    return when (scale) {
        DeadlineScale.SHORT -> RootTree.Weekly(groupWeeks(dayNodes))
        DeadlineScale.MEDIUM -> {
            val byMonth = linkedMapOf<String, MutableList<DayNode>>()
            dayNodes.forEach { d ->
                val date = Instant.ofEpochMilli(d.occ.scheduledAt).atZone(zone).toLocalDate()
                val key = "${date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${date.year}"
                byMonth.getOrPut(key) { mutableListOf() }.add(d)
            }
            RootTree.Monthly(
                byMonth.map { (k, v) -> MonthNode(k, groupWeeks(v)) }
            )
        }
        DeadlineScale.LONG -> {
            val byYear = linkedMapOf<Int, MutableList<DayNode>>()
            dayNodes.forEach { d ->
                val date = Instant.ofEpochMilli(d.occ.scheduledAt).atZone(zone).toLocalDate()
                byYear.getOrPut(date.year) { mutableListOf() }.add(d)
            }
            RootTree.Yearly(
                byYear.map { (year, days) ->
                    val byMonth = linkedMapOf<String, MutableList<DayNode>>()
                    days.forEach { d ->
                        val date = Instant.ofEpochMilli(d.occ.scheduledAt).atZone(zone).toLocalDate()
                        val key = date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                        byMonth.getOrPut(key) { mutableListOf() }.add(d)
                    }
                    YearNode(
                        label = "Year $year",
                        months = byMonth.map { (k, v) -> MonthNode(k, groupWeeks(v)) }
                    )
                }
            )
        }
    }
}

// Convenience for occurrence to itself
private fun OccurrenceEntity.occ(): OccurrenceEntity = this
