package no.nav.melosys.integrasjon.hendelser

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.exception.TekniskException
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

class KafkaMelosysHendelseProducerTest {

    private val kafkaTemplate: KafkaTemplate<String, MelosysHendelse> = mockk()
    private val producer = KafkaMelosysHendelseProducer(TOPIC, kafkaTemplate)

    private val logger = LoggerFactory.getLogger(KafkaMelosysHendelseProducer::class.java) as Logger
    private val listAppender = ListAppender<ILoggingEvent>()

    private val hendelse = MelosysHendelse(
        VedtakHendelseMelding(
            folkeregisterIdent = FOLKEREGISTERIDENT,
            sakstype = Sakstyper.FTRL,
            behandligsresultatType = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN,
            medlemskapsperioder = listOf(
                Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), InnvilgelsesResultat.INNVILGET)
            ),
            lovvalgsperioder = emptyList()
        )
    )

    @BeforeEach
    fun setup() {
        listAppender.start()
        logger.addAppender(listAppender)
    }

    @AfterEach
    fun tearDown() {
        logger.detachAppender(listAppender)
    }

    @Test
    fun `logger sendt melding uten folkeregisterident`() {
        every { kafkaTemplate.send(any<ProducerRecord<String, MelosysHendelse>>()) } answers {
            val record = firstArg<ProducerRecord<String, MelosysHendelse>>()
            CompletableFuture.completedFuture(SendResult(record, RecordMetadata(TopicPartition(TOPIC, 0), 42, 0, 0, 0, 0)))
        }

        producer.produserBestillingsmelding(hendelse)

        val loggmelding = listAppender.list.shouldHaveSize(1).single().formattedMessage
        loggmelding shouldNotContain FOLKEREGISTERIDENT
        loggmelding shouldContain "VedtakHendelseMelding"
        loggmelding shouldContain "sakstype=FTRL"
        loggmelding shouldContain "medlemskapsperioder=1"
        loggmelding shouldContain "Offset: 42"
    }

    @Test
    fun `feilmelding ved sendefeil har ikke folkeregisterident`() {
        every { kafkaTemplate.send(any<ProducerRecord<String, MelosysHendelse>>()) } returns
            CompletableFuture.failedFuture(RuntimeException("broker nede"))

        val feil = shouldThrow<TekniskException> { producer.produserBestillingsmelding(hendelse) }

        feil.message!! shouldNotContain FOLKEREGISTERIDENT
        feil.message!! shouldContain "sakstype=FTRL"
    }

    @Test
    fun `synkron sendefeil gir feilmelding uten folkeregisterident`() {
        every { kafkaTemplate.send(any<ProducerRecord<String, MelosysHendelse>>()) } throws
            RuntimeException("serialisering feilet")

        val feil = shouldThrow<TekniskException> { producer.produserBestillingsmelding(hendelse) }

        feil.message!! shouldNotContain FOLKEREGISTERIDENT
        feil.message!! shouldContain "sakstype=FTRL"
    }

    private companion object {
        const val TOPIC = "teammelosys.melosys-hendelser.v1"
        const val FOLKEREGISTERIDENT = "30056928150"
    }
}
