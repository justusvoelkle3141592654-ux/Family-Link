package com.applimit.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.applimit.ui.components.IosCard
import com.applimit.ui.components.IosPrimaryButton
import com.applimit.ui.components.IosSectionHeader
import com.applimit.ui.theme.AppLimitColors
import com.applimit.util.PermissionsHelper

/**
 * Step-by-step permission onboarding (Prompt "Berechtigungs-Onboarding-Flow").
 * Re-checks each permission whenever the screen resumes (i.e. after the user
 * comes back from a Settings page).
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val helper = remember { PermissionsHelper(context) }

    var refreshKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Reading refreshKey ties these checks to the resume ticker so they are
    // re-evaluated every time the user returns from a Settings page.
    @Suppress("UNUSED_EXPRESSION")
    refreshKey
    val usage = helper.hasUsageAccess()
    val overlay = helper.hasOverlay()
    val accessibility = helper.hasAccessibility()
    val admin = helper.hasDeviceAdmin()
    val requiredDone = usage && overlay && accessibility

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppLimitColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            "Berechtigungen einrichten",
            style = MaterialTheme.typography.headlineMedium,
            color = AppLimitColors.Label,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Diese Sonderzugriffe müssen manuell in den Android-Einstellungen " +
                "erteilt werden – sie können nicht automatisch angefordert werden.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppLimitColors.SecondaryLabel,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        IosSectionHeader("Erforderlich")
        IosCard {
            PermissionRow(
                "Nutzungszugriff",
                "Zum Messen der App-Nutzungszeit (UsageStatsManager).",
                usage,
            ) { helper.openUsageAccessSettings() }
            RowDivider()
            PermissionRow(
                "Bedienungshilfe",
                "Erkennt App-Wechsel in Echtzeit für zuverlässige Overlays.",
                accessibility,
            ) { helper.openAccessibilitySettings() }
            RowDivider()
            PermissionRow(
                "Über anderen Apps anzeigen",
                "Zeigt das Sperr-Pop-up über anderen Apps (SYSTEM_ALERT_WINDOW).",
                overlay,
            ) { helper.openOverlaySettings() }
        }

        IosSectionHeader("Optional – stärkere Sperre")
        IosCard {
            PermissionRow(
                "Geräteadministrator",
                "Ermöglicht echtes Sperren des Bildschirms (lockNow) statt nur Overlay.",
                admin,
            ) { helper.openDeviceAdmin() }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Hinweis: Ohne Geräteadministrator ist nur eine Overlay-Sperre möglich. " +
                "Eine vollständig unumgehbare Sperre (wie bei Family Link) erfordert " +
                "den „Device-Owner\"-Modus, der nur bei der Ersteinrichtung eines " +
                "zurückgesetzten Geräts eingerichtet werden kann.",
            style = MaterialTheme.typography.labelMedium,
            color = AppLimitColors.SecondaryLabel,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )

        Spacer(Modifier.height(24.dp))
        IosPrimaryButton(
            text = if (requiredDone) "Fertig" else "Später fortfahren",
            color = if (requiredDone) AppLimitColors.Accent else AppLimitColors.SecondaryLabel,
        ) { onDone() }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            if (granted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (granted) AppLimitColors.Success else AppLimitColors.Separator,
            modifier = Modifier.size(26.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(title, color = AppLimitColors.Label, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                color = AppLimitColors.SecondaryLabel,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        if (!granted) {
            Text(
                "Öffnen",
                color = AppLimitColors.Accent,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { onClick() },
            )
        }
    }
}

@Composable
private fun RowDivider() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppLimitColors.Separator),
    )
}
