package br.com.meugiga.app.utils

import br.com.meugiga.app.domain.model.DataUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class ByteFormatterTest {
    @Test
    fun `formata bytes automaticamente em pt-BR`() {
        assertEquals("850 B", ByteFormatter.format(850))
        assertEquals("420 KB", ByteFormatter.format(420_000))
        assertEquals("185 MB", ByteFormatter.format(185_000_000))
        assertEquals("1,42 GB", ByteFormatter.format(1_420_000_000))
    }

    @Test
    fun `converte franquia decimal de MB e GB`() {
        assertEquals(500_000_000L, ByteFormatter.planValueToBytes(500.0, DataUnit.MB))
        assertEquals(40_000_000_000L, ByteFormatter.planValueToBytes(40.0, DataUnit.GB))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `recusa plano negativo`() {
        ByteFormatter.planValueToBytes(-1.0, DataUnit.GB)
    }
}

