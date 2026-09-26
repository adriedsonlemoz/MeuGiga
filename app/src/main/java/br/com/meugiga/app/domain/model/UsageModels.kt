package br.com.meugiga.app.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class UsagePeriod(
    val startMillis: Long,
    val endMillis: Long,
    val label: String,
    val cacheKey: String,
) {
    init {
        require(endMillis >= startMillis) { "O fim do período deve ser posterior ao início." }
    }
}

fun UsagePeriod.coveredCalendarDays(zoneId: ZoneId = ZoneId.systemDefault()): Int {
    if (endMillis <= startMillis) return 1

    val startDate = Instant.ofEpochMilli(startMillis)
        .atZone(zoneId)
        .toLocalDate()
    val lastIncludedDate = Instant.ofEpochMilli(endMillis - 1L)
        .atZone(zoneId)
        .toLocalDate()

    return (ChronoUnit.DAYS.between(startDate, lastIncludedDate) + 1L)
        .coerceAtLeast(1L)
        .toInt()
}

enum class PeriodPreset {
    TODAY,
    YESTERDAY,
    SEVEN_DAYS,
    THIRTY_DAYS,
    CYCLE,
    CUSTOM,
}

data class RawUsageBucket(
    val uid: Int,
    val startMillis: Long,
    val endMillis: Long,
    val rxBytes: Long,
    val txBytes: Long,
) {
    val totalBytes: Long get() = rxBytes + txBytes
}

data class UidUsage(
    val uid: Int,
    val rxBytes: Long,
    val txBytes: Long,
) {
    val totalBytes: Long get() = rxBytes + txBytes
}

enum class AppKind {
    APPLICATION,
    SYSTEM,
    REMOVED,
    TETHERING,
    AGGREGATE,
    UNKNOWN,
}

data class AppDescriptor(
    val uid: Int,
    val packageName: String?,
    val label: String,
    val packageNames: List<String> = emptyList(),
    val isSystem: Boolean = false,
    val isRemoved: Boolean = false,
    val kind: AppKind = AppKind.APPLICATION,
)

data class AppUsage(
    val descriptor: AppDescriptor,
    val rxBytes: Long,
    val txBytes: Long,
    val shareOfTotal: Double,
) {
    val uid: Int get() = descriptor.uid
    val totalBytes: Long get() = rxBytes + txBytes
}

data class TimelineUsage(
    val startMillis: Long,
    val endMillis: Long,
    val rxBytes: Long,
    val txBytes: Long,
) {
    val totalBytes: Long get() = rxBytes + txBytes
}

enum class ReportSource {
    LIVE,
    CACHE,
    EMPTY,
}

data class UsageReport(
    val period: UsagePeriod,
    val totalRxBytes: Long,
    val totalTxBytes: Long,
    val apps: List<AppUsage>,
    val timeline: List<TimelineUsage>,
    val appTimelines: Map<Int, List<TimelineUsage>>,
    val capturedAtMillis: Long,
    val source: ReportSource,
) {
    val totalBytes: Long get() = totalRxBytes + totalTxBytes

    fun app(uid: Int): AppUsage? = apps.firstOrNull { it.uid == uid }
    fun appTimeline(uid: Int): List<TimelineUsage> = appTimelines[uid].orEmpty()

    companion object {
        fun empty(period: UsagePeriod): UsageReport = UsageReport(
            period = period,
            totalRxBytes = 0,
            totalTxBytes = 0,
            apps = emptyList(),
            timeline = emptyList(),
            appTimelines = emptyMap(),
            capturedAtMillis = 0,
            source = ReportSource.EMPTY,
        )
    }
}

sealed interface UsageLoadError {
    data object PermissionRequired : UsageLoadError
    data object StatisticsUnavailable : UsageLoadError
    data class Unexpected(val message: String) : UsageLoadError
}

data class UsageLoadResult(
    val report: UsageReport,
    val error: UsageLoadError? = null,
)
