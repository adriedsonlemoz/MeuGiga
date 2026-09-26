package br.com.meugiga.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.meugiga.app.domain.model.PeriodPreset
import br.com.meugiga.app.ui.components.AppUsageRow
import br.com.meugiga.app.ui.components.CompactMetric
import br.com.meugiga.app.ui.components.CompactMetricsGrid
import br.com.meugiga.app.ui.components.EmptyUsage
import br.com.meugiga.app.ui.components.MobileOnlyBadge
import br.com.meugiga.app.ui.components.PeriodFilterBar
import br.com.meugiga.app.ui.components.PermissionRequiredCard
import br.com.meugiga.app.ui.components.UpdateStatus
import br.com.meugiga.app.ui.components.UsageErrorCard
import br.com.meugiga.app.utils.ByteFormatter
import br.com.meugiga.app.viewmodel.MainUiState
import java.time.LocalDate

@Composable
fun AppsScreen(
    state: MainUiState,
    onGrantUsageAccess: () -> Unit,
    onPresetSelected: (PeriodPreset) -> Unit,
    onCustomSelected: (LocalDate, LocalDate) -> Unit,
    onOpenApp: (Int) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val report = state.analysisReport
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val apps = report?.apps.orEmpty()
    val normalizedQuery = searchQuery.trim().lowercase()
    val filteredApps = if (normalizedQuery.isEmpty()) {
        apps
    } else {
        apps.filter { usage ->
            val descriptor = usage.descriptor
            descriptor.label.lowercase().contains(normalizedQuery) ||
                descriptor.packageName?.lowercase()?.contains(normalizedQuery) == true ||
                descriptor.packageNames.any { it.lowercase().contains(normalizedQuery) }
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Por aplicativo", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Do maior consumo para o menor",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            MobileOnlyBadge()
        }
        if (!state.hasUsageAccess) {
            item { PermissionRequiredCard(onGrantUsageAccess) }
        }
        state.error?.let { error -> item { UsageErrorCard(error) } }
        item {
            PeriodFilterBar(
                selected = state.selectedPreset,
                customStart = state.customStart,
                customEndInclusive = state.customEndInclusive,
                onPresetSelected = onPresetSelected,
                onCustomSelected = onCustomSelected,
            )
        }
        item {
            CompactMetricsGrid(
                metrics = listOf(
                    CompactMetric(
                        report?.period?.label ?: "Ciclo atual",
                        ByteFormatter.format(report?.totalBytes ?: 0),
                        Icons.Rounded.DataUsage,
                        supporting = "${report?.apps?.size ?: 0} apps",
                    ),
                    CompactMetric(
                        "Download",
                        ByteFormatter.format(report?.totalRxBytes ?: 0),
                        Icons.Rounded.CloudDownload,
                    ),
                    CompactMetric(
                        "Upload",
                        ByteFormatter.format(report?.totalTxBytes ?: 0),
                        Icons.Rounded.CloudUpload,
                    ),
                ),
            )
        }

        if (apps.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Buscar aplicativo") },
                    placeholder = { Text("Nome ou pacote") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Limpar busca")
                            }
                        }
                    } else {
                        null
                    },
                )
            }
        }

        when {
            apps.isEmpty() -> item { EmptyUsage() }
            filteredApps.isEmpty() -> item {
                Text(
                    "Nenhum aplicativo encontrado para “${searchQuery.trim()}”.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            else -> items(filteredApps, key = { it.uid }) { usage ->
                AppUsageRow(usage, onClick = { onOpenApp(usage.uid) })
            }
        }
        item {
            UpdateStatus(
                lastUpdatedMillis = state.lastUpdatedMillis,
                refreshing = state.isRefreshing,
                onRefresh = onRefresh,
            )
        }
    }
}
