package br.com.meugiga.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface MobileUsageDao {
    @Upsert
    suspend fun upsertBuckets(buckets: List<MobileUsageBucketEntity>)

    @Upsert
    suspend fun upsertAppIdentities(identities: List<AppIdentityEntity>)

    @Query("SELECT * FROM app_identities WHERE uid IN (:uids)")
    suspend fun appIdentities(uids: List<Int>): List<AppIdentityEntity>

    @Query(
        """
        SELECT * FROM mobile_usage_buckets
        WHERE startMillis >= :startMillis AND endMillis <= :endMillis
        ORDER BY startMillis ASC, endMillis ASC, uid ASC
        """,
    )
    suspend fun bucketsForPeriod(startMillis: Long, endMillis: Long): List<MobileUsageBucketEntity>

    @Query("DELETE FROM mobile_usage_buckets WHERE endMillis < :cutoffMillis")
    suspend fun deleteBucketsOlderThan(cutoffMillis: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putQuery(query: UsageQueryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putQueryApps(apps: List<UsageQueryAppEntity>)

    @Query("DELETE FROM usage_query_apps WHERE cacheKey = :cacheKey")
    suspend fun deleteQueryApps(cacheKey: String)

    @Query("SELECT * FROM usage_queries WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun query(cacheKey: String): UsageQueryEntity?

    @Query("SELECT * FROM usage_query_apps WHERE cacheKey = :cacheKey ORDER BY (rxBytes + txBytes) DESC")
    suspend fun queryApps(cacheKey: String): List<UsageQueryAppEntity>

    @Query("DELETE FROM usage_queries WHERE capturedAtMillis < :cutoffMillis")
    suspend fun deleteQueriesOlderThan(cutoffMillis: Long)

    @Query(
        """
        DELETE FROM usage_query_apps
        WHERE cacheKey NOT IN (SELECT cacheKey FROM usage_queries)
        """,
    )
    suspend fun deleteOrphanedQueryApps()

    @Transaction
    suspend fun replaceCachedReport(
        query: UsageQueryEntity,
        apps: List<UsageQueryAppEntity>,
    ) {
        deleteQueryApps(query.cacheKey)
        putQuery(query)
        if (apps.isNotEmpty()) putQueryApps(apps)
    }

    @Transaction
    suspend fun prune(cutoffMillis: Long) {
        deleteBucketsOlderThan(cutoffMillis)
        deleteQueriesOlderThan(cutoffMillis)
        deleteOrphanedQueryApps()
    }
}
