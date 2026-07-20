package com.applimit.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.applimit.data.db.AppCategory
import com.applimit.data.db.ManagedApp
import com.applimit.data.repository.InstalledApp
import com.applimit.ui.components.IosSwitch
import com.applimit.ui.theme.AppLimitColors

/** Colour + short label for each category badge. */
private fun catColor(c: AppCategory) = when (c) {
    AppCategory.PLUS -> AppLimitColors.Success
    AppCategory.STANDARD -> AppLimitColors.Accent
    AppCategory.LIMIT -> AppLimitColors.Warning
    AppCategory.BLOCKED -> AppLimitColors.Danger
}

private fun catLabel(c: AppCategory) = when (c) {
    AppCategory.PLUS -> "Plus"
    AppCategory.STANDARD -> "Standard"
    AppCategory.LIMIT -> "Limit"
    AppCategory.BLOCKED -> "Blockiert"
}

/**
 * Assign each installed app to one of the 4 categories, with the extra controls
 * each category needs (individual limit for LIMIT, global-count toggle for PLUS)
 * and a coloured badge showing the current state.
 */
@Composable
fun CategoriesScreen(
    installed: List<InstalledApp>,
    managed: List<ManagedApp>,
    onSet: (InstalledApp, AppCategory?, Int, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val byPkg = managed.associateBy { it.packageName }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppLimitColors.Background),
    ) {
        ParentTopBar(title = "App-Kategorien", onBack = onBack)
        if (installed.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Apps werden geladen …", color = AppLimitColors.SecondaryLabel)
            }
            return
        }
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(installed, key = { it.packageName }) { app ->
                AppCategoryCard(app = app, current = byPkg[app.packageName], onSet = onSet)
            }
        }
    }
}

@Composable
private fun AppCategoryCard(
    app: InstalledApp,
    current: ManagedApp?,
    onSet: (InstalledApp, AppCategory?, Int, Boolean) -> Unit,
) {
    val selected = current?.category
    val indivLimit = current?.individualLimitMinutes ?: 30
    val plusGlobal = current?.plusCountsToGlobal ?: false

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppLimitColors.Card)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(app.appName, color = AppLimitColors.Label, fontWeight = FontWeight.Medium)
                Text(
                    app.packageName,
                    color = AppLimitColors.SecondaryLabel,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (selected != null) Badge(selected)
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Segment("Plus", selected == AppCategory.PLUS, catColor(AppCategory.PLUS)) {
                onSet(app, AppCategory.PLUS, indivLimit, plusGlobal)
            }
            Segment("Standard", selected == AppCategory.STANDARD, catColor(AppCategory.STANDARD)) {
                onSet(app, AppCategory.STANDARD, indivLimit, plusGlobal)
            }
            Segment("Limit", selected == AppCategory.LIMIT, catColor(AppCategory.LIMIT)) {
                onSet(app, AppCategory.LIMIT, indivLimit, plusGlobal)
            }
            Segment("Sperren", selected == AppCategory.BLOCKED, catColor(AppCategory.BLOCKED)) {
                onSet(app, AppCategory.BLOCKED, indivLimit, plusGlobal)
            }
        }

        // Extra controls depending on the chosen category.
        when (selected) {
            AppCategory.LIMIT -> {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Eigenes Limit", color = AppLimitColors.Label, modifier = Modifier.weight(1f))
                    Text(
                        "$indivLimit Min.",
                        color = AppLimitColors.SecondaryLabel,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    RoundBtn("–") { onSet(app, AppCategory.LIMIT, (indivLimit - 5).coerceAtLeast(5), plusGlobal) }
                    Spacer(Modifier.size(10.dp))
                    RoundBtn("+") { onSet(app, AppCategory.LIMIT, (indivLimit + 5).coerceAtMost(600), plusGlobal) }
                }
            }
            AppCategory.PLUS -> {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Zählt zum globalen Limit", color = AppLimitColors.Label)
                        Text(
                            if (plusGlobal) "Nutzung verbraucht globale Zeit"
                            else "Nutzung ist komplett frei",
                            color = AppLimitColors.SecondaryLabel,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    IosSwitch(plusGlobal) { onSet(app, AppCategory.PLUS, indivLimit, it) }
                }
            }
            else -> {}
        }

        if (selected != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Entfernen",
                color = AppLimitColors.Accent,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clickable { onSet(app, null, indivLimit, plusGlobal) },
            )
        }
    }
}

@Composable
private fun Badge(c: AppCategory) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(catColor(c))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(catLabel(c), color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Segment(
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (active) activeColor else AppLimitColors.Background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (active) Color.White else AppLimitColors.SecondaryLabel,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun RoundBtn(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(AppLimitColors.Accent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ParentTopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(AppLimitColors.Background)
            .padding(top = 44.dp, start = 12.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "‹ Zurück",
            color = AppLimitColors.Accent,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.clickable { onBack() },
        )
        Spacer(Modifier.weight(1f))
        Text(title, color = AppLimitColors.Label, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(56.dp))
    }
}
