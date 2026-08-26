package br.com.meugiga.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.meugiga.app.domain.model.PeriodPreset
import br.com.meugiga.app.ui.components.MobileOnlyBadge
import br.com.meugiga.app.ui.components.CompactMetric
import br.com.meugiga.app.ui.components.CompactMetricsGrid
import br.com.meugiga.app.ui.components.PeriodFilterBar
import br.com.meugiga.app.ui.components.PermissionRequiredCard
import br.com.meugiga.app.ui.components.SectionTitle
import br.com.meugiga.app.ui.components.UpdateStatus
import br.com.meugiga.app.ui.components.UsageBarChart
import br.com.meugiga.app.ui.components.UsageErrorCard
import br.com.meugiga.app.ui.components.dailyChartPoints
import br.com.meugiga.app.ui.components.intervalChartPoints
import br.com.meugiga.app.utils.ByteFormatter
import br.com.meugiga.app.viewmodel.MainUiState
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun HistoryScreen(
    state: MainUiState,
    onGrantUsageAccess: () -> Unit,
    onPresetSelected: (PeriodPreset) -> Unit,
    onCustomSelected: (LocalDate, LocalDate) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val report = state.analysisReport
    val chartPoints = remember(report?.timeline, state.selectedPreset) {
        if (state.selectedPreset in setOf(PeriodPreset.TODAY, PeriodPreset.YESTERDAY)) {
            intervalChartPoints(report?.timeline.orEmpty())
        } else {
            dailyChartPoints(report?.timeline.orEmpty())
        }
    }
    val dayCount = report?.period?.let {
        val durationDays = ChronoUnit.DAYS.between(
            java.time.Instant.ofEpochMilli(it.startMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
            java.time.Instant.ofEpochMilli(it.endMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
        ).toInt() + 1
        durationDays.coerceAtLeast(1)
    } ?: 1

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("Histórico", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Compare seu consumo ao longo do tempo",
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
                        report?.period?.label ?: "Período",
                        ByteFormatter.format(report?.totalBytes ?: 0),
                        Icons.Rounded.Speed,
                        supporting = "${ByteFormatter.format((report?.totalBytes ?: 0) / dayCount)}/dia",
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
        item {
            SectionTitle(
                if (state.selectedPreset in setOf(PeriodPreset.TODAY, PeriodPreset.YESTERDAY)) {
                    "Consumo por intervalo"
                } else {
                    "Consumo diário"
                },
                subtitle = "A granularidade respeita os buckets disponibilizados pelo Android",
            )
        }
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                UsageBarChart(chartPoints, Modifier.padding(18.dp))
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
