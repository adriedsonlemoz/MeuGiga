package br.com.meugiga.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.meugiga.app.MainActivity
import br.com.meugiga.app.R
import br.com.meugiga.app.domain.model.AppSettings
import br.com.meugiga.app.domain.model.BillingCycle
import br.com.meugiga.app.domain.model.PlanMetrics
import br.com.meugiga.app.domain.model.UsageReport
import br.com.meugiga.app.settings.SettingsRepository
import br.com.meugiga.app.utils.ByteFormatter
import kotlin.math.floor

class UsageNotificationManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_STATUS,
                    "Acompanhamento do plano",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Resumo persistente do consumo de dados móveis"
                    setShowBadge(false)
                },
                NotificationChannel(
                    CHANNEL_ALERTS,
                    "Alertas de consumo",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Avisos ao atingir os percentuais escolhidos"
                },
            ),
        )
    }

    fun updateContinuous(
        cycleReport: UsageReport,
        todayReport: UsageReport,
        metrics: PlanMetrics,
        settings: AppSettings,
    ) {
        if (!settings.persistentNotificationEnabled || !canNotify()) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_STATUS_ID)
            return
        }
        val totalLabel = metrics.totalBytes?.let(ByteFormatter::format) ?: "sem limite"
        val remaining = metrics.remainingBytes?.let(ByteFormatter::format) ?: "—"
        val title = "MeuGiga — ${ByteFormatter.format(cycleReport.totalBytes)} / $totalLabel"
        val text = "Hoje: ${ByteFormatter.format(todayReport.totalBytes)}  •  Restante: $remaining"
        val notification = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_STATUS_ID, notification)
        } catch (_: SecurityException) {
            // A permissão pode ser revogada entre a verificação e a publicação.
        }
    }

    suspend fun evaluateAlerts(
        cycle: BillingCycle,
        metrics: PlanMetrics,
        settings: AppSettings,
    ) {
        val percent = metrics.percentUsed ?: return
        if (!settings.alertsEnabled || !canNotify()) return
        val currentSettings = settingsRepository.prepareAlertCycle(cycle.key)
        val reached = currentSettings.alertThresholds
            .filter { percent >= it && it !in currentSettings.sentAlertThresholds }
            .toSet()
        if (reached.isEmpty()) return

        val highest = reached.max()
        val total = metrics.totalBytes ?: return
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Você utilizou $highest% do seu plano")
            .setContentText(
                "${ByteFormatter.format(metrics.usedBytes)} de ${ByteFormatter.format(total)} utilizados.",
            )
            .setContentIntent(contentIntent())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        val posted = try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ALERT_BASE_ID + highest,
                notification,
            )
            true
        } catch (_: SecurityException) {
            false
        }
        if (posted) settingsRepository.markAlertsSent(cycle.key, reached)
    }

    fun cancelContinuous() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_STATUS_ID)
    }

    fun canNotify(): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        private const val CHANNEL_STATUS = "plan_status"
        private const val CHANNEL_ALERTS = "usage_alerts"
        private const val NOTIFICATION_STATUS_ID = 1001
        private const val NOTIFICATION_ALERT_BASE_ID = 2000
    }
}
