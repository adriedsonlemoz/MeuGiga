package br.com.meugiga.app.domain

import br.com.meugiga.app.domain.model.BillingCycle
import br.com.meugiga.app.domain.model.PeriodPreset
import br.com.meugiga.app.domain.model.UsagePeriod
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class PeriodCalculator(
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val displayFormatter = DateTimeFormatter.ofPattern(
        "dd/MM/yyyy",
        Locale.forLanguageTag("pt-BR"),
    )

    fun resolve(
        preset: PeriodPreset,
        cycle: BillingCycle,
        customStart: LocalDate? = null,
        customEndInclusive: LocalDate? = null,
        nowMillis: Long = clock.millis(),
        zoneId: ZoneId = clock.zone,
    ): UsagePeriod {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val today = now.toLocalDate()
        val start: LocalDate
        val endExclusiveDate: LocalDate
        val label: String
        val endMillis: Long

        when (preset) {
            PeriodPreset.TODAY -> {
                start = today
                endExclusiveDate = today.plusDays(1)
                label = "Hoje"
                endMillis = nowMillis
            }
            PeriodPreset.YESTERDAY -> {
                start = today.minusDays(1)
                endExclusiveDate = today
                label = "Ontem"
                endMillis = endExclusiveDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            }
            PeriodPreset.SEVEN_DAYS -> {
                start = today.minusDays(6)
                endExclusiveDate = today.plusDays(1)
                label = "7 dias"
                endMillis = nowMillis
            }
            PeriodPreset.THIRTY_DAYS -> {
                start = today.minusDays(29)
                endExclusiveDate = today.plusDays(1)
                label = "30 dias"
                endMillis = nowMillis
            }
            PeriodPreset.CYCLE -> {
                start = cycle.startDate
                endExclusiveDate = cycle.endDateExclusive
                label = "Ciclo atual"
                endMillis = minOf(nowMillis, cycle.endMillis)
            }
            PeriodPreset.CUSTOM -> {
                requireNotNull(customStart)
                requireNotNull(customEndInclusive)
                require(!customEndInclusive.isBefore(customStart))
                start = customStart
                endExclusiveDate = customEndInclusive.plusDays(1)
                label = "${displayFormatter.format(start)} a ${displayFormatter.format(customEndInclusive)}"
                endMillis = minOf(
                    nowMillis,
                    endExclusiveDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                )
            }
        }

        val startMillis = start.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val normalizedEnd = endMillis.coerceAtLeast(startMillis)
        val key = when (preset) {
            PeriodPreset.CUSTOM -> "custom:${start}:$endExclusiveDate"
            else -> "${preset.name.lowercase(Locale.ROOT)}:$start"
        }
        return UsagePeriod(startMillis, normalizedEnd, label, key)
    }
}
