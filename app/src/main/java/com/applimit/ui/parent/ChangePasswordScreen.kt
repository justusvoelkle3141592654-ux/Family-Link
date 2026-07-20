package com.applimit.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.applimit.ui.components.IosPrimaryButton
import com.applimit.ui.pin.PinDots
import com.applimit.ui.pin.PinPad
import com.applimit.ui.theme.AppLimitColors

private enum class Step { OLD, NEW, CONFIRM, DONE }

/**
 * Change the parent password: verify the old one, then enter the new one twice.
 * [onChange] returns false if the old password was wrong.
 */
@Composable
fun ChangePasswordScreen(
    onChange: (old: String, new: String) -> Boolean,
    onBack: () -> Unit,
) {
    var step by remember { mutableStateOf(Step.OLD) }
    var oldPin by remember { mutableStateOf("") }
    var newFirst by remember { mutableStateOf("") }
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun proceed() {
        error = null
        when (step) {
            Step.OLD -> { oldPin = entered; entered = ""; step = Step.NEW }
            Step.NEW -> {
                if (entered.length < 6) { error = "Neuer PIN: mind. 6 Ziffern."; return }
                newFirst = entered; entered = ""; step = Step.CONFIRM
            }
            Step.CONFIRM -> {
                if (entered != newFirst) { error = "PINs stimmen nicht überein."; entered = ""; step = Step.NEW; return }
                if (onChange(oldPin, entered)) {
                    step = Step.DONE
                } else {
                    error = "Altes Passwort falsch."; entered = ""; oldPin = ""; step = Step.OLD
                }
            }
            Step.DONE -> onBack()
        }
    }

    Column(
        Modifier.fillMaxSize().background(AppLimitColors.Background),
    ) {
        ParentTopBar(title = "Passwort ändern", onBack = onBack)
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val (title, sub) = when (step) {
                Step.OLD -> "Altes Passwort" to "Gib das aktuelle Eltern-Passwort ein."
                Step.NEW -> "Neues Passwort" to "Mindestens 6 Ziffern."
                Step.CONFIRM -> "Neues Passwort bestätigen" to "Gib das neue Passwort erneut ein."
                Step.DONE -> "Fertig ✅" to "Das Passwort wurde geändert."
            }
            Spacer(Modifier.height(32.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = AppLimitColors.Label)
            Spacer(Modifier.height(8.dp))
            Text(
                error ?: sub,
                color = if (error != null) AppLimitColors.Danger else AppLimitColors.SecondaryLabel,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(28.dp))
            if (step != Step.DONE) PinDots(length = maxOf(entered.length, 4), filled = entered.length)

            Spacer(Modifier.weight(1f))
            if (step == Step.DONE) {
                IosPrimaryButton(text = "Zurück") { onBack() }
            } else {
                PinPad(
                    onDigit = { d -> if (entered.length < 12) entered += d.toString() },
                    onBackspace = { if (entered.isNotEmpty()) entered = entered.dropLast(1) },
                )
                Spacer(Modifier.height(16.dp))
                IosPrimaryButton(text = "Weiter", enabled = entered.length >= 4) { proceed() }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
