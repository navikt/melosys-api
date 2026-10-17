package no.nav.melosys.integrasjon.kafka

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.integrasjon.hendelser.MelosysHendelse
import no.nav.melosys.integrasjon.hendelser.VedtakHendelseMelding
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.LoggingProducerListener

class MelosysHendelseKafkaTemplateTest {

    private val kafkaConfig = KafkaConfig(mockk(relaxed = true), "", "", "", "", mockk())

    @Test
    fun `sendefeil logges uten folkeregisterident`() {
        val mockProducer = MockProducer(false, null, StringSerializer(), Serializer<MelosysHendelse> { _, _ -> ByteArray(0) })
        val producerFactory: ProducerFactory<String, MelosysHendelse> = mockk(relaxed = true) {
            every { createProducer() } returns mockProducer
        }
        val kafkaTemplate = kafkaConfig.melosysHendelseKafkaTemplate(producerFactory)
        val hendelse = MelosysHendelse(
            VedtakHendelseMelding(
                folkeregisterIdent = FOLKEREGISTERIDENT,
                sakstype = Sakstyper.FTRL,
                behandligsresultatType = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN,
                medlemskapsperioder = emptyList(),
                lovvalgsperioder = emptyList()
            )
        )
        val logger = LoggerFactory.getLogger(LoggingProducerListener::class.java) as Logger
        val listAppender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(listAppender)

        try {
            kafkaTemplate.send(ProducerRecord("topic", hendelse))
            mockProducer.errorNext(RuntimeException("broker nede"))
        } finally {
            logger.detachAppender(listAppender)
        }

        listAppender.list.shouldNotBeEmpty().forEach { it.formattedMessage shouldNotContain FOLKEREGISTERIDENT }
    }

    private companion object {
        const val FOLKEREGISTERIDENT = "30056928150"
    }
}
