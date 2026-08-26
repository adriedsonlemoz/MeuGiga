package br.com.meugiga.app.domain

import br.com.meugiga.app.domain.model.BillingCycle
import br.com.meugiga.app.domain.model.PlanMetrics
import br.com.meugiga.app.domain.model.PlanSettings
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToLong

class PlanCalculator(
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun currentCycle(
        plan: PlanSettings,
        nowMillis: Long = clock.millis(),
        zoneId: ZoneId = clock.zone,
    ): BillingCycle {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val configuredStart = plan.cycleStartDate
        if (configuredStart != null && !today.isBefore(configuredStart)) {
            val firstRenewal = nextRenewalAfter(configuredStart, plan.renewalDay)
            if (today.isBefore(firstRenewal)) {
                return cycle(configuredStart, firstRenewal, zoneId)
            }
        }
        return currentCycle(plan.renewalDay, nowMillis, zoneId)
    }

    fun currentCycle(
        renewalDay: Int,
        nowMillis: Long = clock.millis(),
        zoneId: ZoneId = clock.zone,
    ): BillingCycle {
        require(renewalDay in 1..31)
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val currentMonthRenewal = renewalDate(YearMonth.from(today), renewalDay)
        val startDate = if (today >= currentMonthRenewal) {
            currentMonthRenewal
        } else {
            renewalDate(YearMonth.from(today).minusMonths(1), renewalDay)
        }
        val endDate = renewalDate(YearMonth.from(startDate).plusMonths(1), renewalDay)

        return cycle(startDate, endDate, zoneId)
    }

    fun metrics(
        usedBytes: Long,
        plan: PlanSettings,
        cycle: BillingCycle,
        nowMillis: Long = clock.millis(),
        zoneId: ZoneId = clock.zone,
    ): PlanMetrics {
        val safeUsed = usedBytes.coerceAtLeast(0)
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val elapsedDays = (ChronoUnit.DAYS.between(cycle.startDate, today) + 1)
            .toInt()
            .coerceAtLeast(1)
        val totalCycleDays = ChronoUnit.DAYS
            .between(cycle.startDate, cycle.endDateExclusive)
            .toInt()
            .coerceAtLeast(1)
        val daysRemaining = ChronoUnit.DAYS
            .between(today, cycle.endDateExclusive)
            .toInt()
            .coerceAtLeast(0)
        val average = safeUsed.toDouble().div(elapsedDays).roundToLong()
        val projection = average.toDouble().times(totalCycleDays).roundToLong()
        val total = plan.totalBytes.takeUnless { plan.unlimited }
        val remaining = total?.let { max(0L, it - safeUsed) }
        val rawPercent = total
            ?.takeIf { it > 0 }
            ?.let { safeUsed.toDouble() / it.toDouble() }
        val availablePerDay = remaining?.let {
            if (daysRemaining > 0) it / daysRemaining else 0L
        }

        return PlanMetrics(
            usedBytes = safeUsed,
            totalBytes = total,
            remainingBytes = remaining,
            percentUsed = rawPercent?.times(100.0),
            progress = rawPercent?.coerceIn(0.0, 1.0)?.toFloat() ?: 0f,
            daysRemaining = daysRemaining,
            elapsedDays = elapsedDays,
            totalCycleDays = totalCycleDays,
            dailyAverageBytes = average,
            availablePerDayBytes = availablePerDay,
            projectedBytes = projection,
            projectedOverageBytes = total?.let { max(0L, projection - it) },
        )
    }

    private fun renewalDate(month: YearMonth, renewalDay: Int): LocalDate =
        month.atDay(renewalDay.coerceAtMost(month.lengthOfMonth()))

    private fun nextRenewalAfter(startDate: LocalDate, renewalDay: Int): LocalDate {
        val startMonth = YearMonth.from(startDate)
        val sameMonth = renewalDate(startMonth, renewalDay)
        return if (sameMonth.isAfter(startDate)) sameMonth
        else renewalDate(startMonth.plusMonths(1), renewalDay)
    }

    private fun cycle(
        startDate: LocalDate,
        endDate: LocalDate,
        zoneId: ZoneId,
    ): BillingCycle = BillingCycle(
        startDate = startDate,
        endDateExclusive = endDate,
        startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
        endMillis = endDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
    )
}
