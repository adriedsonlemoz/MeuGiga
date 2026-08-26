package br.com.meugiga.app.domain

import br.com.meugiga.app.domain.model.RawUsageBucket
import br.com.meugiga.app.domain.model.TimelineUsage

object UsageAggregator {
    fun timeline(buckets: List<RawUsageBucket>): List<TimelineUsage> = buckets
        .groupBy { it.startMillis to it.endMillis }
        .map { (range, values) ->
            TimelineUsage(
                startMillis = range.first,
                endMillis = range.second,
                rxBytes = values.sumOf { it.rxBytes },
                txBytes = values.sumOf { it.txBytes },
            )
        }
        .sortedBy { it.startMillis }

    fun timelinesByUid(buckets: List<RawUsageBucket>): Map<Int, List<TimelineUsage>> =
        buckets.groupBy { it.uid }.mapValues { (_, values) -> timeline(values) }
}

