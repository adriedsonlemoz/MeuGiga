package br.com.meugiga.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import br.com.meugiga.app.domain.model.ThemeMode
import br.com.meugiga.app.ui.components.MobileOnlyBadge
import br.com.meugiga.app.ui.components.PlanEditorDialog
import br.com.meugiga.app.utils.ByteFormatter
import br.com.meugiga.app.viewmodel.MainUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    state: MainUiState,
    onGrantUsageAccess: () -> Unit,
    onSavePlan: (Long?, Int, Boolean, LocalDate?) -> Unit,
    onSetPersistentNotification: (Boolean) -> Unit,
    onSetAlerts: (Boolean, Set<Int>) -> Unit,
    onSetBackgroundInterval: (Int) -> Unit,
    onSetTheme: (ThemeMode) -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showPlanEditor by remember { mutableStateOf(false) }
    var showCustomAlert by remember { mutableStateOf(false) }
    var pendingNotificationAction by remember { mutableIntStateOf(0) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            when (pendingNotificationAction) {
                1 -> onSetPersistentNotification(true)
                2 -> onSetAlerts(true, state.settings.alertThresholds)
            }
        }
        pendingNotificationAction = 0
    }

    fun requestNotifications(action: Int, onGranted: () -> Unit) {
        val alreadyGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) onGranted()
        else {
            pendingNotificationAction = action
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("Configurações", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Ajuste o MeuGiga ao seu plano",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            MobileOnlyBadge()
        }

        item {
            SettingsCard("Plano de internet", Icons.Rounded.CellTower) {
                SettingValueRow(
                    title = if (state.settings.plan.unlimited) "Sem limite definido"
                    else state.settings.plan.totalBytes?.let(ByteFormatter::format) ?: "Não definido",
                    subtitle = buildString {
                        append("Renovação todo dia ${state.settings.plan.renewalDay}")
                        state.settings.plan.cycleStartDate?.let {
                            append(" • início ${it.format(PLAN_DATE_FORMATTER)}")
                        }
                    },
                    onClick = { showPlanEditor = true },
                )
            }
        }

        item {
            SettingsCard("Acesso", Icons.Rounded.Security) {
                SettingValueRow(
                    title = "Estatísticas de uso",
                    subtitle = if (state.hasUsageAccess) "Acesso concedido" else "Acesso necessário",
                    onClick = onGrantUsageAccess,
                )
            }
        }

        item {
            SettingsCard("Notificações", Icons.Rounded.Notifications) {
                ToggleSetting(
                    title = "Acompanhamento contínuo",
                    subtitle = "Mostra usado, plano, hoje e restante",
                    checked = state.settings.persistentNotificationEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) requestNotifications(1) { onSetPersistentNotification(true) }
                        else onSetPersistentNotification(false)
                    },
                )
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                ToggleSetting(
                    title = "Alertas de consumo",
                    subtitle = "Avisa ao atingir os percentuais selecionados",
                    checked = state.settings.alertsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) requestNotifications(2) {
                            onSetAlerts(true, state.settings.alertThresholds)
                        } else onSetAlerts(false, state.settings.alertThresholds)
                    },
                )
                Text(
                    "Percentuais",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val options = (setOf(50, 75, 80, 90, 100) + state.settings.alertThresholds).sorted()
                    options.forEach { threshold ->
                        FilterChip(
                            selected = threshold in state.settings.alertThresholds,
                            onClick = {
                                val next = if (threshold in state.settings.alertThresholds) {
                                    state.settings.alertThresholds - threshold
                                } else {
                                    state.settings.alertThresholds + threshold
                                }
                                onSetAlerts(state.settings.alertsEnabled, next)
                            },
                            label = { Text("$threshold%") },
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = { showCustomAlert = true },
                        label = { Text("Personalizar") },
                    )
                }
            }
        }

        item {
            SettingsCard("Atualização", Icons.Rounded.Refresh) {
                Text(
                    "Segundo plano",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "O Android pode atrasar a execução para economizar bateria.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(15, 30, 60).forEach { minutes ->
                        FilterChip(
                            selected = state.settings.backgroundIntervalMinutes == minutes,
                            onClick = { onSetBackgroundInterval(minutes) },
                            label = { Text("$minutes min") },
                        )
                    }
                }
            }
        }

        item {
            SettingsCard("Tema", Icons.Rounded.Palette) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSetTheme(mode) }
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = state.settings.themeMode == mode,
                            onClick = { onSetTheme(mode) },
                        )
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> "Seguir sistema"
                                ThemeMode.LIGHT -> "Claro"
                                ThemeMode.DARK -> "Escuro"
                            },
                        )
                    }
                }
            }
        }

        item {
            SettingsCard("MeuGiga", Icons.Rounded.Info) {
                SettingValueRow("Privacidade", "Como seus dados são protegidos", onOpenPrivacy)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SettingValueRow("Sobre o MeuGiga", "Versão, tecnologia e limitações", onOpenAbout)
            }
        }
    }

    if (showPlanEditor) {
        PlanEditorDialog(
            current = state.settings.plan,
            onDismiss = { showPlanEditor = false },
            onSave = onSavePlan,
        )
    }
    if (showCustomAlert) {
        CustomThresholdDialog(
            onDismiss = { showCustomAlert = false },
            onSave = { threshold ->
                onSetAlerts(
                    state.settings.alertsEnabled,
                    state.settings.alertThresholds + threshold,
                )
                showCustomAlert = false
            },
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ToggleSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingValueRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
private fun CustomThresholdDialog(onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val value = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Percentual personalizado") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(3) },
                label = { Text("Percentual") },
                suffix = { Text("%") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = text.isNotEmpty() && (value?.let { it !in 1..100 } ?: true),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(value!!) },
                enabled = value?.let { it in 1..100 } == true,
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private val PLAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
