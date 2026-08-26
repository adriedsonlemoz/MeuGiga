package br.com.meugiga.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.meugiga.app.domain.model.TimelineUsage
import br.com.meugiga.app.utils.ByteFormatter
import br.com.meugiga.app.utils.DateFormatters
import java.time.Instant
import java.time.ZoneId

data class UsageChartPoint(
    val label: String,
    val rxBytes: Long,
    val txBytes: Long,
) {
    val totalBytes: Long get() = rxBytes + txBytes
}

@Composable
fun UsageBarChart(
    points: List<UsageChartPoint>,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) {
        EmptyUsage(modifier = modifier)
        return
    }
    var selected by remember(points) { mutableIntStateOf(points.lastIndex) }
    val maxValue = points.maxOf { it.totalBytes }.coerceAtLeast(1L)
    val selectedPoint = points[selected.coerceIn(points.indices)]

    Column(modifier.fillMaxWidth()) {
        Text(
            "${selectedPoint.label}  •  ${ByteFormatter.format(selectedPoint.totalBytes)}",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Download ${ByteFormatter.format(selectedPoint.rxBytes)}  •  Upload ${ByteFormatter.format(selectedPoint.txBytes)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            points.forEachIndexed { index, point ->
                val targetHeight = (120f * point.totalBytes / maxValue)
                    .coerceAtLeast(if (point.totalBytes > 0) 4f else 1f).dp
                val height by animateDpAsState(targetHeight, label = "usage-bar")
                val rxRatio = if (point.totalBytes > 0) point.rxBytes.toFloat() / point.totalBytes else 0f
                Column(
                    modifier = Modifier
                        .width(46.dp)
                        .semantics {
                            contentDescription = "${point.label}: ${ByteFormatter.format(point.totalBytes)}"
                        }
                        .clickable { selected = index },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier
                            .width(28.dp)
                            .height(height)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(MaterialTheme.colorScheme.tertiary),
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(height * rxRatio)
                                .background(
                                    if (index == selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                                ),
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        point.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (index == selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        ChartLegend()
    }
}

@Composable
private fun ChartLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        LegendItem("Download", MaterialTheme.colorScheme.primary)
        LegendItem("Upload", MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).background(color, RoundedCornerShape(3.dp)))
        Text(label, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

fun intervalChartPoints(timeline: List<TimelineUsage>): List<UsageChartPoint> = timeline.map {
    UsageChartPoint(
        label = DateFormatters.timelineLabel(it.startMillis, it.endMillis),
        rxBytes = it.rxBytes,
        txBytes = it.txBytes,
    )
}

fun dailyChartPoints(
    timeline: List<TimelineUsage>,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<UsageChartPoint> = timeline
    .groupBy { Instant.ofEpochMilli(it.startMillis).atZone(zoneId).toLocalDate() }
    .toSortedMap()
    .map { (date, values) ->
        UsageChartPoint(
            label = "${date.dayOfMonth.toString().padStart(2, '0')}/${date.monthValue.toString().padStart(2, '0')}",
            rxBytes = values.sumOf { it.rxBytes },
            txBytes = values.sumOf { it.txBytes },
        )
    }

