package com.applimit.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.applimit.data.db.AppCategory
import com.applimit.data.db.ManagedApp
import com.applimit.domain.LimitDecision
import com.applimit.ui.components.IosCard
import com.applimit.ui.components.IosRow
import com.applimit.ui.components.IosSectionHeader
import com.applimit.ui.theme.AppLimitColors

/**
 * Child-facing home: a big iOS-style ring showing how much of the daily limit
 * is left, plus the categorised app lists.
 */
@Composable
fun HomeScreen(
    decision: LimitDecision?,
    managedApps: List<ManagedApp>,
    onRefresh: () -> Unit,
) {
    LaunchedEffect(Unit) { onRefresh() }

    val used = decision?.limitedUsedMinutes ?: 0
    val limit = decision?.dailyLimitMinutes ?: 60
    val remaining = (limit - used).coerceAtLeast(0)
    val progress = if (limit > 0) (used.toFloat() / limit).coerceIn(0f, 1f) else 0f

    val plus = managedApps.filter { it.category == AppCategory.PLUS }
    val limited = managedApps.filter { it.category == AppCategory.LIMITED }
    val blocked = managedApps.filter { it.category == AppCategory.BLOCKED }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppLimitColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            "Hallo! 👋",
            style = MaterialTheme.typography.headlineMedium,
            color = AppLimitColors.Label,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TimeRing(progress = progress, remainingMinutes = remaining)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "von $limit Min. Tageslimit übrig",
            color = AppLimitColors.SecondaryLabel,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        if (limited.isNotEmpty()) {
            IosSectionHeader("Apps mit Limit")
            IosCard { limited.forEachIndexed { i, a -> AppLine(a.appName, i > 0) } }
        }
        if (plus.isNotEmpty()) {
            IosSectionHeader("Freigegebene Plus-Apps")
            IosCard { plus.forEachIndexed { i, a -> AppLine(a.appName, i > 0) } }
        }
        if (blocked.isNotEmpty()) {
            IosSectionHeader("Gesperrte Apps")
            IosCard { blocked.forEachIndexed { i, a -> AppLine(a.appName, i > 0) } }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun AppLine(name: String, divider: Boolean) {
    if (divider) {
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(AppLimitColors.Separator))
    }
    IosRow(title = name)
}

@Composable
private fun TimeRing(progress: Float, remainingMinutes: Int) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
        Canvas(Modifier.size(200.dp)) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = AppLimitColors.Separator,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            val color = when {
                progress >= 1f -> AppLimitColors.Danger
                progress >= 0.75f -> AppLimitColors.Warning
                else -> AppLimitColors.Success
            }
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * (1f - progress),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$remainingMinutes",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = AppLimitColors.Label,
            )
            Text("Minuten übrig", color = AppLimitColors.SecondaryLabel)
        }
    }
}
