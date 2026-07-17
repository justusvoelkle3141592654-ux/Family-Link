package com.applimit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.applimit.service.EnforcementService
import com.applimit.ui.AppRoot
import com.applimit.ui.theme.AppLimitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Ensure the enforcement service is running whenever the app is opened.
        EnforcementService.start(this)

        setContent {
            AppLimitTheme {
                AppRoot()
            }
        }
    }
}
