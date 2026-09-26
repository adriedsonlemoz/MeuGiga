package br.com.meugiga.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.meugiga.app.domain.model.AppSettings
import br.com.meugiga.app.domain.model.PlanSettings
import br.com.meugiga.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.time.LocalDate

private val Context.meugigaSettings: DataStore<Preferences> by preferencesDataStore("meugiga_settings")

class SettingsRepository(context: Context) {
    private val dataStore = context.meugigaSettings

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map(::toSettings)

    suspend fun setPlan(
        totalBytes: Long?,
        renewalDay: Int,
        unlimited: Boolean,
        cycleStartDate: LocalDate?,
    ) {
        require(renewalDay in 1..31)
        require(totalBytes == null || totalBytes >= 0)
        dataStore.edit { preferences ->
            if (totalBytes == null) preferences.remove(Keys.PLAN_BYTES)
            else preferences[Keys.PLAN_BYTES] = totalBytes
            preferences[Keys.RENEWAL_DAY] = renewalDay
            preferences[Keys.UNLIMITED] = unlimited
            if (cycleStartDate == null) preferences.remove(Keys.CYCLE_START_EPOCH_DAY)
            else preferences[Keys.CYCLE_START_EPOCH_DAY] = cycleStartDate.toEpochDay()
        }
    }

    suspend fun completeOnboarding() {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = true }
    }

    suspend fun setPersistentNotification(enabled: Boolean) {
        dataStore.edit { it[Keys.PERSISTENT_NOTIFICATION] = enabled }
    }

    suspend fun setAlerts(enabled: Boolean, thresholds: Set<Int>) {
        val valid = thresholds.filter { it in 1..100 }.toSortedSet()
        dataStore.edit {
            it[Keys.ALERTS_ENABLED] = enabled
            it[Keys.ALERT_THRESHOLDS] = valid.joinToString(",")
        }
    }

    suspend fun setBackgroundInterval(minutes: Int) {
        require(minutes in setOf(15, 30, 60))
        dataStore.edit { it[Keys.BACKGROUND_INTERVAL] = minutes }
    }

    suspend fun setTheme(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun markWhatsNewSeen(versionCode: Int) {
        require(versionCode > 0)
        dataStore.edit { preferences ->
            val previous = preferences[Keys.LAST_SEEN_WHATS_NEW_VERSION_CODE] ?: 0
            if (versionCode > previous) {
                preferences[Keys.LAST_SEEN_WHATS_NEW_VERSION_CODE] = versionCode
            }
        }
    }

    suspend fun prepareAlertCycle(cycleKey: String): AppSettings {
        dataStore.edit { preferences ->
            if (preferences[Keys.LAST_ALERT_CYCLE] != cycleKey) {
                preferences[Keys.LAST_ALERT_CYCLE] = cycleKey
                preferences[Keys.SENT_THRESHOLDS] = ""
            }
        }
        return settings.first()
    }

    suspend fun markAlertsSent(cycleKey: String, thresholds: Set<Int>) {
        dataStore.edit { preferences ->
            val existing = if (preferences[Keys.LAST_ALERT_CYCLE] == cycleKey) {
                parseThresholds(preferences[Keys.SENT_THRESHOLDS])
            } else {
                emptySet()
            }
            preferences[Keys.LAST_ALERT_CYCLE] = cycleKey
            preferences[Keys.SENT_THRESHOLDS] = (existing + thresholds).sorted().joinToString(",")
        }
    }

    private fun toSettings(preferences: Preferences): AppSettings {
        val unlimited = preferences[Keys.UNLIMITED] ?: false
        return AppSettings(
            plan = PlanSettings(
                totalBytes = preferences[Keys.PLAN_BYTES] ?: 40_000_000_000L,
                renewalDay = (preferences[Keys.RENEWAL_DAY] ?: 1).coerceIn(1, 31),
                unlimited = unlimited,
                cycleStartDate = preferences[Keys.CYCLE_START_EPOCH_DAY]?.let(LocalDate::ofEpochDay),
            ),
            onboardingComplete = preferences[Keys.ONBOARDING_COMPLETE] ?: false,
            persistentNotificationEnabled = preferences[Keys.PERSISTENT_NOTIFICATION] ?: false,
            alertsEnabled = preferences[Keys.ALERTS_ENABLED] ?: false,
            alertThresholds = parseThresholds(preferences[Keys.ALERT_THRESHOLDS])
                .ifEmpty { setOf(50, 75, 80, 90, 100) },
            backgroundIntervalMinutes = (preferences[Keys.BACKGROUND_INTERVAL] ?: 30)
                .takeIf { it in setOf(15, 30, 60) } ?: 30,
            themeMode = runCatching {
                ThemeMode.valueOf(preferences[Keys.THEME] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            lastAlertCycleKey = preferences[Keys.LAST_ALERT_CYCLE],
            sentAlertThresholds = parseThresholds(preferences[Keys.SENT_THRESHOLDS]),
            lastSeenWhatsNewVersionCode = preferences[Keys.LAST_SEEN_WHATS_NEW_VERSION_CODE] ?: 0,
        )
    }

    private fun parseThresholds(raw: String?): Set<Int> = raw
        .orEmpty()
        .split(',')
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it in 1..100 }
        .toSet()

    private object Keys {
        val PLAN_BYTES = longPreferencesKey("plan_bytes")
        val RENEWAL_DAY = intPreferencesKey("renewal_day")
        val UNLIMITED = booleanPreferencesKey("unlimited")
        val CYCLE_START_EPOCH_DAY = longPreferencesKey("cycle_start_epoch_day")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val PERSISTENT_NOTIFICATION = booleanPreferencesKey("persistent_notification")
        val ALERTS_ENABLED = booleanPreferencesKey("alerts_enabled")
        val ALERT_THRESHOLDS = stringPreferencesKey("alert_thresholds")
        val BACKGROUND_INTERVAL = intPreferencesKey("background_interval")
        val THEME = stringPreferencesKey("theme")
        val LAST_ALERT_CYCLE = stringPreferencesKey("last_alert_cycle")
        val SENT_THRESHOLDS = stringPreferencesKey("sent_alert_thresholds")
        val LAST_SEEN_WHATS_NEW_VERSION_CODE = intPreferencesKey("last_seen_whats_new_version_code")
    }
}
