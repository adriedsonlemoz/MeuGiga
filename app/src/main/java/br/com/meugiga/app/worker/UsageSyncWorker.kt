package br.com.meugiga.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.meugiga.app.MeugigaApplication
import br.com.meugiga.app.domain.model.PeriodPreset
import kotlinx.coroutines.flow.first

class UsageSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val application = applicationContext as? MeugigaApplication ?: return Result.failure()
        val container = application.container
        val settings = container.settingsRepository.settings.first()
        val cycle = container.planCalculator.currentCycle(settings.plan)
        val cyclePeriod = container.periodCalculator.resolve(PeriodPreset.CYCLE, cycle)
        val todayPeriod = container.periodCalculator.resolve(PeriodPreset.TODAY, cycle)

        val cycleResult = container.usageRepository.load(
            cyclePeriod,
            refresh = true,
            includeDetails = false,
        )
        val todayResult = container.usageRepository.load(
            todayPeriod,
            refresh = true,
            includeDetails = false,
        )
        val metrics = container.planCalculator.metrics(
            usedBytes = cycleResult.report.totalBytes,
            plan = settings.plan,
            cycle = cycle,
        )

        container.notificationManager.updateContinuous(
            cycleReport = cycleResult.report,
            todayReport = todayResult.report,
            metrics = metrics,
            settings = settings,
        )
        container.notificationManager.evaluateAlerts(cycle, metrics, settings)
        return Result.success()
    }
}
