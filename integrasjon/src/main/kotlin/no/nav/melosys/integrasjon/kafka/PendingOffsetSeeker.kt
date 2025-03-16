package no.nav.melosys.integrasjon.kafka

import org.apache.kafka.common.TopicPartition
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class PendingOffsetSeeker : ConsumerSeekAware {

    // Map of pending seeks; key: TopicPartition, value: offset to seek to.
    private val pendingSeeks = ConcurrentHashMap<TopicPartition, Long>()

    fun addPendingSeek(tp: TopicPartition, offset: Long) {
        pendingSeeks[tp] = offset
    }

    override fun onPartitionsAssigned(assignments: Map<TopicPartition, Long>, callback: ConsumerSeekAware.ConsumerSeekCallback) {
        // For each pending seek, call callback.seek()
        pendingSeeks.forEach { (tp, offset) ->
            callback.seek(tp.topic(), tp.partition(), offset)
            println("Seeking ${tp.topic()}-${tp.partition()} to offset $offset")
            pendingSeeks.remove(tp)
        }
    }
}
