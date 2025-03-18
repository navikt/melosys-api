package no.nav.melosys.itest

import ch.qos.logback.classic.Level
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.melosys.KafkaOffsetChecker
import no.nav.melosys.LoggingTestUtils
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.integrasjon.kafka.KafkaConfig
import no.nav.melosys.integrasjon.kafka.KafkaContainerService
import no.nav.melosys.integrasjon.kafka.SkippableKafkaErrorHandler
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.eessi.kafka.EessiMeldingConsumer
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.util.*
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
        EessiMeldingConsumer::class,
        KafkaTestConfig::class,
        KafkaConfig::class,
        SkippableKafkaErrorHandler::class,
        KafkaContainerService::class,
        KafkaOffsetChecker::class
    ]
)
class EessiMeldingConsumerIT(
    @Autowired @Qualifier("jsonSomString") private val kafkaTemplate: KafkaTemplate<String, String>,
    @Autowired private val skippableKafkaErrorHandler: SkippableKafkaErrorHandler,
    @Autowired private val kafkaOffsetChecker: KafkaOffsetChecker,
    @Autowired private val kafkaContainerService: KafkaContainerService,
    @Value("\${kafka.aiven.eessi.topic}") private val topic: String,
    @Value("\${kafka.aiven.eessi.groupId}") private val groupId: String
) {
    @MockkBean
    private lateinit var prosessinstansService: ProsessinstansService

    private val melosysEessiMeldingSlot = slot<MelosysEessiMelding>()

    @BeforeEach
    fun setUp() {
        clearMocks(prosessinstansService)
    }

    @Test
    @DirtiesContext
    fun `mottak av gyldig json på kafka kø skal fungere`() {
        prosessinstansService.also {
            every { it.opprettProsessinstansSedMottak(capture(melosysEessiMeldingSlot)) } returns mockk<UUID>()
        }
        val jsonMelosysEessiMelding = """
            {
                "sedId": "sedId"
            }
        """
        kafkaTemplate.send("teammelosys.eessi.v1-local", jsonMelosysEessiMelding)

        await.atMost(5, TimeUnit.SECONDS).until {
            melosysEessiMeldingSlot.isCaptured
        }
        melosysEessiMeldingSlot.captured.sedId shouldBe "sedId"
    }

    @Test
    @DirtiesContext
    fun `feil ved consumering skal føre til registering av json med offset`() {
        prosessinstansService.also {
            every { it.opprettProsessinstansSedMottak(any()) } throws RuntimeException("Error fra test")
        }
        val jsonMelosysEessiMelding = """
            {
                "sedId": "sedId"
            }
        """
        kafkaTemplate.send("teammelosys.eessi.v1-local", jsonMelosysEessiMelding)

        LoggingTestUtils.withLogCapture { logs ->
            await.atMost(5, TimeUnit.SECONDS).until {
                logs.any { it.formattedMessage == "Error handler threw an exception" }
            }
            logs.filter { it.level == Level.ERROR }.run {
                get(0).run {
                    formattedMessage shouldBe "Feil ved mottak av SED! ConsumerRecord.offset: 0"
                }
                get(1).run {
                    formattedMessage shouldBe "Error handler threw an exception"
                    throwableProxy.shouldNotBeNull()
                        .message shouldBe "Stopped container"
                }
            }
        }
    }

    @Test
    @DirtiesContext
    fun `feil ved consumering skal stoppe conteriner, og markToSkip skal hoppe over`() {
        prosessinstansService.also {
            every { it.opprettProsessinstansSedMottak(any()) } throws RuntimeException("Error fra test")
        }
        val jsonMelosysEessiMelding = """
            {
                "sedId": "sedId"
            }
        """
        kafkaOffsetChecker.offsetIncreased(topic, groupId) {
            kafkaTemplate.send("teammelosys.eessi.v1-local", jsonMelosysEessiMelding)


            LoggingTestUtils.withLogCapture { logs ->
                await.atMost(5, TimeUnit.SECONDS).until {
                    logs.any { it.formattedMessage == "Error handler threw an exception" }
                }
                logs.filter { it.level == Level.ERROR }.run {
                    get(0).run {
                        formattedMessage shouldBe "Feil ved mottak av SED! ConsumerRecord.offset: 0"
                    }
                    get(1).run {
                        formattedMessage shouldBe "Error handler threw an exception"
                        throwableProxy.shouldNotBeNull()
                            .message shouldBe "Stopped container"
                    }
                }
            }
        }.shouldBe(0)

        skippableKafkaErrorHandler.failedMessages
            .toList()
            .shouldHaveSize(1)
            .single().apply {
                first shouldBe "teammelosys.eessi.v1-local-0-0"
                second.apply {
                    offset shouldBe 0
                    partition shouldBe 0
                    topic shouldBe "teammelosys.eessi.v1-local"
                    errorStack shouldStartWith  "Error fra test - (RuntimeException)"
                    record.shouldNotBeNull()["value"] shouldBe MelosysEessiMelding().apply { sedId = "sedId" }
                }
            }

        kafkaOffsetChecker.offsetIncreased(topic, groupId) {
            skippableKafkaErrorHandler.markToSkip("teammelosys.eessi.v1-local-0-0")
            kafkaContainerService.ensureContainersRunningForTopic(topic)

            await.atMost(5, TimeUnit.SECONDS).until {
                skippableKafkaErrorHandler.failedMessages.isEmpty()
            }
        }.shouldBe(1)
    }

    @Test
    @DirtiesContext
    fun `feil ved json format skal stoppe conteriner, og markToSkip skal hoppe over`() {
        prosessinstansService.also {
            every { it.opprettProsessinstansSedMottak(any()) } returns mockk<UUID>()
        }
        val jsonMelosysEessiMelding = """
            {
                "tullball"
            }
        """
        kafkaOffsetChecker.offsetIncreased(topic, groupId) {
            kafkaTemplate.send("teammelosys.eessi.v1-local", jsonMelosysEessiMelding)


            LoggingTestUtils.withLogCapture { logs ->
                await.atMost(5, TimeUnit.SECONDS).until {
                    logs.any { it.formattedMessage == "Consumer exception" }
                }
                logs.filter { it.level == Level.ERROR }.run {
                    get(0).run {
                        formattedMessage shouldContain  "Deserializers with error"
                    }
                    get(1).run {
                        formattedMessage shouldBe "Consumer exception"
                        throwableProxy.shouldNotBeNull()
                            .message shouldBe "Stopped container"
                    }
                }
            }
        }.shouldBe(0)

        skippableKafkaErrorHandler.failedMessages
            .toList()
            .shouldHaveSize(1)
            .single().apply {
                first shouldBe "teammelosys.eessi.v1-local-0-0"
                second.apply {
                    offset shouldBe 0
                    partition shouldBe 0
                    topic shouldBe "teammelosys.eessi.v1-local"
                    errorStack shouldStartWith  "Unexpected character ('}'"

                    rawMessage shouldBe jsonMelosysEessiMelding
                    record.shouldBeNull()
                }
            }

        kafkaOffsetChecker.offsetIncreased(topic, groupId) {
            skippableKafkaErrorHandler.markToSkip("teammelosys.eessi.v1-local-0-0")
            kafkaContainerService.ensureContainersRunningForTopic(topic)

            await.atMost(5, TimeUnit.SECONDS).until {
                skippableKafkaErrorHandler.failedMessages.isEmpty()
            }
        }.shouldBe(1)
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
                get(0).message shouldContain "Deserializers with error:"
                get(1).run {
                    formattedMessage shouldBe "Consumer exception"
                    throwableProxy.shouldNotBeNull()
                        .message shouldBe "Stopped container"
                }
            }
        }
    }
}
