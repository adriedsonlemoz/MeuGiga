package br.com.meugiga.app.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mobile_usage_buckets",
    primaryKeys = ["uid", "startMillis", "endMillis"],
    indices = [
        Index(name = "index_mobile_usage_period", value = ["startMillis", "endMillis"]),
        Index(name = "index_mobile_usage_last_seen", value = ["lastSeenAtMillis"]),
    ],
)
data class MobileUsageBucketEntity(
    val uid: Int,
    val startMillis: Long,
    val endMillis: Long,
    val rxBytes: Long,
    val txBytes: Long,
    val lastSeenAtMillis: Long,
)

@Entity(tableName = "usage_queries", primaryKeys = ["cacheKey"])
data class UsageQueryEntity(
    val cacheKey: String,
    val periodStartMillis: Long,
    val periodEndMillis: Long,
    val capturedAtMillis: Long,
    val deviceRxBytes: Long,
    val deviceTxBytes: Long,
)

@Entity(
    tableName = "usage_query_apps",
    primaryKeys = ["cacheKey", "uid"],
)
data class UsageQueryAppEntity(
    val cacheKey: String,
    val uid: Int,
    val rxBytes: Long,
    val txBytes: Long,
)

@Entity(tableName = "app_identities")
data class AppIdentityEntity(
    @PrimaryKey val uid: Int,
    val packageName: String?,
    val label: String,
    val isSystem: Boolean,
    val lastSeenAtMillis: Long,
)
