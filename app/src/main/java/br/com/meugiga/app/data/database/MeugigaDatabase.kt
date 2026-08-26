package br.com.meugiga.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MobileUsageBucketEntity::class,
        UsageQueryEntity::class,
        UsageQueryAppEntity::class,
        AppIdentityEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class MeugigaDatabase : RoomDatabase() {
    abstract fun mobileUsageDao(): MobileUsageDao

    companion object {
        @Volatile
        private var instance: MeugigaDatabase? = null

        fun getInstance(context: Context): MeugigaDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MeugigaDatabase::class.java,
                "meugiga.db",
            )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_mobile_usage_period " +
                        "ON mobile_usage_buckets(startMillis, endMillis)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_mobile_usage_last_seen " +
                        "ON mobile_usage_buckets(lastSeenAtMillis)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS app_identities (
                        uid INTEGER NOT NULL,
                        packageName TEXT,
                        label TEXT NOT NULL,
                        isSystem INTEGER NOT NULL,
                        lastSeenAtMillis INTEGER NOT NULL,
                        PRIMARY KEY(uid)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
