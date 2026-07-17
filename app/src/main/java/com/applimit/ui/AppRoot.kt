package com.applimit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.applimit.ui.home.HomeScreen
import com.applimit.ui.onboarding.OnboardingScreen
import com.applimit.ui.parent.ParentScreen
import com.applimit.ui.pin.PinScreen
import com.applimit.ui.pin.SetupScreen
import com.applimit.ui.theme.AppLimitColors

/**
 * Root navigation. Everything is gated behind the PIN screen (Prompt Punkt 1):
 * the app cannot be used without entering a PIN, and the child path is further
 * gated by the once-per-week window.
 */
@Composable
fun AppRoot(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    if (state.loading) {
        Box(
            Modifier.fillMaxSize().background(AppLimitColors.Background),
            contentAlignment = Alignment.Center,
        ) { Text("…", color = AppLimitColors.SecondaryLabel) }
        return
    }

    when (state.screen) {
        Screen.SETUP -> SetupScreen(onComplete = vm::completeSetup)

        Screen.PIN_ENTRY -> PinScreen(
            error = state.pinError,
            weeklyLocked = state.weeklyLocked,
            lockReason = state.lockReason,
            onSubmit = vm::submitPin,
            onErrorConsumed = vm::clearPinError,
        )

        Screen.ONBOARDING -> OnboardingScreen(onDone = vm::goToParentHome)

        Screen.HOME -> HomeScreen(
            decision = state.childDecision,
            managedApps = state.managedApps,
            settings = state.settings,
            onRefresh = vm::loadChildDecision,
        )

        Screen.PARENT -> ParentScreen(
            vm = vm,
            onOpenOnboarding = vm::goToOnboarding,
            onLogout = vm::logout,
        )
    }
}
