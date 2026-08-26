package br.com.meugiga.app.utils

import br.com.meugiga.app.domain.model.DataUnit
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object ByteFormatter {
    private const val KILO = 1_000L
    private const val MEGA = 1_000_000L
    private const val GIGA = 1_000_000_000L
    private const val TERA = 1_000_000_000_000L
    private val ptBr = Locale.forLanguageTag("pt-BR")

    fun format(bytes: Long, locale: Locale = ptBr): String {
        val safe = bytes.coerceAtLeast(0)
        val (value, suffix) = when {
            safe >= TERA -> safe.toDouble() / TERA to "TB"
            safe >= GIGA -> safe.toDouble() / GIGA to "GB"
            safe >= MEGA -> safe.toDouble() / MEGA to "MB"
            safe >= KILO -> safe.toDouble() / KILO to "KB"
            else -> safe.toDouble() to "B"
        }
        val decimals = when {
            suffix == "B" -> 0
            abs(value) >= 100 -> 0
            abs(value) >= 10 -> 1
            else -> 2
        }
        val formatter = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = decimals
        }
        return "${formatter.format(value)} $suffix"
    }

    fun planValueToBytes(value: Double, unit: DataUnit): Long {
        require(value >= 0 && value.isFinite())
        val multiplier = when (unit) {
            DataUnit.MB -> MEGA
            DataUnit.GB -> GIGA
        }
        return (value * multiplier.toDouble()).toLong()
    }

    fun bytesToPlanValue(bytes: Long, unit: DataUnit): Double = when (unit) {
        DataUnit.MB -> bytes.toDouble() / MEGA
        DataUnit.GB -> bytes.toDouble() / GIGA
    }
}
