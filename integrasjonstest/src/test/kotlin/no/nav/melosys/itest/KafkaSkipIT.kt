package no.nav.melosys.itest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import no.nav.melosys.KafkaOffsetChecker
import no.nav.melosys.LoggingTestUtils
import no.nav.melosys.integrasjon.kafka.KafkaConfig
import no.nav.melosys.integrasjon.kafka.KafkaContainerService
import no.nav.melosys.integrasjon.kafka.KafkaErrorController
import no.nav.melosys.integrasjon.kafka.SkippableKafkaErrorHandler
import no.nav.melosys.integrasjon.kafka.SkippableKafkaErrorHandler.Failed
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.AdminController
import no.nav.melosys.service.eessi.kafka.EessiMeldingConsumer
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.concurrent.TimeUnit


@ActiveProfiles("test")
@EmbeddedKafka(
    count = 1, controlledShutdown = true, partitions = 1,
    topics = ["teammelosys.eessi.v1-local"],
    brokerProperties = ["offsets.topic.replication.factor=1", "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"]
)
@ContextConfiguration(
    classes = [
        JacksonAutoConfiguration::class,
        KafkaAutoConfiguration::class,
        HttpMessageConvertersAutoConfiguration::class,
        WebMvcAutoConfiguration::class,
        EessiMeldingConsumer::class,
        KafkaTestConfig::class,
        KafkaConfig::class,
        SkippableKafkaErrorHandler::class,
        KafkaContainerService::class,
        KafkaOffsetChecker::class,
        KafkaErrorController::class
    ]
)
@SpringBootTest()
@AutoConfigureMockMvc
class KafkaSkipIT(
    @Autowired @Qualifier("jsonSomString") private val kafkaTemplate: KafkaTemplate<String, String>,
    @Autowired private val skippableKafkaErrorHandler: SkippableKafkaErrorHandler,
    @Autowired private val kafkaOffsetChecker: KafkaOffsetChecker,
    @Value("\${kafka.aiven.eessi.topic}") private val topic: String,
    @Value("\${kafka.aiven.eessi.groupId}") private val groupId: String
) {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var prosessinstansService: ProsessinstansService

    @BeforeEach
    fun setUp() {
        clearMocks(prosessinstansService)
    }

    @Test
    fun `should skip failed message and remove it from failedMessages`() {
        prosessinstansService.also {
            every { it.opprettProsessinstansSedMottak(any()) } throws RuntimeException("Error fra test")
        }
        kafkaOffsetChecker.offsetIncreased(topic, groupId) {
            kafkaTemplate.send("teammelosys.eessi.v1-local", "{\"sedId\": \"sedId\"}")

            LoggingTestUtils.withLogCapture { logs ->
                await.atMost(5, TimeUnit.SECONDS).until {
                    logs.any { it.formattedMessage == "Error handler threw an exception" }
                }
            }
        }.shouldBe(0) // offset should not be increased

        skippableKafkaErrorHandler.failedMessages.shouldHaveSize(1)

        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/admin/kafka/errors")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)
            .andReturn()

        val node: JsonNode = jacksonObjectMapper().readTree(result.response.contentAsString)
        val errorsNode = node.get("errors")
        val errorKey = errorsNode.fieldNames().next()

        kafkaOffsetChecker.offsetIncreased(topic, groupId) {
            mockMvc.perform(
                MockMvcRequestBuilders.delete("/admin/kafka/errors/$errorKey")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(AdminController.API_KEY_HEADER, "Dummy")
            ).andExpect(status().isOk)

            await.atMost(5, TimeUnit.SECONDS).until {
                skippableKafkaErrorHandler.failedMessages.isEmpty()
            }
        }.shouldBe(1)  // offset should be increased after skipping the message

        skippableKafkaErrorHandler.failedMessages.shouldBeEmpty()
    }

    @Test
    fun `should remove message from failedMessages if already skipped by another pod`() {

        skippableKafkaErrorHandler.failedMessages["teammelosys.skattehendelser.v1-q2-0-77"] =
            Failed(
                topic = "teammelosys.skattehendelser.v1-q2",
                offset = 77,
                partition = 0,
                record = null,
                rawMessage = "{}",
                errorStack = ""
            )

        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/admin/kafka/errors")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(AdminController.API_KEY_HEADER, "Dummy")
        ).andExpect(status().isOk)
            .andReturn()

        val node: JsonNode = jacksonObjectMapper().readTree(result.response.contentAsString)
        val errorsNode = node.get("errors")
        val errorKey = errorsNode.fieldNames().next()

        kafkaOffsetChecker.offsetIncreased(topic, groupId) {
            mockMvc.perform(
                MockMvcRequestBuilders.delete("/admin/kafka/errors/$errorKey")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(AdminController.API_KEY_HEADER, "Dummy")
            ).andExpect(status().isOk)
                .andReturn()
            await.atMost(5, TimeUnit.SECONDS).until {
                skippableKafkaErrorHandler.failedMessages.isEmpty()


            }
        }.shouldBe(0)

        skippableKafkaErrorHandler.failedMessages.shouldBeEmpty()
    }
}
