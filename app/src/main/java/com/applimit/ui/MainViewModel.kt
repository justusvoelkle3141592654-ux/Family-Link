package com.applimit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.applimit.data.db.AppCategory
import com.applimit.data.db.ManagedApp
import com.applimit.data.prefs.AppSettings
import com.applimit.data.repository.AppLimitRepository
import com.applimit.data.repository.InstalledApp
import com.applimit.domain.LimitDecision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Who is currently allowed in. */
enum class AuthLevel { NONE, CHILD, PARENT }

/** Top-level destinations. */
enum class Screen { SETUP, PIN_ENTRY, ONBOARDING, HOME, PARENT }

data class MainUiState(
    val loading: Boolean = true,
    val needsSetup: Boolean = false,
    val auth: AuthLevel = AuthLevel.NONE,
    val screen: Screen = Screen.PIN_ENTRY,
    val weeklyLocked: Boolean = false,
    val lockReason: String = "",
    val pinError: String? = null,
    val settings: AppSettings = AppSettings(),
    val managedApps: List<ManagedApp> = emptyList(),
    val installedApps: List<InstalledApp> = emptyList(),
    val childDecision: LimitDecision? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppLimitRepository.get(app)

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        refresh()
        observe()
    }

    private fun observe() {
        viewModelScope.launch {
            repo.settings.collect { s -> _state.value = _state.value.copy(settings = s) }
        }
        viewModelScope.launch {
            repo.managedApps.collect { apps ->
                _state.value = _state.value.copy(managedApps = apps)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val needsSetup = !(repo.pinStore.isChildPinSet() && repo.pinStore.isParentPinSet())
            _state.value = _state.value.copy(
                loading = false,
                needsSetup = needsSetup,
                screen = if (needsSetup) Screen.SETUP else _state.value.screen,
            )
        }
    }

    // ----- Setup (first launch) -----

    fun completeSetup(childPin: String, parentPin: String) {
        viewModelScope.launch {
            repo.pinStore.setChildPin(childPin)
            repo.pinStore.setParentPin(parentPin)
            _state.value = _state.value.copy(
                needsSetup = false,
                auth = AuthLevel.PARENT,
                screen = Screen.ONBOARDING,
            )
        }
    }

    // ----- PIN entry -----

    fun submitPin(pin: String) {
        viewModelScope.launch {
            when {
                repo.pinStore.verifyParentPin(pin) -> {
                    // Parent bypasses the weekly lock (emergency access, Punkt 6).
                    _state.value = _state.value.copy(
                        auth = AuthLevel.PARENT, screen = Screen.PARENT, pinError = null,
                    )
                }
                repo.pinStore.verifyChildPin(pin) -> {
                    val canOpen = repo.canChildOpenNow()
                    if (canOpen) {
                        repo.markChildOpened()
                        loadChildDecision()
                        _state.value = _state.value.copy(
                            auth = AuthLevel.CHILD, screen = Screen.HOME,
                            weeklyLocked = false, pinError = null,
                        )
                    } else {
                        _state.value = _state.value.copy(
                            weeklyLocked = true,
                            lockReason = repo.lockReason(),
                            pinError = null,
                        )
                    }
                }
                else -> _state.value = _state.value.copy(pinError = "Falscher PIN")
            }
        }
    }

    fun clearPinError() {
        _state.value = _state.value.copy(pinError = null)
    }

    fun logout() {
        _state.value = _state.value.copy(
            auth = AuthLevel.NONE, screen = Screen.PIN_ENTRY, weeklyLocked = false,
        )
    }

    fun goToOnboarding() {
        _state.value = _state.value.copy(screen = Screen.ONBOARDING)
    }

    fun goToParentHome() {
        _state.value = _state.value.copy(screen = Screen.PARENT)
    }

    // ----- Child home -----

    fun loadChildDecision() {
        viewModelScope.launch {
            val decision = repo.evaluate(getApplication<Application>().packageName)
            _state.value = _state.value.copy(childDecision = decision)
        }
    }

    // ----- Parent actions -----

    fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = repo.installedApps()
            _state.value = _state.value.copy(installedApps = apps)
        }
    }

    fun setCategory(app: InstalledApp, category: AppCategory?) {
        viewModelScope.launch {
            if (category == null) repo.clearCategory(app.packageName)
            else repo.setCategory(app.packageName, app.appName, category)
        }
    }

    fun setDailyLimit(minutes: Int) {
        viewModelScope.launch { repo.settingsStore.setDailyLimit(minutes) }
    }

    fun setFullLockMinutes(minutes: Int) {
        viewModelScope.launch { repo.settingsStore.setFullLockMinutes(minutes) }
    }

    fun setFullLockCountsAllApps(value: Boolean) {
        viewModelScope.launch { repo.settingsStore.setFullLockCountsAllApps(value) }
    }

    fun setWeeklyWindow(dayOfWeek: Int, startHour: Int, endHour: Int) {
        viewModelScope.launch { repo.settingsStore.setWeeklyWindow(dayOfWeek, startHour, endHour) }
    }

    /** Master switch: turn the whole protection on/off (setup mode when off). */
    fun setProtectionEnabled(value: Boolean) {
        viewModelScope.launch {
            repo.settingsStore.setProtectionEnabled(value)
            if (!value) com.applimit.service.Enforcer.clearOverlay()
        }
    }

    fun setQuietTimeEnabled(value: Boolean) {
        viewModelScope.launch { repo.settingsStore.setQuietTimeEnabled(value) }
    }

    fun setUsageWindow(startHour: Int, endHour: Int) {
        viewModelScope.launch { repo.settingsStore.setUsageWindow(startHour, endHour) }
    }

    fun setWeeklyLockEnabled(value: Boolean) {
        viewModelScope.launch { repo.settingsStore.setWeeklyLockEnabled(value) }
    }

    fun emergencyReset() {
        viewModelScope.launch {
            repo.parentEmergencyReset()
            com.applimit.service.Enforcer.clearOverlay()
        }
    }
}
