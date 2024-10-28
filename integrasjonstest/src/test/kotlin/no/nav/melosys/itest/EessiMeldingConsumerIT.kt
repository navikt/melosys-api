package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.LoggingTestUtils
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
        EessiMeldingConsumerIT.Config::class,
        EessiMeldingConsumer::class,
        KafkaTestConfig::class,
        KafkaConfig::class
    ]
)
class EessiMeldingConsumerIT(
    @Autowired @Qualifier("melosysEessiMeldingString") private val melosysEessiMeldingKafkaTemplateString: KafkaTemplate<String, String>
) {

    @TestConfiguration
    class Config {
        @Bean
        fun prosessinstansService() = mockk<ProsessinstansService>(relaxed = true).also {
            every { it.opprettProsessinstansSedMottak(any()) } returns mockk<UUID>()
        }
    }

    val jsonMelosysEessiMeldingMedFeil = """
            {
                "sedId": "sedId",
                "avsender": []
            }
        """

    @Test
    fun `mottak av ugyldig json på kafka kø skal feile å logge`() {
        melosysEessiMeldingKafkaTemplateString.send("teammelosys.eessi.v1-local", jsonMelosysEessiMeldingMedFeil)

        LoggingTestUtils.withLogCapture { logs ->
            await.atMost(5, TimeUnit.SECONDS).untilAsserted {
                logs.shouldHaveSize(2)
            }

            logs.map { it.formattedMessage }.run {
                get(0) shouldBe  ("Failed to deserialize message on topic teammelosys.eessi.v1-local: $jsonMelosysEessiMeldingMedFeil")
                get(1) shouldContain ("Consumer exception")
            }
        }
    }

}
