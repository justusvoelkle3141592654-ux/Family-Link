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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.applimit.data.db.AppCategory
import com.applimit.data.db.ManagedApp
import com.applimit.data.repository.InstalledApp
import com.applimit.ui.CategorySelection
import com.applimit.ui.components.IosPrimaryButton
import com.applimit.ui.components.IosSwitch
import com.applimit.ui.theme.AppLimitColors

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

/** Local editable state per app (defaults to STANDARD). */
private data class Sel(
    val category: AppCategory = AppCategory.STANDARD,
    val individualLimitMinutes: Int = 30,
    val plusCountsToGlobal: Boolean = false,
)

/**
 * Assign each installed app to one of the 4 categories. Every app starts as
 * STANDARD. Changes are held locally and only written when the parent taps
 * "Speichern", which then re-checks enforcement immediately.
 */
@Composable
fun CategoriesScreen(
    installed: List<InstalledApp>,
    managed: List<ManagedApp>,
    protectionEnabled: Boolean,
    onSave: (List<CategorySelection>) -> Unit,
    onBack: () -> Unit,
) {
    val pending = remember { mutableStateMapOf<String, Sel>() }
    var dirty by remember { mutableStateOf(false) }
    var justSaved by remember { mutableStateOf(false) }

    // Seed local state: use the saved category if present, otherwise STANDARD.
    LaunchedEffect(installed, managed) {
        val byPkg = managed.associateBy { it.packageName }
        installed.forEach { app ->
            if (!pending.containsKey(app.packageName)) {
                val m = byPkg[app.packageName]
                pending[app.packageName] = if (m != null) {
                    Sel(m.category, m.individualLimitMinutes, m.plusCountsToGlobal)
                } else Sel()
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(AppLimitColors.Background),
    ) {
        ParentTopBar(title = "App-Kategorien", onBack = onBack)

        if (!protectionEnabled) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF3CD))
                    .padding(12.dp),
            ) {
                Text(
                    "⚠️ Der Schutz ist noch AUS. Sperren/Limits wirken erst, wenn du " +
                        "im Elternbereich den Schutz-Schalter aktivierst.",
                    color = Color(0xFF7A5B00),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        if (installed.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Apps werden geladen …", color = AppLimitColors.SecondaryLabel)
            }
            return
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(installed, key = { it.packageName }) { app ->
                val sel = pending[app.packageName] ?: Sel()
                AppCategoryCard(
                    app = app,
                    sel = sel,
                    onChange = { newSel ->
                        pending[app.packageName] = newSel
                        dirty = true
                        justSaved = false
                    },
                )
            }
        }

        // Sticky Save bar.
        Column(
            Modifier
                .fillMaxWidth()
                .background(AppLimitColors.Card)
                .padding(16.dp),
        ) {
            if (justSaved) {
                Text(
                    "✓ Gespeichert",
                    color = AppLimitColors.Success,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            IosPrimaryButton(
                text = if (dirty) "Speichern" else "Gespeichert",
                enabled = dirty,
            ) {
                val selections = installed.map { app ->
                    val s = pending[app.packageName] ?: Sel()
                    CategorySelection(app, s.category, s.individualLimitMinutes, s.plusCountsToGlobal)
                }
                onSave(selections)
                dirty = false
                justSaved = true
            }
        }
    }
}

@Composable
private fun AppCategoryCard(
    app: InstalledApp,
    sel: Sel,
    onChange: (Sel) -> Unit,
) {
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
            Badge(sel.category)
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Segment("Plus", sel.category == AppCategory.PLUS, catColor(AppCategory.PLUS)) {
                onChange(sel.copy(category = AppCategory.PLUS))
            }
            Segment("Standard", sel.category == AppCategory.STANDARD, catColor(AppCategory.STANDARD)) {
                onChange(sel.copy(category = AppCategory.STANDARD))
            }
            Segment("Limit", sel.category == AppCategory.LIMIT, catColor(AppCategory.LIMIT)) {
                onChange(sel.copy(category = AppCategory.LIMIT))
            }
            Segment("Sperren", sel.category == AppCategory.BLOCKED, catColor(AppCategory.BLOCKED)) {
                onChange(sel.copy(category = AppCategory.BLOCKED))
            }
        }

        when (sel.category) {
            AppCategory.LIMIT -> {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Eigenes Limit", color = AppLimitColors.Label, modifier = Modifier.weight(1f))
                    Text(
                        "${sel.individualLimitMinutes} Min.",
                        color = AppLimitColors.SecondaryLabel,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    RoundBtn("–") { onChange(sel.copy(individualLimitMinutes = (sel.individualLimitMinutes - 5).coerceAtLeast(5))) }
                    Spacer(Modifier.size(10.dp))
                    RoundBtn("+") { onChange(sel.copy(individualLimitMinutes = (sel.individualLimitMinutes + 5).coerceAtMost(600))) }
                }
            }
            AppCategory.PLUS -> {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Zählt zum globalen Limit", color = AppLimitColors.Label)
                        Text(
                            if (sel.plusCountsToGlobal) "Nutzung verbraucht globale Zeit"
                            else "Nutzung ist komplett frei",
                            color = AppLimitColors.SecondaryLabel,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    IosSwitch(sel.plusCountsToGlobal) { onChange(sel.copy(plusCountsToGlobal = it)) }
                }
            }
            else -> {}
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
