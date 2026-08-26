package br.com.meugiga.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.meugiga.app.domain.model.PeriodPreset
import br.com.meugiga.app.domain.model.AppKind
import br.com.meugiga.app.ui.components.AppIcon
import br.com.meugiga.app.ui.components.CompactMetric
import br.com.meugiga.app.ui.components.CompactMetricsGrid
import br.com.meugiga.app.ui.components.EmptyUsage
import br.com.meugiga.app.ui.components.PeriodFilterBar
import br.com.meugiga.app.ui.components.SectionTitle
import br.com.meugiga.app.ui.components.UsageBarChart
import br.com.meugiga.app.ui.components.dailyChartPoints
import br.com.meugiga.app.ui.components.intervalChartPoints
import br.com.meugiga.app.utils.ByteFormatter
import br.com.meugiga.app.utils.DateFormatters
import br.com.meugiga.app.viewmodel.MainUiState
import java.time.LocalDate
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    uid: Int,
    state: MainUiState,
    onBack: () -> Unit,
    onPresetSelected: (PeriodPreset) -> Unit,
    onCustomSelected: (LocalDate, LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentUsage = state.analysisReport?.app(uid)
    val fallbackUsage = currentUsage ?: state.cycleReport?.app(uid) ?: state.todayReport?.app(uid)
    val descriptor = fallbackUsage?.descriptor
    val timeline = state.analysisReport?.appTimeline(uid).orEmpty()
    val chartPoints = remember(timeline, state.selectedPreset) {
        if (state.selectedPreset in setOf(PeriodPreset.TODAY, PeriodPreset.YESTERDAY)) {
            intervalChartPoints(timeline)
        } else {
            dailyChartPoints(timeline)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        descriptor?.label ?: "Detalhes do aplicativo",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(
                        packageName = descriptor?.packageName,
                        contentDescription = null,
                        size = 68.dp,
                    )
                    Spacer(Modifier.height(1.dp).weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            descriptor?.packageName ?: specialUidDescription(descriptor?.kind),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
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
                            state.analysisReport?.period?.label ?: "Período",
                            ByteFormatter.format(currentUsage?.totalBytes ?: 0),
                            Icons.Rounded.PieChart,
                            "${((currentUsage?.shareOfTotal ?: 0.0) * 100).roundToInt()}% do período",
                        ),
                        CompactMetric(
                            "Download",
                            ByteFormatter.format(currentUsage?.rxBytes ?: 0),
                            Icons.Rounded.CloudDownload,
                        ),
                        CompactMetric(
                            "Upload",
                            ByteFormatter.format(currentUsage?.txBytes ?: 0),
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
                        "Consumo por dia"
                    },
                    subtitle = "Intervalos reais disponibilizados pelo Android",
                )
            }
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    UsageBarChart(chartPoints, Modifier.padding(18.dp))
                }
            }
            item {
                SectionTitle(
                    "Histórico detalhado",
                    subtitle = state.analysisReport?.period?.label,
                )
            }
            if (timeline.isEmpty()) {
                item { EmptyUsage() }
            } else {
                items(timeline, key = { "${it.startMillis}:${it.endMillis}" }) { bucket ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                DateFormatters.timelineLabel(bucket.startMillis, bucket.endMillis),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                ByteFormatter.format(bucket.totalBytes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            "↓ ${ByteFormatter.format(bucket.rxBytes)}   ↑ ${ByteFormatter.format(bucket.txBytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider(Modifier.padding(top = 12.dp))
                    }
                }
            }
        }
    }
}

private fun specialUidDescription(kind: AppKind?): String = when (kind) {
    AppKind.REMOVED -> "Tráfego agrupado de aplicativos desinstalados"
    AppKind.TETHERING -> "Internet compartilhada por roteador ou USB"
    AppKind.AGGREGATE -> "Tráfego não atribuído pelo Android"
    AppKind.SYSTEM -> "Componente do sistema Android"
    AppKind.UNKNOWN -> "Aplicativo não identificado pelo Android"
    else -> "Aplicativo instalado"
}
