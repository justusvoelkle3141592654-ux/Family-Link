package com.applimit.ui.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.applimit.ui.components.IosPrimaryButton
import com.applimit.ui.theme.AppLimitColors
import kotlinx.coroutines.delay

private const val CHILD_PIN_LENGTH = 4

/**
 * PIN entry (Prompt Punkt 7: dots on top, keypad below).
 *
 * Two modes on one screen to avoid a length collision between the 4-digit child
 * PIN and the longer parent PIN:
 *  - Child mode: 4 dots, auto-submits when full.
 *  - Parent mode: variable length, explicit "Bestätigen" button. Reached via the
 *    subtle "Eltern-Zugang" link, and always available even when weekly-locked
 *    (emergency access, Punkt 6).
 */
@Composable
fun PinScreen(
    error: String?,
    weeklyLocked: Boolean,
    lockReason: String,
    onSubmit: (String) -> Unit,
    onErrorConsumed: () -> Unit,
) {
    var entered by remember { mutableStateOf("") }
    var parentMode by remember { mutableStateOf(false) }

    LaunchedEffect(entered, parentMode) {
        if (!parentMode && entered.length == CHILD_PIN_LENGTH) {
            onSubmit(entered)
        }
    }
    LaunchedEffect(error) {
        if (error != null) {
            delay(600)
            entered = ""
            onErrorConsumed()
        }
    }

    val title = if (parentMode) "Eltern-PIN" else "PIN eingeben"
    val subtitle = if (parentMode) {
        "Gib den Eltern-PIN ein, um in den Elternbereich zu gelangen."
    } else {
        "Willkommen zurück!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppLimitColors.Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(60.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = AppLimitColors.Label)
        Spacer(Modifier.height(8.dp))
        Text(
            error ?: subtitle,
            color = if (error != null) AppLimitColors.Danger else AppLimitColors.SecondaryLabel,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(36.dp))

        if (parentMode) {
            // Show one dot per entered digit, minimum 4 placeholders.
            PinDots(length = maxOf(entered.length, 4), filled = entered.length)
        } else {
            PinDots(length = CHILD_PIN_LENGTH, filled = entered.length)
        }

        if (weeklyLocked && !parentMode) {
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier
                    .background(AppLimitColors.Card, MaterialTheme.shapes.medium)
                    .padding(16.dp),
            ) {
                Text(
                    lockReason,
                    color = AppLimitColors.Label,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        PinPad(
            onDigit = { d -> if (entered.length < 12) entered += d.toString() },
            onBackspace = { if (entered.isNotEmpty()) entered = entered.dropLast(1) },
        )
        Spacer(Modifier.height(16.dp))

        if (parentMode) {
            IosPrimaryButton(text = "Bestätigen", enabled = entered.length >= 4) {
                onSubmit(entered)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Zurück",
                color = AppLimitColors.Accent,
                modifier = Modifier.clickable { parentMode = false; entered = "" },
            )
        } else {
            Text(
                "Eltern-Zugang",
                color = AppLimitColors.Accent,
                modifier = Modifier.clickable { parentMode = true; entered = "" },
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
