package no.nav.melosys.itest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.Application
import no.nav.melosys.melosysmock.medl.MedlRepo
import no.nav.melosys.melosysmock.melosyseessi.MelosysEessiRepo
import no.nav.melosys.melosysmock.sak.SakRepo
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(
    classes = [Application::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@EmbeddedKafka(
    count = 1, controlledShutdown = true, partitions = 1,
    topics = ["teammelosys.eessi.v1-local", "teammelosys.soknad-mottak.v1-local", "teammelosys.melosys-utstedt-a1.v1-local",
        "teammelosys.fattetvedtak.v1-local", "teammelosys.manglende-fakturabetaling-local", "teammelosys.melosys-hendelse-local"],
    brokerProperties = ["offsets.topic.replication.factor=1", "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"]
)
@Import(KafkaTestConfig::class)
@DirtiesContext
@EnableMockOAuth2Server
class ComponentTestBase : OracleTestContainerBase() {
    @Autowired
    private lateinit var unleash: Unleash

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    val fakeUnleash: FakeUnleash by lazy {
        unleash.shouldBeInstanceOf<FakeUnleash>()
    }

    @AfterEach
    fun afterEachComponentTestBase() {
        SakRepo.clear()
        MedlRepo.repo.clear()
        MelosysEessiRepo.sedRepo.clear()
        fakeUnleash.enableAll()
    }

    val Any.toJsonNode: JsonNode
        get() = objectMapper.valueToTree(this)
}
