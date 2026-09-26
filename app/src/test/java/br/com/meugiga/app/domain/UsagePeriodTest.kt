package br.com.meugiga.app.domain

import br.com.meugiga.app.domain.model.UsagePeriod
import br.com.meugiga.app.domain.model.coveredCalendarDays
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class UsagePeriodTest {
    private val zone = ZoneId.of("America/Sao_Paulo")

    private fun millis(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun `ontem encerrado a meia noite conta um dia`() {
        val period = UsagePeriod(
            startMillis = millis(LocalDate.of(2026, 9, 25)),
            endMillis = millis(LocalDate.of(2026, 9, 26)),
            label = "Ontem",
            cacheKey = "yesterday",
        )

        assertEquals(1, period.coveredCalendarDays(zone))
    }

    @Test
    fun `periodo personalizado encerrado conta apenas dias cobertos`() {
        val period = UsagePeriod(
            startMillis = millis(LocalDate.of(2026, 9, 20)),
            endMillis = millis(LocalDate.of(2026, 9, 26)),
            label = "20/09/2026 a 25/09/2026",
            cacheKey = "custom",
        )

        assertEquals(6, period.coveredCalendarDays(zone))
    }

    @Test
    fun `periodo em andamento inclui o dia atual`() {
        val start = millis(LocalDate.of(2026, 9, 20))
        val end = LocalDate.of(2026, 9, 26)
            .atTime(10, 30)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        val period = UsagePeriod(start, end, "7 dias", "seven-days")

        assertEquals(7, period.coveredCalendarDays(zone))
    }
}
