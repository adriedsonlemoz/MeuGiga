package br.com.meugiga.app.data.repository

import br.com.meugiga.app.data.database.MobileUsageBucketEntity
import br.com.meugiga.app.data.database.MobileUsageDao
import br.com.meugiga.app.data.database.UsageQueryAppEntity
import br.com.meugiga.app.data.database.UsageQueryEntity
import br.com.meugiga.app.data.networkstats.MobileStatsRead
import br.com.meugiga.app.data.networkstats.MobileUsageReader
import br.com.meugiga.app.data.networkstats.NetworkStatisticsUnavailableException
import br.com.meugiga.app.data.networkstats.UsageAccessDeniedException
import br.com.meugiga.app.domain.model.AppUsage
import br.com.meugiga.app.domain.model.RawUsageBucket
import br.com.meugiga.app.domain.model.ReportSource
import br.com.meugiga.app.domain.model.TimelineUsage
import br.com.meugiga.app.domain.model.UidUsage
import br.com.meugiga.app.domain.model.UsageLoadError
import br.com.meugiga.app.domain.model.UsageLoadResult
import br.com.meugiga.app.domain.model.UsagePeriod
import br.com.meugiga.app.domain.model.UsageReport
import br.com.meugiga.app.domain.UsageAggregator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration

class UsageRepository(
    private val reader: MobileUsageReader,
    private val dao: MobileUsageDao,
    private val packageCatalog: PackageCatalog,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val refreshMutex = Mutex()
    private var lastPruneAtMillis: Long = 0

    suspend fun load(
        period: UsagePeriod,
        refresh: Boolean = true,
        includeDetails: Boolean = true,
    ): UsageLoadResult = withContext(Dispatchers.Default) {
        refreshMutex.withLock {
            if (!refresh) return@withLock cached(period)
            try {
                val read = reader.read(period, includeDetails)
                persist(read)
                val reportRead = if (includeDetails) read else read.copy(
                    buckets = cachedBuckets(period),
                )
                UsageLoadResult(toReport(reportRead, ReportSource.LIVE))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: UsageAccessDeniedException) {
                cached(period, UsageLoadError.PermissionRequired)
            } catch (_: NetworkStatisticsUnavailableException) {
                cached(period, UsageLoadError.StatisticsUnavailable)
            } catch (error: Exception) {
                cached(
                    period,
                    UsageLoadError.Unexpected(error.message ?: "Falha ao ler as estatísticas."),
                )
            }
        }
    }

    private suspend fun persist(read: MobileStatsRead) {
        val capturedAt = read.capturedAtMillis
        dao.upsertBuckets(read.buckets.map {
            MobileUsageBucketEntity(
                uid = it.uid,
                startMillis = it.startMillis,
                endMillis = it.endMillis,
                rxBytes = it.rxBytes,
                txBytes = it.txBytes,
                lastSeenAtMillis = capturedAt,
            )
        })
        dao.replaceCachedReport(
            query = UsageQueryEntity(
                cacheKey = read.period.cacheKey,
                periodStartMillis = read.period.startMillis,
                periodEndMillis = read.period.endMillis,
                capturedAtMillis = capturedAt,
                deviceRxBytes = read.deviceRxBytes,
                deviceTxBytes = read.deviceTxBytes,
            ),
            apps = read.uidUsage.map {
                UsageQueryAppEntity(
                    cacheKey = read.period.cacheKey,
                    uid = it.uid,
                    rxBytes = it.rxBytes,
                    txBytes = it.txBytes,
                )
            },
        )
        if (capturedAt - lastPruneAtMillis >= PRUNE_INTERVAL_MILLIS) {
            dao.prune(clock.millis() - RETENTION_MILLIS)
            lastPruneAtMillis = capturedAt
        }
    }

    private suspend fun cached(
        period: UsagePeriod,
        error: UsageLoadError? = null,
    ): UsageLoadResult {
        val query = dao.query(period.cacheKey)
        val cachedApps = query?.let { dao.queryApps(it.cacheKey) }.orEmpty()
        val buckets = cachedBuckets(period)
        if (query == null && buckets.isEmpty()) {
            return UsageLoadResult(UsageReport.empty(period), error)
        }

        val uidUsage = if (cachedApps.isNotEmpty()) {
            cachedApps.map { UidUsage(it.uid, it.rxBytes, it.txBytes) }
        } else {
            buckets.groupBy { it.uid }.map { (uid, values) ->
                UidUsage(uid, values.sumOf { it.rxBytes }, values.sumOf { it.txBytes })
            }
        }
        val fallbackRx = uidUsage.sumOf { it.rxBytes }
        val fallbackTx = uidUsage.sumOf { it.txBytes }
        val read = MobileStatsRead(
            period = period,
            deviceRxBytes = query?.deviceRxBytes ?: fallbackRx,
            deviceTxBytes = query?.deviceTxBytes ?: fallbackTx,
            uidUsage = uidUsage,
            buckets = buckets,
            capturedAtMillis = query?.capturedAtMillis ?: 0,
        )
        return UsageLoadResult(toReport(read, ReportSource.CACHE), error)
    }

    private suspend fun cachedBuckets(period: UsagePeriod): List<RawUsageBucket> =
        dao.bucketsForPeriod(period.startMillis, period.endMillis).map {
            RawUsageBucket(it.uid, it.startMillis, it.endMillis, it.rxBytes, it.txBytes)
        }

    private suspend fun toReport(read: MobileStatsRead, source: ReportSource): UsageReport {
        val descriptors = packageCatalog.resolve(read.uidUsage.map { it.uid })
        val total = read.deviceRxBytes + read.deviceTxBytes
        val apps = read.uidUsage
            .asSequence()
            .filter { it.totalBytes > 0 }
            .map { usage ->
                AppUsage(
                    descriptor = descriptors.getValue(usage.uid),
                    rxBytes = usage.rxBytes,
                    txBytes = usage.txBytes,
                    shareOfTotal = if (total > 0) usage.totalBytes.toDouble() / total else 0.0,
                )
            }
            .sortedByDescending { it.totalBytes }
            .toList()

        val timeline = UsageAggregator.timeline(read.buckets)
        val appTimelines = UsageAggregator.timelinesByUid(read.buckets)

        return UsageReport(
            period = read.period,
            totalRxBytes = read.deviceRxBytes,
            totalTxBytes = read.deviceTxBytes,
            apps = apps,
            timeline = timeline,
            appTimelines = appTimelines,
            capturedAtMillis = read.capturedAtMillis,
            source = source,
        )
    }

    companion object {
        private val RETENTION_MILLIS = Duration.ofDays(120).toMillis()
        private val PRUNE_INTERVAL_MILLIS = Duration.ofHours(12).toMillis()
    }
}
