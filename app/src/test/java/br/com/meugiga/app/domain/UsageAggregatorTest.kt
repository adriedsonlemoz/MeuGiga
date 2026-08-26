package br.com.meugiga.app.domain

import br.com.meugiga.app.domain.model.RawUsageBucket
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageAggregatorTest {
    @Test
    fun `preserva download e upload ao combinar UIDs no mesmo intervalo`() {
        val buckets = listOf(
            RawUsageBucket(10001, 1_000L, 2_000L, 100L, 20L),
            RawUsageBucket(10002, 1_000L, 2_000L, 300L, 40L),
        )

        val timeline = UsageAggregator.timeline(buckets)

        assertEquals(1, timeline.size)
        assertEquals(400L, timeline.single().rxBytes)
        assertEquals(60L, timeline.single().txBytes)
        assertEquals(460L, timeline.single().totalBytes)
    }

    @Test
    fun `mantem historico separado por aplicativo`() {
        val buckets = listOf(
            RawUsageBucket(10001, 1_000L, 2_000L, 100L, 20L),
            RawUsageBucket(10002, 1_000L, 2_000L, 300L, 40L),
        )

        val byUid = UsageAggregator.timelinesByUid(buckets)

        assertEquals(120L, byUid.getValue(10001).single().totalBytes)
        assertEquals(340L, byUid.getValue(10002).single().totalBytes)
    }
}

