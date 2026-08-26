package br.com.meugiga.app

import android.content.Context
import br.com.meugiga.app.data.database.MeugigaDatabase
import br.com.meugiga.app.data.networkstats.AndroidNetworkStatsReader
import br.com.meugiga.app.data.networkstats.UsageAccessController
import br.com.meugiga.app.data.repository.PackageCatalog
import br.com.meugiga.app.data.repository.UsageRepository
import br.com.meugiga.app.domain.PeriodCalculator
import br.com.meugiga.app.domain.PlanCalculator
import br.com.meugiga.app.notification.UsageNotificationManager
import br.com.meugiga.app.settings.SettingsRepository
import br.com.meugiga.app.worker.UsageWorkScheduler

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = MeugigaDatabase.getInstance(appContext)

    val settingsRepository = SettingsRepository(appContext)
    val usageAccessController = UsageAccessController(appContext)
    val planCalculator = PlanCalculator()
    val periodCalculator = PeriodCalculator()
    val usageRepository = UsageRepository(
        reader = AndroidNetworkStatsReader(appContext, usageAccessController),
        dao = database.mobileUsageDao(),
        packageCatalog = PackageCatalog(appContext, database.mobileUsageDao()),
    )
    val workScheduler = UsageWorkScheduler(appContext)
    val notificationManager = UsageNotificationManager(appContext, settingsRepository)
}
