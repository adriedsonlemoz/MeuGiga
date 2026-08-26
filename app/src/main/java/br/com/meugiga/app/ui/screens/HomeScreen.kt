package br.com.meugiga.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.meugiga.app.domain.model.AppUsage
import br.com.meugiga.app.ui.components.AppUsageRow
import br.com.meugiga.app.ui.components.CompactMetric
import br.com.meugiga.app.ui.components.CompactMetricsGrid
import br.com.meugiga.app.ui.components.MobileOnlyBadge
import br.com.meugiga.app.ui.components.PermissionRequiredCard
import br.com.meugiga.app.ui.components.SectionTitle
import br.com.meugiga.app.ui.components.UpdateStatus
import br.com.meugiga.app.ui.components.UsageErrorCard
import br.com.meugiga.app.ui.components.UsageBarChart
import br.com.meugiga.app.ui.components.dailyChartPoints
import br.com.meugiga.app.utils.ByteFormatter
import br.com.meugiga.app.viewmodel.MainUiState
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    state: MainUiState,
    onGrantUsageAccess: () -> Unit,
    onRefresh: () -> Unit,
    onOpenApp: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val report = state.cycleReport
    val today = state.todayReport
    val metrics = state.planMetrics
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
    }
    val cycleStartLabel = state.cycle?.startDate?.format(dateFormatter) ?: "—"
    val cycleEndLabel = state.cycle?.endDateExclusive?.minusDays(1)?.format(dateFormatter) ?: "—"
    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text("MeuGiga", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Seu consumo de dados, sem complicação.",
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
            PlanHero(
                usedBytes = report?.totalBytes ?: 0,
                totalBytes = metrics?.totalBytes,
                remainingBytes = metrics?.remainingBytes,
                percentUsed = metrics?.percentUsed,
                progress = metrics?.progress ?: 0f,
                daysRemaining = metrics?.daysRemaining ?: 0,
                periodLabel = "$cycleStartLabel até $cycleEndLabel",
                renewalLabel = state.cycle?.endDateExclusive?.format(dateFormatter) ?: "—",
            )
        }

        item {
            CompactMetricsGrid(
                metrics = listOf(
                    CompactMetric(
                        "Hoje",
                        ByteFormatter.format(today?.totalBytes ?: 0),
                        Icons.Rounded.CalendarMonth,
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
                    CompactMetric(
                        "Média diária",
                        ByteFormatter.format(metrics?.dailyAverageBytes ?: 0),
                        Icons.Rounded.Speed,
                    ),
                    CompactMetric(
                        "Disponível/dia",
                        metrics?.availablePerDayBytes?.let(ByteFormatter::format) ?: "—",
                        Icons.Rounded.EventAvailable,
                    ),
                    CompactMetric(
                        "Previsão",
                        ByteFormatter.format(metrics?.projectedBytes ?: 0),
                        Icons.Rounded.DataUsage,
                        supporting = metrics?.projectedOverageBytes
                            ?.takeIf { it > 0 }
                            ?.let { "+${ByteFormatter.format(it)}" }
                            ?: "até renovar",
                    ),
                ),
            )
        }

        item {
            SectionTitle(
                "Evolução do ciclo",
                subtitle = "Download e upload conforme os intervalos fornecidos pelo Android",
            )
        }
        item {
            val chartPoints = remember(report?.timeline) {
                dailyChartPoints(report?.timeline.orEmpty())
            }
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                UsageBarChart(
                    points = chartPoints,
                    modifier = Modifier.padding(18.dp),
                )
            }
        }

        val topApps = report?.apps.orEmpty().take(3)
        item { SectionTitle("Mais usados", subtitle = "Aplicativos no ciclo atual") }
        items(topApps, key = { it.uid }) { usage ->
            AppUsageRow(usage, onClick = { onOpenApp(usage.uid) })
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

@Composable
private fun PlanHero(
    usedBytes: Long,
    totalBytes: Long?,
    remainingBytes: Long?,
    percentUsed: Double?,
    progress: Float,
    daysRemaining: Int,
    periodLabel: String,
    renewalLabel: String,
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < .5f
    val gradient = if (darkTheme) {
        listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
        )
    } else {
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    }
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        gradient,
                    ),
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text("Ciclo atual", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = .82f))
            Text(
                periodLabel,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = .78f),
            )
            Text(
                "${ByteFormatter.format(usedBytes)} usados",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
            )
            Text(
                totalBytes?.let { "de ${ByteFormatter.format(it)}  •  ${(percentUsed ?: 0.0).roundToInt()}% do plano" }
                    ?: "Plano sem limite definido",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = .9f),
            )
            if (totalBytes != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(9.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = .24f),
                )
            }
            Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Restante", color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        remainingBytes?.let(ByteFormatter::format) ?: "—",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column {
                    Text("Renovação", color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        renewalLabel,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (daysRemaining == 1) "1 dia restante" else "$daysRemaining dias restantes",
                        color = Color.White.copy(alpha = .75f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
