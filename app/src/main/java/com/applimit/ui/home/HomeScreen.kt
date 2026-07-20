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
import com.applimit.data.prefs.AppSettings
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
    settings: AppSettings,
    onRefresh: () -> Unit,
) {
    LaunchedEffect(Unit) { onRefresh() }

    // The ring tracks the general limit (normal usage pool).
    val usedMin = ((decision?.generalUsedSec ?: 0) / 60).toInt()
    val limitMin = ((decision?.generalLimitSec ?: (settings.generalLimitMinutes * 60L)) / 60).toInt()
    val remaining = (limitMin - usedMin).coerceAtLeast(0)
    val progress = if (limitMin > 0) (usedMin.toFloat() / limitMin).coerceIn(0f, 1f) else 0f

    val globalUsedMin = ((decision?.globalUsedSec ?: 0) / 60).toInt()
    val globalLimitMin = ((decision?.globalLimitSec ?: (settings.globalLimitMinutes * 60L)) / 60).toInt()

    val plus = managedApps.filter { it.category == AppCategory.PLUS }
    val limited = managedApps.filter { it.category == AppCategory.LIMIT }
    val standard = managedApps.filter { it.category == AppCategory.STANDARD }
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
            "von $limitMin Min. allgemeinem Limit übrig",
            color = AppLimitColors.SecondaryLabel,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        IosSectionHeader("Meine Zeit")
        IosCard {
            IosRow(
                title = "Allgemeines Limit",
                subtitle = "Normale Nutzung",
                trailing = { Text("$usedMin / $limitMin Min.", color = AppLimitColors.SecondaryLabel) },
            )
            Divider()
            IosRow(
                title = "Gesamte Bildschirmzeit",
                subtitle = "Globales Limit",
                trailing = { Text("$globalUsedMin / $globalLimitMin Min.", color = AppLimitColors.SecondaryLabel) },
            )
            Divider()
            IosRow(
                title = "Ruhezeit",
                subtitle = if (settings.quietTimeEnabled)
                    "Nutzung nur ${settings.usageWindowStartHour}:00–${settings.usageWindowEndHour}:00 Uhr"
                else "Keine Ruhezeit",
                trailing = {
                    Text(
                        if (settings.quietTimeEnabled) "aktiv" else "aus",
                        color = AppLimitColors.SecondaryLabel,
                    )
                },
            )
        }

        if (limited.isNotEmpty()) {
            IosSectionHeader("Apps mit eigenem Limit")
            IosCard {
                limited.forEachIndexed { i, a ->
                    if (i > 0) Divider()
                    IosRow(title = a.appName, subtitle = "max. ${a.individualLimitMinutes} Min./Tag")
                }
            }
        }
        if (standard.isNotEmpty()) {
            IosSectionHeader("Standard-Apps")
            IosCard { standard.forEachIndexed { i, a -> AppLine(a.appName, i > 0) } }
        }
        if (plus.isNotEmpty()) {
            IosSectionHeader("Zugelassen Plus")
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
    if (divider) Divider()
    IosRow(title = name)
}

@Composable
private fun Divider() {
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(AppLimitColors.Separator))
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
