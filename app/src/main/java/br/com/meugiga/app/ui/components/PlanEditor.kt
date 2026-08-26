package br.com.meugiga.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.meugiga.app.domain.model.DataUnit
import br.com.meugiga.app.domain.model.PlanSettings
import br.com.meugiga.app.utils.ByteFormatter
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class PlanDraft(
    val amount: String = "40",
    val unit: DataUnit = DataUnit.GB,
    val renewalDay: String = "1",
    val unlimited: Boolean = false,
    val cycleStartDate: LocalDate? = null,
) {
    val parsedAmount: Double? get() = amount.trim().replace(',', '.').toDoubleOrNull()
    val parsedDay: Int? get() = renewalDay.trim().toIntOrNull()
    val valid: Boolean get() = parsedDay?.let { it in 1..31 } == true &&
        (cycleStartDate == null || !cycleStartDate.isAfter(LocalDate.now())) &&
        (unlimited || (parsedAmount != null && parsedAmount!! > 0))
    val totalBytes: Long? get() = if (unlimited) null else parsedAmount?.let {
        ByteFormatter.planValueToBytes(it, unit)
    }
}

fun planDraftFrom(settings: PlanSettings): PlanDraft {
    val bytes = settings.totalBytes ?: 40_000_000_000L
    val unit = if (bytes >= 1_000_000_000L) DataUnit.GB else DataUnit.MB
    val numeric = ByteFormatter.bytesToPlanValue(bytes, unit)
    val text = BigDecimal.valueOf(numeric).stripTrailingZeros().toPlainString()
    return PlanDraft(
        amount = text,
        unit = unit,
        renewalDay = settings.renewalDay.toString(),
        unlimited = settings.unlimited,
        cycleStartDate = settings.cycleStartDate,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanFields(
    draft: PlanDraft,
    onChange: (PlanDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Plano sem limite definido", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Acompanhar sem calcular restante",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = draft.unlimited,
                onCheckedChange = { onChange(draft.copy(unlimited = it)) },
            )
        }
        OutlinedTextField(
            value = draft.amount,
            onValueChange = { onChange(draft.copy(amount = it.filter(::isPlanCharacter))) },
            label = { Text("Quantidade de dados") },
            suffix = { Text(draft.unit.name) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            enabled = !draft.unlimited,
            isError = !draft.unlimited && (draft.parsedAmount == null || draft.parsedAmount!! <= 0),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DataUnit.entries.forEach { unit ->
                FilterChip(
                    selected = draft.unit == unit,
                    onClick = { onChange(draft.copy(unit = unit)) },
                    label = { Text(unit.name) },
                    enabled = !draft.unlimited,
                )
            }
        }
        OutlinedTextField(
            value = draft.renewalDay,
            onValueChange = { onChange(draft.copy(renewalDay = it.filter(Char::isDigit).take(2))) },
            label = { Text("Dia de renovação") },
            supportingText = { Text("Use um dia de 1 a 31") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = draft.parsedDay?.let { it !in 1..31 } ?: true,
            modifier = Modifier.fillMaxWidth(),
        )
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Início do ciclo", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Text(
                        draft.cycleStartDate?.format(DATE_FORMATTER)
                            ?: "Definir data",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                if (draft.cycleStartDate != null) {
                    TextButton(onClick = { onChange(draft.copy(cycleStartDate = null)) }) {
                        Text("Automático")
                    }
                }
            }
            Text(
                if (draft.cycleStartDate == null) {
                    "Calculado automaticamente pelo dia de renovação"
                } else {
                    "Primeiro dia considerado no ciclo atual"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showStartDatePicker) {
        val initialMillis = draft.cycleStartDate
            ?.atStartOfDay(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli()
        val pickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val selected = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            if (!selected.isAfter(LocalDate.now())) {
                                onChange(draft.copy(cycleStartDate = selected))
                            }
                        }
                        showStartDatePicker = false
                    },
                    enabled = pickerState.selectedDateMillis?.let { millis ->
                        !Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            .isAfter(LocalDate.now())
                    } == true,
                ) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun PlanEditorDialog(
    current: PlanSettings,
    onDismiss: () -> Unit,
    onSave: (
        totalBytes: Long?,
        renewalDay: Int,
        unlimited: Boolean,
        cycleStartDate: LocalDate?,
    ) -> Unit,
) {
    var draft by remember(current) { mutableStateOf(planDraftFrom(current)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plano de internet") },
        text = { PlanFields(draft, { draft = it }) },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        draft.totalBytes,
                        draft.parsedDay!!,
                        draft.unlimited,
                        draft.cycleStartDate,
                    )
                    onDismiss()
                },
                enabled = draft.valid,
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private fun isPlanCharacter(character: Char): Boolean =
    character.isDigit() || character == ',' || character == '.'

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
