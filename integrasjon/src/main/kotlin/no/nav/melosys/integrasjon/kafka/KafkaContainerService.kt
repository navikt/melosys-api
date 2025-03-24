package no.nav.melosys.integrasjon.kafka

import mu.KotlinLogging
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class KafkaContainerService(
    private val registry: KafkaListenerEndpointRegistry
) {

    fun ensureContainersRunningForTopic(topic: String): Int {
        val relevantContainers = findContainersForTopic(topic)

        if (relevantContainers.isEmpty()) {
            log.warn("No containers found for topic: $topic")
            return 0
        }

        var startedCount = 0

        // Only start containers that aren't already running
        relevantContainers.forEach { container ->
            if (!container.isRunning) {
                log.info("Starting stopped container for topic $topic: ${container.listenerId}")
                container.start()
                startedCount++
            } else {
                log.debug("Container already running for topic $topic: ${container.listenerId}")
            }
        }

        return startedCount
    }

    private fun findContainersForTopic(topic: String) = registry.listenerContainers
        .filter { container ->
            container.containerProperties.topics?.contains(topic) == true ||
                container.containerProperties.topicPattern?.pattern()?.let {
                    topic.matches(it.toRegex())
                } == true
        }
}
