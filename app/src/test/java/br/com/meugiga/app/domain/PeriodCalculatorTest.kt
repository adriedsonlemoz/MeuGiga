package br.com.meugiga.app.domain

import br.com.meugiga.app.domain.model.PeriodPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PeriodCalculatorTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val now = Instant.parse("2026-08-25T19:30:00Z")
    private val clock = Clock.fixed(now, zone)
    private val planCalculator = PlanCalculator(clock)
    private val calculator = PeriodCalculator(clock)
    private val cycle = planCalculator.currentCycle(15)

    @Test
    fun `hoje termina no instante atual`() {
        val period = calculator.resolve(PeriodPreset.TODAY, cycle)
        assertEquals(clock.millis(), period.endMillis)
        assertEquals("Hoje", period.label)
    }

    @Test
    fun `periodo personalizado inclui todo o ultimo dia sem passar do agora`() {
        val period = calculator.resolve(
            PeriodPreset.CUSTOM,
            cycle,
            LocalDate.of(2026, 8, 15),
            LocalDate.of(2026, 8, 24),
        )
        assertTrue(period.endMillis <= clock.millis())
        assertEquals("15/08/2026 a 24/08/2026", period.label)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `recusa intervalo invertido`() {
        calculator.resolve(
            PeriodPreset.CUSTOM,
            cycle,
            LocalDate.of(2026, 8, 24),
            LocalDate.of(2026, 8, 15),
        )
    }
}

