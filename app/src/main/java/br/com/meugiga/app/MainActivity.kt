package br.com.meugiga.app

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meugiga.app.ui.MeugigaApp
import br.com.meugiga.app.ui.screens.OnboardingScreen
import br.com.meugiga.app.ui.theme.MeugigaTheme
import br.com.meugiga.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.factory((application as MeugigaApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by mainViewModel.state.collectAsStateWithLifecycle()
            MeugigaTheme(state.settings.themeMode) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when {
                        !state.initialized -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        !state.settings.onboardingComplete -> OnboardingScreen(
                            hasUsageAccess = state.hasUsageAccess,
                            onSavePlan = mainViewModel::savePlan,
                            onOpenUsageAccess = ::openUsageAccessSettings,
                            onFinish = mainViewModel::finishOnboarding,
                        )
                        else -> MeugigaApp(
                            state = state,
                            onGrantUsageAccess = ::openUsageAccessSettings,
                            onRefresh = mainViewModel::refreshNow,
                            onPresetSelected = mainViewModel::selectPeriod,
                            onCustomSelected = mainViewModel::selectCustomPeriod,
                            onSavePlan = mainViewModel::savePlan,
                            onSetPersistentNotification = mainViewModel::setPersistentNotification,
                            onSetAlerts = mainViewModel::setAlerts,
                            onSetBackgroundInterval = mainViewModel::setBackgroundInterval,
                            onSetTheme = mainViewModel::setTheme,
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mainViewModel.refreshPermissionState()
        mainViewModel.startForegroundUpdates()
    }

    override fun onStop() {
        mainViewModel.stopForegroundUpdates()
        super.onStop()
    }

    private fun openUsageAccessSettings() {
        runCatching { startActivity(mainViewModel.usageAccessSettingsIntent()) }
            .onFailure { startActivity(android.content.Intent(Settings.ACTION_SETTINGS)) }
    }
}
