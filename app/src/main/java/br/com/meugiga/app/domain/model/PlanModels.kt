package br.com.meugiga.app.domain.model

import java.time.LocalDate

enum class DataUnit { MB, GB }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class PlanSettings(
    val totalBytes: Long?,
    val renewalDay: Int,
    val unlimited: Boolean,
    val cycleStartDate: LocalDate? = null,
) {
    init {
        require(renewalDay in 1..31)
        require(totalBytes == null || totalBytes >= 0)
    }
}

data class AppSettings(
    val plan: PlanSettings = PlanSettings(
        totalBytes = 40_000_000_000L,
        renewalDay = 1,
        unlimited = false,
    ),
    val onboardingComplete: Boolean = false,
    val persistentNotificationEnabled: Boolean = false,
    val alertsEnabled: Boolean = false,
    val alertThresholds: Set<Int> = setOf(50, 75, 80, 90, 100),
    val backgroundIntervalMinutes: Int = 30,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lastAlertCycleKey: String? = null,
    val sentAlertThresholds: Set<Int> = emptySet(),
    val lastSeenWhatsNewVersionCode: Int = 0,
)

data class BillingCycle(
    val startDate: LocalDate,
    val endDateExclusive: LocalDate,
    val startMillis: Long,
    val endMillis: Long,
) {
    val key: String get() = startDate.toString()
}

data class PlanMetrics(
    val usedBytes: Long,
    val totalBytes: Long?,
    val remainingBytes: Long?,
    val percentUsed: Double?,
    val progress: Float,
    val daysRemaining: Int,
    val elapsedDays: Int,
    val totalCycleDays: Int,
    val dailyAverageBytes: Long,
    val availablePerDayBytes: Long?,
    val projectedBytes: Long,
    val projectedOverageBytes: Long?,
)
