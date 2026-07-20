package com.applimit.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.applimit.data.db.AppCategory
import com.applimit.data.repository.UsageOverview
import com.applimit.ui.MainViewModel
import com.applimit.ui.components.IosCard
import com.applimit.ui.components.IosPrimaryButton
import com.applimit.ui.components.IosRow
import com.applimit.ui.components.IosSectionHeader
import com.applimit.ui.components.IosSwitch
import com.applimit.ui.theme.AppLimitColors

private enum class ParentTab { DASHBOARD, CATEGORIES, LIMITS, PASSWORD }

/** Parent management area. Reached only via the parent PIN. */
@Composable
fun ParentScreen(
    vm: MainViewModel,
    onOpenOnboarding: () -> Unit,
    onLogout: () -> Unit,
) {
    var tab by remember { mutableStateOf(ParentTab.DASHBOARD) }
    val state by vm.state.collectAsState()

    androidx.compose.runtime.LaunchedEffect(tab) {
        if (tab == ParentTab.DASHBOARD) vm.loadOverview()
    }

    when (tab) {
        ParentTab.DASHBOARD -> Dashboard(
            state = state,
            onToggleProtection = { vm.setProtectionEnabled(it) },
            onPauseLimits = { vm.pauseLimitsUntil23() },
            onResumeLimits = { vm.resumeLimits() },
            onCategories = { vm.loadInstalledApps(); tab = ParentTab.CATEGORIES },
            onLimits = { tab = ParentTab.LIMITS },
            onPassword = { tab = ParentTab.PASSWORD },
            onOnboarding = onOpenOnboarding,
            onEmergencyReset = { vm.emergencyReset() },
            onLogout = onLogout,
        )
        ParentTab.CATEGORIES -> CategoriesScreen(
            installed = state.installedApps,
            managed = state.managedApps,
            protectionEnabled = state.settings.protectionEnabled,
            onSave = { selections -> vm.saveCategories(selections) },
            onBack = { tab = ParentTab.DASHBOARD },
        )
        ParentTab.LIMITS -> LimitsScreen(
            settings = state.settings,
            onGeneralLimit = vm::setGeneralLimit,
            onGlobalLimit = vm::setGlobalLimit,
            onQuietTimeEnabled = vm::setQuietTimeEnabled,
            onUsageWindow = vm::setUsageWindow,
            onWeeklyLockEnabled = vm::setWeeklyLockEnabled,
            onWeeklyWindow = vm::setWeeklyWindow,
            onBack = { tab = ParentTab.DASHBOARD },
        )
        ParentTab.PASSWORD -> ChangePasswordScreen(
            onChange = { old, new -> vm.changeParentPassword(old, new) },
            onBack = { tab = ParentTab.DASHBOARD },
        )
    }
}

@Composable
private fun Dashboard(
    state: com.applimit.ui.MainUiState,
    onToggleProtection: (Boolean) -> Unit,
    onPauseLimits: () -> Unit,
    onResumeLimits: () -> Unit,
    onCategories: () -> Unit,
    onLimits: () -> Unit,
    onPassword: () -> Unit,
    onOnboarding: () -> Unit,
    onEmergencyReset: () -> Unit,
    onLogout: () -> Unit,
) {
    val managed = state.managedApps
    val plusCount = managed.count { it.category == AppCategory.PLUS }
    val standardCount = managed.count { it.category == AppCategory.STANDARD }
    val limitCount = managed.count { it.category == AppCategory.LIMIT }
    val blockedCount = managed.count { it.category == AppCategory.BLOCKED }
    val ov = state.overview

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

        IosSectionHeader("Schutz")
        IosCard {
            IosRow(
                title = if (state.settings.protectionEnabled) "Schutz aktiv" else "Schutz inaktiv (Einrichtung)",
                subtitle = if (state.settings.protectionEnabled)
                    "Limits, Sperre & Ruhezeit sind aktiv."
                else
                    "Nichts wird gesperrt. Zum Aktivieren einschalten, wenn alles eingerichtet ist.",
                trailing = { IosSwitch(state.settings.protectionEnabled) { onToggleProtection(it) } },
            )
        }

        val paused = state.settings.limitsPausedUntilMillis > System.currentTimeMillis()
        IosSectionHeader("Limits vorübergehend aus")
        IosCard {
            IosRow(
                title = if (paused) "Limits pausiert – bis 23:00 Uhr" else "Limits aktiv",
                subtitle = if (paused)
                    "Sperren & Ruhezeit sind heute bis 23:00 Uhr aus."
                else
                    "Für heute bis 23:00 Uhr alle Limits & Ruhezeit ausschalten.",
                onClick = { if (paused) onResumeLimits() else onPauseLimits() },
                trailing = {
                    Text(
                        if (paused) "Wieder an" else "Aus bis 23:00",
                        color = if (paused) AppLimitColors.Success else AppLimitColors.Accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        }

        IosSectionHeader("Verbrauchte Zeit heute")
        IosCard {
            UsageBar("Globales Limit", ov?.globalUsedSeconds ?: 0, ov?.globalLimitSeconds ?: (state.settings.globalLimitMinutes * 60L), AppLimitColors.Accent)
            Sep()
            UsageBar("Allgemeines Limit", ov?.generalUsedSeconds ?: 0, ov?.generalLimitSeconds ?: (state.settings.generalLimitMinutes * 60L), AppLimitColors.Success)
        }

        if (ov != null && ov.apps.any { it.usedSeconds > 0 }) {
            IosSectionHeader("Einzelne App-Zeiten")
            IosCard {
                val used = ov.apps.filter { it.usedSeconds > 0 }
                used.forEachIndexed { i, a ->
                    if (i > 0) Sep()
                    IosRow(
                        title = a.appName,
                        subtitle = a.individualLimitMinutes?.let { "Eigenes Limit: $it Min." },
                        trailing = { Text(fmt(a.usedSeconds), color = AppLimitColors.SecondaryLabel) },
                    )
                }
            }
        }

        IosSectionHeader("Verwaltung")
        IosCard {
            IosRow("App-Kategorien", subtitle = "$plusCount Plus · $standardCount Standard · $limitCount Limit · $blockedCount gesperrt", onClick = onCategories, trailing = { Chevron() })
            Sep()
            IosRow("Zeitlimits & Ruhezeit", subtitle = "Allgemein ${state.settings.generalLimitMinutes} / Global ${state.settings.globalLimitMinutes} Min.", onClick = onLimits, trailing = { Chevron() })
            Sep()
            IosRow("Passwort ändern", subtitle = "Eltern-PIN neu setzen", onClick = onPassword, trailing = { Chevron() })
            Sep()
            IosRow("Berechtigungen", subtitle = "Onboarding erneut öffnen", onClick = onOnboarding, trailing = { Chevron() })
        }

        IosSectionHeader("Notfall")
        IosCard {
            IosRow(
                "Wochenfenster zurücksetzen",
                subtitle = "Portal-Öffnung wieder freigeben",
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
private fun UsageBar(label: String, usedSec: Long, limitSec: Long, color: androidx.compose.ui.graphics.Color) {
    val fraction = if (limitSec > 0) (usedSec.toFloat() / limitSec).coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row {
            Text(label, color = AppLimitColors.Label, modifier = Modifier.weight(1f))
            Text("${fmt(usedSec)} / ${fmt(limitSec)}", color = AppLimitColors.SecondaryLabel)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(AppLimitColors.Separator),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (fraction >= 1f) AppLimitColors.Danger else color),
            )
        }
    }
}

/** Formats seconds as "M Min." or "H Std. M Min." */
private fun fmt(sec: Long): String {
    val totalMin = (sec / 60).toInt()
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "$h Std. $m Min." else "$m Min."
}

@Composable
private fun Sep() {
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(AppLimitColors.Separator))
}

@Composable
private fun Chevron() {
    Text("›", color = AppLimitColors.SecondaryLabel, style = MaterialTheme.typography.titleLarge)
}
