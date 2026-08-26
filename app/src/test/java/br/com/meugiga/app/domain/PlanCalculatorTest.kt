package br.com.meugiga.app.domain

import br.com.meugiga.app.domain.model.PlanSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PlanCalculatorTest {
    private val zone = ZoneId.of("America/Sao_Paulo")

    @Test
    fun `dia 31 adapta para o ultimo dia de fevereiro`() {
        val now = Instant.parse("2026-02-20T15:00:00Z")
        val calculator = PlanCalculator(Clock.fixed(now, zone))

        val cycle = calculator.currentCycle(31)

        assertEquals("2026-01-31", cycle.startDate.toString())
        assertEquals("2026-02-28", cycle.endDateExclusive.toString())
    }

    @Test
    fun `calcula percentual restante media e previsao`() {
        val now = Instant.parse("2026-08-10T15:00:00Z")
        val calculator = PlanCalculator(Clock.fixed(now, zone))
        val cycle = calculator.currentCycle(1)
        val metrics = calculator.metrics(
            usedBytes = 10_000_000_000L,
            plan = PlanSettings(40_000_000_000L, 1, false),
            cycle = cycle,
        )

        assertEquals(30_000_000_000L, metrics.remainingBytes)
        assertEquals(25.0, metrics.percentUsed!!, 0.001)
        assertEquals(1_000_000_000L, metrics.dailyAverageBytes)
        assertEquals(22, metrics.daysRemaining)
        assertEquals(31, metrics.totalCycleDays)
        assertTrue(metrics.projectedBytes >= 30_000_000_000L)
    }

    @Test
    fun `plano ilimitado nao calcula percentual nem restante`() {
        val now = Instant.parse("2026-08-10T15:00:00Z")
        val calculator = PlanCalculator(Clock.fixed(now, zone))
        val cycle = calculator.currentCycle(1)
        val metrics = calculator.metrics(
            10_000_000L,
            PlanSettings(null, 1, true),
            cycle,
        )

        assertNull(metrics.totalBytes)
        assertNull(metrics.remainingBytes)
        assertNull(metrics.percentUsed)
    }

    @Test
    fun `inicio configurado ancora o primeiro ciclo`() {
        val now = Instant.parse("2026-08-20T15:00:00Z")
        val calculator = PlanCalculator(Clock.fixed(now, zone))
        val plan = PlanSettings(
            totalBytes = 56_000_000_000L,
            renewalDay = 10,
            unlimited = false,
            cycleStartDate = LocalDate.of(2026, 8, 15),
        )

        val cycle = calculator.currentCycle(plan)

        assertEquals("2026-08-15", cycle.startDate.toString())
        assertEquals("2026-09-10", cycle.endDateExclusive.toString())
    }

    @Test
    fun `apos primeira renovacao usa ciclos recorrentes`() {
        val now = Instant.parse("2026-09-15T15:00:00Z")
        val calculator = PlanCalculator(Clock.fixed(now, zone))
        val plan = PlanSettings(
            totalBytes = 56_000_000_000L,
            renewalDay = 10,
            unlimited = false,
            cycleStartDate = LocalDate.of(2026, 8, 15),
        )

        val cycle = calculator.currentCycle(plan)

        assertEquals("2026-09-10", cycle.startDate.toString())
        assertEquals("2026-10-10", cycle.endDateExclusive.toString())
    }
}
