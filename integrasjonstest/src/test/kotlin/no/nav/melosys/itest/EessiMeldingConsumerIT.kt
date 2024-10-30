package no.nav.melosys.itest

import ch.qos.logback.classic.Level
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.melosys.LoggingTestUtils
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.integrasjon.kafka.KafkaConfig
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.eessi.kafka.EessiMeldingConsumer
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.util.UUID
import java.util.concurrent.TimeUnit

@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka(
    count = 1, controlledShutdown = true, partitions = 1,
    topics = ["teammelosys.eessi.v1-local"],
    brokerProperties = ["offsets.topic.replication.factor=1", "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"]
)
@ContextConfiguration(
    classes = [
        JacksonAutoConfiguration::class,
        KafkaAutoConfiguration::class,
        EessiMeldingConsumerIT.TestConfig::class,
        EessiMeldingConsumer::class,
        KafkaTestConfig::class,
        KafkaConfig::class
    ]
)
class EessiMeldingConsumerIT(
    @Autowired @Qualifier("kafkaTemplateString") private val kafkaTemplate: KafkaTemplate<String, String>,
    @Autowired private val testConfig: TestConfig
) {

    @TestConfiguration
    class TestConfig {
        private val melosysEessiMeldingSlot = slot<MelosysEessiMelding>()

        fun capturedMelosysEessiMelding() = melosysEessiMeldingSlot.captured

        fun isCaptured() = melosysEessiMeldingSlot.isCaptured

        @Bean
        fun prosessinstansService() = mockk<ProsessinstansService>(relaxed = true).also {
            every { it.opprettProsessinstansSedMottak(capture(melosysEessiMeldingSlot)) } returns mockk<UUID>()
        }
    }

    @Test
    @DirtiesContext
    fun `mottak av gyldig json på kafka kø skal fungere`() {
        val jsonMelosysEessiMelding = """
            {
                "sedId": "sedId"
            }
        """
        kafkaTemplate.send("teammelosys.eessi.v1-local", jsonMelosysEessiMelding)

        await.atMost(5, TimeUnit.SECONDS).until {
            testConfig.isCaptured()
        }
        testConfig.capturedMelosysEessiMelding().sedId shouldBe "sedId"
    }

    @Test
    @DirtiesContext
    fun `mottak av ugyldig json på kafka kø skal feile å logge`() {
        val jsonMelosysEessiMeldingMedFeil = """
            {
                "sedId": "sedId",
                "avsender": []
            }
        """

        kafkaTemplate.send("teammelosys.eessi.v1-local", jsonMelosysEessiMeldingMedFeil)

        LoggingTestUtils.withLogCapture { logs ->
            await.atMost(5, TimeUnit.SECONDS).until {
                logs.any { it.formattedMessage == "Consumer exception" }
            }
            logs.filter { it.level == Level.ERROR }.run {
                get(0).formattedMessage shouldBe "Failed to deserialize message on topic teammelosys.eessi.v1-local: $jsonMelosysEessiMeldingMedFeil"
                get(1).run {
                    formattedMessage shouldBe "Consumer exception"
                    throwableProxy.shouldNotBeNull()
                        .message shouldBe "Stopped container"
                }
            }
        }
    }
}
