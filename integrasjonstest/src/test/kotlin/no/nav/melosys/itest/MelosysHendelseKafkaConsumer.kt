package no.nav.melosys.itest

import no.nav.melosys.integrasjon.hendelser.MelosysHendelse
import no.nav.melosys.integrasjon.kafka.KafkaConsumerContainerFactory
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.stereotype.Component
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

@Component
class MelosysHendelseKafkaConsumer {

    private val records: BlockingQueue<ConsumerRecord<String, MelosysHendelse>> = LinkedBlockingQueue()

    @KafkaListener(topics = ["\${kafka.aiven.melosys-hendelser.topic}"], groupId = "\${kafka.aiven.melosys-hendelse.groupid}",
        containerFactory = "melosysMeldingListenerContainerFactory")
    private fun melosysHendelseListener(record: ConsumerRecord<String, MelosysHendelse>) {
        records.add(record)
    }

    val melosysHendelser: List<ConsumerRecord<String, MelosysHendelse>>
        get() = records.toList()

    fun clear() {
        records.clear()
    }

    @TestConfiguration
    class Config {
        @Bean
        fun melosysMeldingListenerContainerFactory(
            kafkaProperties: KafkaProperties
        ): KafkaConsumerContainerFactory<MelosysHendelse> =
            ConcurrentKafkaListenerContainerFactory<String, MelosysHendelse>().apply {
                consumerFactory = DefaultKafkaConsumerFactory(
                    kafkaProperties.buildConsumerProperties(null),
                    StringDeserializer(),
                    JsonDeserializer(MelosysHendelse::class.java, false)
                )
            }
    }
}
