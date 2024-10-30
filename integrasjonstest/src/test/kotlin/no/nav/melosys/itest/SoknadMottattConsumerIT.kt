package no.nav.melosys.itest

import ch.qos.logback.classic.Level
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.melosys.LoggingTestUtils
import no.nav.melosys.integrasjon.kafka.KafkaConfig
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.soknad.SoknadMottattConsumer
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
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka(
    count = 1, controlledShutdown = true, partitions = 1,
    topics = ["teammelosys.soknad-mottak.v1-local"],
    brokerProperties = ["offsets.topic.replication.factor=1", "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"]
)
@ContextConfiguration(
    classes = [
        JacksonAutoConfiguration::class,
        KafkaAutoConfiguration::class,
        SoknadMottattConsumer::class,
        SoknadMottattConsumerIT.TestConfig::class,
        KafkaTestConfig::class,
        KafkaConfig::class
    ]
)
class SoknadMottattConsumerIT(
    @Autowired @Qualifier("melosysEessiMeldingString") private val templateString: KafkaTemplate<String, String>,
    @Autowired private val testConfig: TestConfig
) {

    @TestConfiguration
    class TestConfig {
        private val soknadIDSlot = slot<String>()

        fun capturedSoknadID() = soknadIDSlot.captured

        fun isCaptured() = soknadIDSlot.isCaptured

        @Bean
        fun prosessinstansService() = mockk<ProsessinstansService>(relaxed = true).also {
            every { it.opprettProsessinstansSøknadMottatt(capture(soknadIDSlot), any(), any()) } returns Unit
        }

        @Bean
        fun mottatteOpplysningerService() = mockk<MottatteOpplysningerService>(relaxed = true)
    }

    @Test
    @DirtiesContext
    fun `mottak av gyldig json på kafka kø skal fungere`() {
        val melding = """
            {
                "soknadID": "123",
                "occuredOn": "${ZonedDateTime.now()}"
            }
        """
        templateString.send("teammelosys.soknad-mottak.v1-local", melding)

        await.atMost(5, TimeUnit.SECONDS).until {
            testConfig.isCaptured()
        }
        testConfig.capturedSoknadID() shouldBe "123"
    }

    @Test
    @DirtiesContext
    fun `mottak av ugyldig json på kafka kø skal feile å logge`() {
        val melding = """
            {
                soknadID: 123
            }
        """

        templateString.send("teammelosys.soknad-mottak.v1-local", melding)

        LoggingTestUtils.withLogCapture { logs ->
            await.atMost(5, TimeUnit.SECONDS).until {
                logs.any { it.formattedMessage == "Consumer exception" }
            }
            logs.filter { it.level == Level.ERROR }.run {
                get(0).formattedMessage shouldBe "Failed to deserialize message on topic teammelosys.soknad-mottak.v1-local: $melding"
                get(1).run {
                    formattedMessage shouldBe "Consumer exception"
                    throwableProxy.shouldNotBeNull()
                        .message shouldBe "Stopped container"
                }
            }
        }
    }
}
