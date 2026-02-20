package no.nav.melosys.itest

import no.nav.melosys.integrasjon.popp.PensjonsopptjeningHendelse
import no.nav.melosys.integrasjon.kafka.KafkaConsumerContainerFactory
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.melosys.integrasjon.kafka.LoggingDeserializer
import org.springframework.stereotype.Component
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

@Component
class PensjonsopptjeningHendelseKafkaConsumer {

    private val records: BlockingQueue<ConsumerRecord<String, PensjonsopptjeningHendelse>> = LinkedBlockingQueue()

    @KafkaListener(
        topics = ["\${kafka.aiven.popp-hendelser.topic}"], groupId = "\${kafka.aiven.popp-hendelser.groupid}",
        containerFactory = "pensjonsopptjeningHendelseListenerContainerFactory")
    private fun melosysHendelseListener(record: ConsumerRecord<String, PensjonsopptjeningHendelse>) {
        records.add(record)
    }

    val pensjonsopptjeningHendelser: List<ConsumerRecord<String, PensjonsopptjeningHendelse>>
        get() = records.toList()

    fun clear() {
        records.clear()
    }

    @TestConfiguration
    class Config {
        @Bean
        fun pensjonsopptjeningHendelseListenerContainerFactory(
            kafkaProperties: KafkaProperties,
            objectMapper: ObjectMapper
        ): KafkaConsumerContainerFactory<PensjonsopptjeningHendelse> =
            ConcurrentKafkaListenerContainerFactory<String, PensjonsopptjeningHendelse>().apply {
                setConsumerFactory(DefaultKafkaConsumerFactory(
                    kafkaProperties.buildConsumerProperties(),
                    StringDeserializer(),
                    LoggingDeserializer(objectMapper, PensjonsopptjeningHendelse::class.java)
                ))
            }
    }
}
