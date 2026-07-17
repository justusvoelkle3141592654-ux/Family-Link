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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.applimit.ui.theme.AppLimitColors

/**
 * Assign each installed app to a category (Prompt Punkt 2). Tapping a segment
 * writes immediately; tapping the active segment again clears it (→ unmanaged,
 * treated as a free PLUS app).
 */
@Composable
fun CategoriesScreen(
    installed: List<InstalledApp>,
    managed: List<ManagedApp>,
    onSet: (InstalledApp, AppCategory?) -> Unit,
    onBack: () -> Unit,
) {
    val categoryByPkg = managed.associate { it.packageName to it.category }

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
                AppCategoryCard(
                    app = app,
                    selected = categoryByPkg[app.packageName],
                    onSelect = { cat ->
                        if (categoryByPkg[app.packageName] == cat) onSet(app, null)
                        else onSet(app, cat)
                    },
                )
            }
        }
    }
}

@Composable
private fun AppCategoryCard(
    app: InstalledApp,
    selected: AppCategory?,
    onSelect: (AppCategory) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppLimitColors.Card)
            .padding(14.dp),
    ) {
        Text(app.appName, color = AppLimitColors.Label, fontWeight = FontWeight.Medium)
        Text(app.packageName, color = AppLimitColors.SecondaryLabel, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Segment("Plus", selected == AppCategory.PLUS, AppLimitColors.Success) { onSelect(AppCategory.PLUS) }
            Segment("Limit", selected == AppCategory.LIMITED, AppLimitColors.Accent) { onSelect(AppCategory.LIMITED) }
            Segment("Gesperrt", selected == AppCategory.BLOCKED, AppLimitColors.Danger) { onSelect(AppCategory.BLOCKED) }
        }
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
            .height(36.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (active) activeColor else AppLimitColors.Background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (active) Color.White else AppLimitColors.SecondaryLabel,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium,
        )
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
        Spacer(Modifier.height(1.dp).padding(end = 40.dp))
    }
}
