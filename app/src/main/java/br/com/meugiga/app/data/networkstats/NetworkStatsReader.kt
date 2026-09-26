package br.com.meugiga.app.data.networkstats

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import br.com.meugiga.app.domain.model.RawUsageBucket
import br.com.meugiga.app.domain.model.UidUsage
import br.com.meugiga.app.domain.model.UsagePeriod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MobileStatsRead(
    val period: UsagePeriod,
    val deviceRxBytes: Long,
    val deviceTxBytes: Long,
    val uidUsage: List<UidUsage>,
    val buckets: List<RawUsageBucket>,
    val capturedAtMillis: Long,
)

class UsageAccessDeniedException(cause: Throwable? = null) : Exception(cause)
class NetworkStatisticsUnavailableException(cause: Throwable? = null) : Exception(cause)

interface MobileUsageReader {
    suspend fun read(period: UsagePeriod, includeDetails: Boolean = true): MobileStatsRead
}

@Suppress("DEPRECATION") // A API pública do NetworkStats usa TYPE_MOBILE neste overload.
class AndroidNetworkStatsReader(
    context: Context,
    private val accessController: UsageAccessController,
    private val manager: NetworkStatsManager = context.getSystemService(NetworkStatsManager::class.java),
) : MobileUsageReader {
    override suspend fun read(
        period: UsagePeriod,
        includeDetails: Boolean,
    ): MobileStatsRead = withContext(Dispatchers.IO) {
        if (!accessController.hasAccess()) throw UsageAccessDeniedException()
        try {
            val uidUsage = readUidSummary(period)
            val buckets = if (includeDetails) readDetails(period) else emptyList()
            val device = manager.querySummaryForDevice(
                ConnectivityManager.TYPE_MOBILE,
                null,
                period.startMillis,
                period.endMillis,
            )
            val fallbackRx = uidUsage.sumOf { it.rxBytes }
            val fallbackTx = uidUsage.sumOf { it.txBytes }
            MobileStatsRead(
                period = period,
                deviceRxBytes = device?.rxBytes?.coerceAtLeast(0) ?: fallbackRx,
                deviceTxBytes = device?.txBytes?.coerceAtLeast(0) ?: fallbackTx,
                uidUsage = uidUsage,
                buckets = buckets,
                capturedAtMillis = System.currentTimeMillis(),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (security: SecurityException) {
            throw UsageAccessDeniedException(security)
        } catch (error: RuntimeException) {
            throw NetworkStatisticsUnavailableException(error)
        }
    }

    private fun readUidSummary(period: UsagePeriod): List<UidUsage> {
        val totals = mutableMapOf<Int, LongArray>()
        val stats = manager.querySummary(
            ConnectivityManager.TYPE_MOBILE,
            null,
            period.startMillis,
            period.endMillis,
        ) ?: throw NetworkStatisticsUnavailableException()
        stats.use {
            val bucket = NetworkStats.Bucket()
            while (it.hasNextBucket()) {
                it.getNextBucket(bucket)
                val total = totals.getOrPut(bucket.uid) { longArrayOf(0L, 0L) }
                total[0] += bucket.rxBytes.coerceAtLeast(0)
                total[1] += bucket.txBytes.coerceAtLeast(0)
            }
        }
        return totals.map { (uid, bytes) -> UidUsage(uid, bytes[0], bytes[1]) }
    }

    private fun readDetails(period: UsagePeriod): List<RawUsageBucket> {
        data class BucketKey(val uid: Int, val start: Long, val end: Long)

        val totals = linkedMapOf<BucketKey, LongArray>()
        val stats = manager.queryDetails(
            ConnectivityManager.TYPE_MOBILE,
            null,
            period.startMillis,
            period.endMillis,
        ) ?: throw NetworkStatisticsUnavailableException()
        stats.use {
            val bucket = NetworkStats.Bucket()
            while (it.hasNextBucket()) {
                it.getNextBucket(bucket)
                val key = BucketKey(bucket.uid, bucket.startTimeStamp, bucket.endTimeStamp)
                val total = totals.getOrPut(key) { longArrayOf(0L, 0L) }
                total[0] += bucket.rxBytes.coerceAtLeast(0)
                total[1] += bucket.txBytes.coerceAtLeast(0)
            }
        }
        return totals.map { (key, bytes) ->
            RawUsageBucket(
                uid = key.uid,
                startMillis = key.start,
                endMillis = key.end,
                rxBytes = bytes[0],
                txBytes = bytes[1],
            )
        }
    }
}
