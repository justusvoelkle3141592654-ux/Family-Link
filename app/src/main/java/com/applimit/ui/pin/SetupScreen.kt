package com.applimit.ui.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import com.applimit.ui.theme.AppLimitColors

/**
 * First-launch setup (Prompt Punkt 1 & 6): choose the 4-digit child PIN and,
 * separately, a longer parent PIN. Each PIN is entered twice to confirm.
 */
private enum class Step { CHILD_ENTER, CHILD_CONFIRM, PARENT_ENTER, PARENT_CONFIRM }

@Composable
fun SetupScreen(onComplete: (childPin: String, parentPin: String) -> Unit) {
    var step by remember { mutableStateOf(Step.CHILD_ENTER) }
    var childFirst by remember { mutableStateOf("") }
    var childPin by remember { mutableStateOf("") }
    var parentFirst by remember { mutableStateOf("") }
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val isChildStep = step == Step.CHILD_ENTER || step == Step.CHILD_CONFIRM

    fun advance(value: String) {
        error = null
        when (step) {
            Step.CHILD_ENTER -> {
                if (value.length != 4) { error = "Der Kinder-PIN muss 4 Ziffern haben."; return }
                childFirst = value; entered = ""; step = Step.CHILD_CONFIRM
            }
            Step.CHILD_CONFIRM -> {
                if (value != childFirst) { error = "PINs stimmen nicht überein."; entered = ""; step = Step.CHILD_ENTER; return }
                childPin = value; entered = ""; step = Step.PARENT_ENTER
            }
            Step.PARENT_ENTER -> {
                if (value.length < 6) { error = "Der Eltern-PIN sollte mind. 6 Ziffern haben."; return }
                parentFirst = value; entered = ""; step = Step.PARENT_CONFIRM
            }
            Step.PARENT_CONFIRM -> {
                if (value != parentFirst) { error = "PINs stimmen nicht überein."; entered = ""; step = Step.PARENT_ENTER; return }
                onComplete(childPin, value)
            }
        }
    }

    val (title, subtitle) = when (step) {
        Step.CHILD_ENTER -> "Kinder-PIN festlegen" to "4 Ziffern, die das Kind zum Öffnen nutzt."
        Step.CHILD_CONFIRM -> "Kinder-PIN bestätigen" to "Gib den Kinder-PIN erneut ein."
        Step.PARENT_ENTER -> "Eltern-PIN festlegen" to "Mindestens 6 Ziffern. Nur für Eltern."
        Step.PARENT_CONFIRM -> "Eltern-PIN bestätigen" to "Gib den Eltern-PIN erneut ein."
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
        PinDots(length = if (isChildStep) 4 else maxOf(entered.length, 6), filled = entered.length)

        Spacer(Modifier.weight(1f))
        PinPad(
            onDigit = { d ->
                if (entered.length < 12) {
                    entered += d.toString()
                    if (isChildStep && entered.length == 4) advance(entered)
                }
            },
            onBackspace = { if (entered.isNotEmpty()) entered = entered.dropLast(1) },
        )
        Spacer(Modifier.height(16.dp))
        if (!isChildStep) {
            com.applimit.ui.components.IosPrimaryButton(
                text = "Weiter", enabled = entered.length >= 6,
            ) { advance(entered) }
        }
        Spacer(Modifier.height(24.dp))
    }
}
