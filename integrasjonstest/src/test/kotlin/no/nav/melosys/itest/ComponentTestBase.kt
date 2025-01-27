package no.nav.melosys.itest

import io.getunleash.FakeUnleash
import no.nav.melosys.Application
import no.nav.melosys.melosysmock.config.GraphqlConfig
import no.nav.melosys.melosysmock.config.SoapConfig
import no.nav.melosys.melosysmock.medl.MedlRepo
import no.nav.melosys.melosysmock.melosyseessi.MelosysEessiRepo
import no.nav.melosys.melosysmock.sak.SakRepo
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
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
@Import(GraphqlConfig::class, SoapConfig::class, KafkaTestConfig::class, ComponentTestBase.TestConfig::class)
@DirtiesContext
@EnableMockOAuth2Server
class ComponentTestBase : OracleTestContainerBase() {

    @TestConfiguration
    class TestConfig {
        // TODO: vi kan fjerne denne og bruke FeatureToggleConfig som bruker enable all som vi vil så på etter rydding
        @Primary
        @Bean
        fun fakeUnleash(): FakeUnleash = FakeUnleash()
    }

    @Autowired
    protected lateinit var unleash: FakeUnleash

    @AfterEach
    fun afterEachComponentTestBase() {
        SakRepo.clear()
        MedlRepo.repo.clear()
        MelosysEessiRepo.sedRepo.clear()
    }
}
