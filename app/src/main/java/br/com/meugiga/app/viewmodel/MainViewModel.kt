package br.com.meugiga.app.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.meugiga.app.AppContainer
import br.com.meugiga.app.domain.model.AppSettings
import br.com.meugiga.app.domain.model.BillingCycle
import br.com.meugiga.app.domain.model.PeriodPreset
import br.com.meugiga.app.domain.model.PlanMetrics
import br.com.meugiga.app.domain.model.ReportSource
import br.com.meugiga.app.domain.model.ThemeMode
import br.com.meugiga.app.domain.model.UsageLoadError
import br.com.meugiga.app.domain.model.UsagePeriod
import br.com.meugiga.app.domain.model.UsageReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

data class MainUiState(
    val settings: AppSettings = AppSettings(),
    val hasUsageAccess: Boolean = false,
    val cycle: BillingCycle? = null,
    val cycleReport: UsageReport? = null,
    val todayReport: UsageReport? = null,
    val analysisReport: UsageReport? = null,
    val planMetrics: PlanMetrics? = null,
    val selectedPreset: PeriodPreset = PeriodPreset.CYCLE,
    val customStart: LocalDate? = null,
    val customEndInclusive: LocalDate? = null,
    val isRefreshing: Boolean = false,
    val initialized: Boolean = false,
    val lastUpdatedMillis: Long = 0,
    val error: UsageLoadError? = null,
)

class MainViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private val refreshMutex = Mutex()
    private var foregroundJob: Job? = null
    private var lastTimelineRefreshMillis: Long = 0

    init {
        _state.update { it.copy(hasUsageAccess = container.usageAccessController.hasAccess()) }
        viewModelScope.launch {
            var previous: AppSettings? = null
            container.settingsRepository.settings.collect { settings ->
                val shouldReload = previous == null || previous?.plan != settings.plan
                previous = settings
                _state.update { it.copy(settings = settings, initialized = true) }
                if (settings.onboardingComplete && shouldReload) refreshInternal(forceDetails = true)
            }
        }
    }

    fun startForegroundUpdates() {
        if (foregroundJob?.isActive == true) return
        foregroundJob = viewModelScope.launch {
            while (isActive) {
                refreshInternal(forceDetails = false)
                delay(FOREGROUND_REFRESH_MILLIS)
            }
        }
    }

    fun stopForegroundUpdates() {
        foregroundJob?.cancel()
        foregroundJob = null
    }

    fun refreshNow() {
        viewModelScope.launch { refreshInternal(forceDetails = true) }
    }

    fun refreshPermissionState() {
        val granted = container.usageAccessController.hasAccess()
        _state.update { it.copy(hasUsageAccess = granted) }
        if (granted && _state.value.settings.onboardingComplete) refreshNow()
    }

    fun usageAccessSettingsIntent(): Intent = container.usageAccessController.settingsIntent()

    fun selectPeriod(preset: PeriodPreset) {
        if (preset == PeriodPreset.CUSTOM) return
        _state.update { it.copy(selectedPreset = preset) }
        refreshNow()
    }

    fun selectCustomPeriod(start: LocalDate, endInclusive: LocalDate) {
        if (endInclusive.isBefore(start)) return
        _state.update {
            it.copy(
                selectedPreset = PeriodPreset.CUSTOM,
                customStart = start,
                customEndInclusive = endInclusive,
            )
        }
        refreshNow()
    }

    fun savePlan(
        totalBytes: Long?,
        renewalDay: Int,
        unlimited: Boolean,
        cycleStartDate: LocalDate?,
    ) {
        viewModelScope.launch {
            container.settingsRepository.setPlan(
                totalBytes,
                renewalDay,
                unlimited,
                cycleStartDate,
            )
        }
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            container.settingsRepository.completeOnboarding()
        }
    }

    fun setPersistentNotification(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.setPersistentNotification(enabled)
            if (!enabled) container.notificationManager.cancelContinuous()
            else refreshInternal(forceDetails = true)
        }
    }

    fun setAlerts(enabled: Boolean, thresholds: Set<Int>) {
        viewModelScope.launch {
            container.settingsRepository.setAlerts(enabled, thresholds)
        }
    }

    fun setBackgroundInterval(minutes: Int) {
        viewModelScope.launch {
            container.settingsRepository.setBackgroundInterval(minutes)
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { container.settingsRepository.setTheme(mode) }
    }

    private suspend fun refreshInternal(forceDetails: Boolean) = refreshMutex.withLock {
        val current = _state.value
        val settings = current.settings
        val permissionGranted = container.usageAccessController.hasAccess()
        if (!settings.onboardingComplete) {
            _state.update {
                it.copy(
                    hasUsageAccess = permissionGranted,
                    initialized = true,
                    isRefreshing = false,
                )
            }
            return@withLock
        }

        _state.update { it.copy(isRefreshing = true, hasUsageAccess = permissionGranted) }
        try {
            val nowMillis = System.currentTimeMillis()
            val includeDetails = forceDetails ||
                nowMillis - lastTimelineRefreshMillis >= TIMELINE_REFRESH_MILLIS ||
                current.cycleReport == null
            val cycle = container.planCalculator.currentCycle(settings.plan)
            val cyclePeriod = container.periodCalculator.resolve(PeriodPreset.CYCLE, cycle)
            val todayPeriod = container.periodCalculator.resolve(PeriodPreset.TODAY, cycle)
            val analysisPeriod = selectedPeriod(current, cycle)

            val cycleResult = container.usageRepository.load(
                cyclePeriod,
                refresh = permissionGranted,
                includeDetails = includeDetails,
            )
            val todayResult = container.usageRepository.load(
                todayPeriod,
                refresh = permissionGranted,
                includeDetails = includeDetails && current.selectedPreset == PeriodPreset.TODAY,
            )
            val analysisResult = when (current.selectedPreset) {
                PeriodPreset.CYCLE -> cycleResult
                PeriodPreset.TODAY -> todayResult
                else -> container.usageRepository.load(
                    analysisPeriod,
                    refresh = permissionGranted,
                    includeDetails = includeDetails,
                )
            }
            val metrics = container.planCalculator.metrics(
                usedBytes = cycleResult.report.totalBytes,
                plan = settings.plan,
                cycle = cycle,
            )
            val error = sequenceOf(cycleResult.error, todayResult.error, analysisResult.error)
                .filterNotNull()
                .firstOrNull()
            val updatedAt = listOf(
                cycleResult.report.capturedAtMillis,
                todayResult.report.capturedAtMillis,
                analysisResult.report.capturedAtMillis,
            ).max()

            _state.update {
                it.copy(
                    settings = settings,
                    hasUsageAccess = permissionGranted,
                    cycle = cycle,
                    cycleReport = cycleResult.report,
                    todayReport = todayResult.report,
                    analysisReport = analysisResult.report,
                    planMetrics = metrics,
                    isRefreshing = false,
                    initialized = true,
                    lastUpdatedMillis = updatedAt,
                    error = error,
                )
            }
            if (permissionGranted && includeDetails) lastTimelineRefreshMillis = nowMillis

            container.notificationManager.updateContinuous(
                cycleResult.report,
                todayResult.report,
                metrics,
                settings,
            )
            container.notificationManager.evaluateAlerts(cycle, metrics, settings)
        } catch (cancelled: CancellationException) {
            _state.update { it.copy(isRefreshing = false) }
            throw cancelled
        } catch (error: Exception) {
            _state.update {
                it.copy(
                    isRefreshing = false,
                    initialized = true,
                    error = UsageLoadError.Unexpected(
                        error.message ?: "Não foi possível atualizar os dados.",
                    ),
                )
            }
        }
    }

    private fun selectedPeriod(state: MainUiState, cycle: BillingCycle): UsagePeriod =
        container.periodCalculator.resolve(
            preset = state.selectedPreset,
            cycle = cycle,
            customStart = state.customStart,
            customEndInclusive = state.customEndInclusive,
        )

    companion object {
        private const val FOREGROUND_REFRESH_MILLIS = 60_000L
        private const val TIMELINE_REFRESH_MILLIS = 15 * 60_000L

        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(MainViewModel::class.java))
                    return MainViewModel(container) as T
                }
            }
    }
}
