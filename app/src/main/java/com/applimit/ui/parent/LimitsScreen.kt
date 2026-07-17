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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.applimit.data.prefs.AppSettings
import com.applimit.ui.components.IosCard
import com.applimit.ui.components.IosRow
import com.applimit.ui.components.IosSectionHeader
import com.applimit.ui.components.IosSwitch
import com.applimit.ui.theme.AppLimitColors

/**
 * Time-limit + Ruhezeit configuration (Prompt Punkt 3). Steppers can never
 * exceed the hard caps in [AppSettings]; the SettingsStore also clamps on write,
 * so the 2h ceiling is enforced in code.
 */
@Composable
fun LimitsScreen(
    settings: AppSettings,
    onDailyLimit: (Int) -> Unit,
    onFullLockMinutes: (Int) -> Unit,
    onCountAll: (Boolean) -> Unit,
    onQuietTimeEnabled: (Boolean) -> Unit,
    onUsageWindow: (Int, Int) -> Unit,
    onWeeklyLockEnabled: (Boolean) -> Unit,
    onWeeklyWindow: (Int, Int, Int) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AppLimitColors.Background),
    ) {
        ParentTopBar(title = "Zeitlimits", onBack = onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            IosSectionHeader("Tageslimit (Apps mit Limit)")
            IosCard {
                Stepper(
                    label = "Limit",
                    valueLabel = "${settings.dailyLimitMinutes} Min.",
                    onMinus = { onDailyLimit(settings.dailyLimitMinutes - 5) },
                    onPlus = { onDailyLimit(settings.dailyLimitMinutes + 5) },
                    minusEnabled = settings.dailyLimitMinutes > AppSettings.MIN_DAILY_LIMIT_MIN,
                    plusEnabled = settings.dailyLimitMinutes < AppSettings.MAX_DAILY_LIMIT_MIN,
                )
            }
            Hint("Standard 1 Std., harte Obergrenze 2 Std. (im Code erzwungen).")

            IosSectionHeader("Geräte-Vollsperre")
            IosCard {
                Stepper(
                    label = "Gesamtzeit bis Sperre",
                    valueLabel = "${settings.fullLockMinutes} Min.",
                    onMinus = { onFullLockMinutes(settings.fullLockMinutes - 5) },
                    onPlus = { onFullLockMinutes(settings.fullLockMinutes + 5) },
                    minusEnabled = settings.fullLockMinutes > 30,
                    plusEnabled = settings.fullLockMinutes < AppSettings.MAX_FULL_LOCK_MIN,
                )
                Sep()
                IosRow(
                    title = "Alle Apps zählen mit",
                    subtitle = if (settings.fullLockCountsAllApps)
                        "Auch Plus-Apps zählen in die Gesamtzeit"
                    else "Nur limitierte Apps zählen in die Gesamtzeit",
                    trailing = { IosSwitch(settings.fullLockCountsAllApps) { onCountAll(it) } },
                )
            }

            IosSectionHeader("Ruhezeit (Nutzungsfenster)")
            IosCard {
                IosRow(
                    title = "Ruhezeit aktiv",
                    subtitle = if (settings.quietTimeEnabled)
                        "Außerhalb des Fensters ist das Gerät gesperrt"
                    else "Keine Ruhezeit",
                    trailing = { IosSwitch(settings.quietTimeEnabled) { onQuietTimeEnabled(it) } },
                )
                Sep()
                Stepper(
                    label = "Nutzung erlaubt ab",
                    valueLabel = "${settings.usageWindowStartHour}:00 Uhr",
                    onMinus = { onUsageWindow((settings.usageWindowStartHour - 1).coerceAtLeast(0), settings.usageWindowEndHour) },
                    onPlus = { onUsageWindow((settings.usageWindowStartHour + 1).coerceAtMost(settings.usageWindowEndHour - 1), settings.usageWindowEndHour) },
                )
                Sep()
                Stepper(
                    label = "Nutzung erlaubt bis",
                    valueLabel = "${settings.usageWindowEndHour}:00 Uhr",
                    onMinus = { onUsageWindow(settings.usageWindowStartHour, (settings.usageWindowEndHour - 1).coerceAtLeast(settings.usageWindowStartHour + 1)) },
                    onPlus = { onUsageWindow(settings.usageWindowStartHour, (settings.usageWindowEndHour + 1).coerceAtMost(24)) },
                )
            }
            Hint("Beispiel: 7:00–20:00 Uhr. Außerhalb dieser Zeit sind die Apps gesperrt (Telefon bleibt erreichbar).")

            IosSectionHeader("App nur einmal pro Woche öffenbar")
            IosCard {
                IosRow(
                    title = "Wochensperre",
                    subtitle = if (settings.weeklyLockEnabled)
                        "Kind kann das Portal nur 1×/Woche öffnen"
                    else "Portal ist jederzeit mit Kinder-PIN erreichbar",
                    trailing = { IosSwitch(settings.weeklyLockEnabled) { onWeeklyLockEnabled(it) } },
                )
                if (settings.weeklyLockEnabled) {
                    Sep()
                    Stepper(
                        label = "Wochentag",
                        valueLabel = dayName(settings.weeklyOpenDayOfWeek),
                        onMinus = { onWeeklyWindow(wrapDay(settings.weeklyOpenDayOfWeek - 1), settings.weeklyOpenStartHour, settings.weeklyOpenEndHour) },
                        onPlus = { onWeeklyWindow(wrapDay(settings.weeklyOpenDayOfWeek + 1), settings.weeklyOpenStartHour, settings.weeklyOpenEndHour) },
                    )
                    Sep()
                    Stepper(
                        label = "Von (Uhr)",
                        valueLabel = "${settings.weeklyOpenStartHour}:00",
                        onMinus = { onWeeklyWindow(settings.weeklyOpenDayOfWeek, (settings.weeklyOpenStartHour - 1).coerceAtLeast(0), settings.weeklyOpenEndHour) },
                        onPlus = { onWeeklyWindow(settings.weeklyOpenDayOfWeek, (settings.weeklyOpenStartHour + 1).coerceAtMost(settings.weeklyOpenEndHour - 1), settings.weeklyOpenEndHour) },
                    )
                    Sep()
                    Stepper(
                        label = "Bis (Uhr)",
                        valueLabel = "${settings.weeklyOpenEndHour}:00",
                        onMinus = { onWeeklyWindow(settings.weeklyOpenDayOfWeek, settings.weeklyOpenStartHour, (settings.weeklyOpenEndHour - 1).coerceAtLeast(settings.weeklyOpenStartHour + 1)) },
                        onPlus = { onWeeklyWindow(settings.weeklyOpenDayOfWeek, settings.weeklyOpenStartHour, (settings.weeklyOpenEndHour + 1).coerceAtMost(24)) },
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = AppLimitColors.SecondaryLabel,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
    )
}

@Composable
private fun Stepper(
    label: String,
    valueLabel: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean = true,
    plusEnabled: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = AppLimitColors.Label, modifier = Modifier.weight(1f))
        Text(valueLabel, color = AppLimitColors.SecondaryLabel, modifier = Modifier.padding(end = 12.dp))
        StepButton("–", minusEnabled, onMinus)
        Spacer(Modifier.size(10.dp))
        StepButton("+", plusEnabled, onPlus)
    }
}

@Composable
private fun StepButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (enabled) AppLimitColors.Accent else AppLimitColors.Separator)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Sep() {
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(AppLimitColors.Separator))
}

private fun dayName(dow: Int) = when (dow) {
    1 -> "Montag"; 2 -> "Dienstag"; 3 -> "Mittwoch"; 4 -> "Donnerstag"
    5 -> "Freitag"; 6 -> "Samstag"; else -> "Sonntag"
}

private fun wrapDay(d: Int) = ((d - 1 + 7) % 7) + 1
