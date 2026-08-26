package br.com.meugiga.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.meugiga.app.domain.model.PeriodPreset
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodFilterBar(
    selected: PeriodPreset,
    customStart: LocalDate?,
    customEndInclusive: LocalDate?,
    onPresetSelected: (PeriodPreset) -> Unit,
    onCustomSelected: (LocalDate, LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val options = listOf(
        PeriodPreset.TODAY to "Hoje",
        PeriodPreset.YESTERDAY to "Ontem",
        PeriodPreset.SEVEN_DAYS to "7 dias",
        PeriodPreset.THIRTY_DAYS to "30 dias",
        PeriodPreset.CYCLE to "Ciclo",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (preset, label) ->
            FilterChip(
                selected = selected == preset,
                onClick = { onPresetSelected(preset) },
                label = { Text(label) },
            )
        }
        FilterChip(
            selected = selected == PeriodPreset.CUSTOM,
            onClick = { showDatePicker = true },
            label = { Text("Personalizado") },
            leadingIcon = {
                Icon(Icons.Rounded.DateRange, contentDescription = null)
            },
        )
    }

    if (showDatePicker) {
        val today = LocalDate.now()
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = toUtcMillis(customStart ?: today.minusDays(6)),
            initialSelectedEndDateMillis = toUtcMillis(customEndInclusive ?: today),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = pickerState.selectedStartDateMillis?.let(::fromUtcMillis)
                        val end = pickerState.selectedEndDateMillis?.let(::fromUtcMillis)
                        if (start != null && end != null) onCustomSelected(start, end)
                        showDatePicker = false
                    },
                    enabled = pickerState.selectedStartDateMillis != null &&
                        pickerState.selectedEndDateMillis != null,
                ) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                DateRangePicker(
                    state = pickerState,
                    modifier = Modifier.fillMaxWidth().height(500.dp),
                    showModeToggle = false,
                    title = { Text("Escolha o período", Modifier.padding(horizontal = 24.dp)) },
                )
            }
        }
    }
}

private fun toUtcMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun fromUtcMillis(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

