package br.com.meugiga.app.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatters {
    private val ptBr = Locale.forLanguageTag("pt-BR")
    private val day = DateTimeFormatter.ofPattern("dd/MM", ptBr)
    private val fullDate = DateTimeFormatter.ofPattern("dd/MM/yyyy", ptBr)
    private val time = DateTimeFormatter.ofPattern("HH:mm", ptBr)

    fun day(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        day.format(Instant.ofEpochMilli(millis).atZone(zoneId))

    fun fullDate(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        fullDate.format(Instant.ofEpochMilli(millis).atZone(zoneId))

    fun time(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        time.format(Instant.ofEpochMilli(millis).atZone(zoneId))

    fun timelineLabel(
        startMillis: Long,
        endMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        val start = Instant.ofEpochMilli(startMillis).atZone(zoneId)
        val end = Instant.ofEpochMilli(endMillis).atZone(zoneId)
        return if (start.toLocalDate() == end.toLocalDate() && endMillis - startMillis <= 3_600_000L) {
            "${start.hour.toString().padStart(2, '0')}h"
        } else if (start.toLocalDate() == end.toLocalDate()) {
            "${start.hour.toString().padStart(2, '0')}–${end.hour.toString().padStart(2, '0')}h"
        } else {
            "${day.format(start)}–${day.format(end)}"
        }
    }
}
