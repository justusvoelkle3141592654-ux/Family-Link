package com.applimit.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.applimit.ui.MainViewModel
import com.applimit.ui.components.IosCard
import com.applimit.ui.components.IosPrimaryButton
import com.applimit.ui.components.IosRow
import com.applimit.ui.components.IosSectionHeader
import com.applimit.ui.theme.AppLimitColors

private enum class ParentTab { DASHBOARD, CATEGORIES, LIMITS }

/** Parent management area (Prompt Punkt 6). Reached only via the parent PIN. */
@Composable
fun ParentScreen(
    vm: MainViewModel,
    onOpenOnboarding: () -> Unit,
    onLogout: () -> Unit,
) {
    var tab by remember { mutableStateOf(ParentTab.DASHBOARD) }
    val state by vm.state.collectAsState()

    when (tab) {
        ParentTab.DASHBOARD -> Dashboard(
            state = state,
            onCategories = { vm.loadInstalledApps(); tab = ParentTab.CATEGORIES },
            onLimits = { tab = ParentTab.LIMITS },
            onOnboarding = onOpenOnboarding,
            onEmergencyReset = { vm.emergencyReset() },
            onLogout = onLogout,
        )
        ParentTab.CATEGORIES -> CategoriesScreen(
            installed = state.installedApps,
            managed = state.managedApps,
            onSet = { app, cat -> vm.setCategory(app, cat) },
            onBack = { tab = ParentTab.DASHBOARD },
        )
        ParentTab.LIMITS -> LimitsScreen(
            settings = state.settings,
            onDailyLimit = vm::setDailyLimit,
            onFullLockMinutes = vm::setFullLockMinutes,
            onCountAll = vm::setFullLockCountsAllApps,
            onWeeklyWindow = vm::setWeeklyWindow,
            onBack = { tab = ParentTab.DASHBOARD },
        )
    }
}

@Composable
private fun Dashboard(
    state: com.applimit.ui.MainUiState,
    onCategories: () -> Unit,
    onLimits: () -> Unit,
    onOnboarding: () -> Unit,
    onEmergencyReset: () -> Unit,
    onLogout: () -> Unit,
) {
    val managed = state.managedApps
    val limitedCount = managed.count { it.category.name == "LIMITED" }
    val plusCount = managed.count { it.category.name == "PLUS" }
    val blockedCount = managed.count { it.category.name == "BLOCKED" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppLimitColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            "Elternbereich",
            style = MaterialTheme.typography.headlineMedium,
            color = AppLimitColors.Label,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        IosSectionHeader("Nutzung heute")
        IosCard {
            IosRow(
                title = "Limitierte Nutzung",
                subtitle = "Tageslimit: ${state.settings.dailyLimitMinutes} Min.",
                trailing = {
                    Text(
                        "${state.childDecision?.limitedUsedMinutes ?: 0} Min.",
                        color = AppLimitColors.SecondaryLabel,
                    )
                },
            )
            Sep()
            IosRow(
                title = "Gesamtnutzung",
                subtitle = "Vollsperre bei ${state.settings.fullLockMinutes} Min.",
                trailing = {
                    Text(
                        "${state.childDecision?.totalUsedMinutes ?: 0} Min.",
                        color = AppLimitColors.SecondaryLabel,
                    )
                },
            )
        }

        IosSectionHeader("Verwaltung")
        IosCard {
            IosRow("App-Kategorien", subtitle = "$plusCount Plus · $limitedCount Limit · $blockedCount gesperrt", onClick = onCategories, trailing = { Chevron() })
            Sep()
            IosRow("Zeitlimits", subtitle = "Tages- & Gesamtlimit, Wochenfenster", onClick = onLimits, trailing = { Chevron() })
            Sep()
            IosRow("Berechtigungen", subtitle = "Onboarding erneut öffnen", onClick = onOnboarding, trailing = { Chevron() })
        }

        IosSectionHeader("Notfall")
        IosCard {
            IosRow(
                "Notfall-Entsperren",
                subtitle = "Vollsperre aufheben & Wochenfenster zurücksetzen",
                onClick = onEmergencyReset,
                trailing = { Text("Reset", color = AppLimitColors.Danger, fontWeight = FontWeight.SemiBold) },
            )
        }

        Spacer(Modifier.height(24.dp))
        IosPrimaryButton(text = "Abmelden", color = AppLimitColors.SecondaryLabel) { onLogout() }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Sep() {
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(AppLimitColors.Separator))
}

@Composable
private fun Chevron() {
    Text("›", color = AppLimitColors.SecondaryLabel, style = MaterialTheme.typography.titleLarge)
}
