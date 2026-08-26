package br.com.meugiga.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.meugiga.app.domain.model.ThemeMode
import br.com.meugiga.app.domain.model.PeriodPreset
import br.com.meugiga.app.ui.screens.AboutScreen
import br.com.meugiga.app.ui.screens.AppDetailScreen
import br.com.meugiga.app.ui.screens.AppsScreen
import br.com.meugiga.app.ui.screens.HistoryScreen
import br.com.meugiga.app.ui.screens.HomeScreen
import br.com.meugiga.app.ui.screens.PrivacyScreen
import br.com.meugiga.app.ui.screens.SettingsScreen
import br.com.meugiga.app.viewmodel.MainUiState
import java.time.LocalDate

private data class MainDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val destinations = listOf(
    MainDestination("summary", "Resumo", Icons.Rounded.Home),
    MainDestination("apps", "Aplicativos", Icons.Rounded.Apps),
    MainDestination("history", "Histórico", Icons.Rounded.History),
    MainDestination("settings", "Configurações", Icons.Rounded.Settings),
)

@Composable
fun MeugigaApp(
    state: MainUiState,
    onGrantUsageAccess: () -> Unit,
    onRefresh: () -> Unit,
    onPresetSelected: (PeriodPreset) -> Unit,
    onCustomSelected: (LocalDate, LocalDate) -> Unit,
    onSavePlan: (Long?, Int, Boolean, LocalDate?) -> Unit,
    onSetPersistentNotification: (Boolean) -> Unit,
    onSetAlerts: (Boolean, Set<Int>) -> Unit,
    onSetBackgroundInterval: (Int) -> Unit,
    onSetTheme: (ThemeMode) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "summary"
    val isMainDestination = destinations.any { it.route == route }

    Scaffold(
        bottomBar = {
            if (isMainDestination) {
                NavigationBar {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = route == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
        contentWindowInsets = if (isMainDestination) WindowInsets.safeDrawing else WindowInsets(0, 0, 0, 0),
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "summary",
            modifier = Modifier.padding(padding),
        ) {
            composable("summary") {
                HomeScreen(
                    state,
                    onGrantUsageAccess,
                    onRefresh,
                    onOpenApp = { navController.navigate("app/$it") },
                )
            }
            composable("apps") {
                AppsScreen(
                    state,
                    onGrantUsageAccess,
                    onPresetSelected,
                    onCustomSelected,
                    onOpenApp = { navController.navigate("app/$it") },
                    onRefresh = onRefresh,
                )
            }
            composable("history") {
                HistoryScreen(
                    state,
                    onGrantUsageAccess,
                    onPresetSelected,
                    onCustomSelected,
                    onRefresh,
                )
            }
            composable("settings") {
                SettingsScreen(
                    state = state,
                    onGrantUsageAccess = onGrantUsageAccess,
                    onSavePlan = onSavePlan,
                    onSetPersistentNotification = onSetPersistentNotification,
                    onSetAlerts = onSetAlerts,
                    onSetBackgroundInterval = onSetBackgroundInterval,
                    onSetTheme = onSetTheme,
                    onOpenPrivacy = { navController.navigate("privacy") },
                    onOpenAbout = { navController.navigate("about") },
                )
            }
            composable("app/{uid}") { entry ->
                val uid = entry.arguments?.getString("uid")?.toIntOrNull() ?: return@composable
                AppDetailScreen(
                    uid = uid,
                    state = state,
                    onBack = navController::navigateUp,
                    onPresetSelected = onPresetSelected,
                    onCustomSelected = onCustomSelected,
                )
            }
            composable("privacy") {
                PrivacyScreen(onBack = navController::navigateUp)
            }
            composable("about") {
                AboutScreen(onBack = navController::navigateUp)
            }
        }
    }
}
