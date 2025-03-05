package no.nav.melosys.itest

import no.nav.melosys.Application
import no.nav.melosys.service.ftrl.FinnPersonerHvorÅrsavregningSkalOpprettes
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Disabled
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Test

@ActiveProfiles("test")
@SpringBootTest(
    classes = [Application::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@EmbeddedKafka(
    count = 1, controlledShutdown = true, partitions = 1,
    topics = ["teammelosys.eessi.v1-local", "teammelosys.soknad-mottak.v1-local", "teammelosys.melosys-utstedt-a1.v1-local", "teammelosys.fattetvedtak.v1-local"],
    brokerProperties = ["offsets.topic.replication.factor=1", "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
@EnableMockOAuth2Server
@Disabled("Kjøres manuelt og tester da mot det som alt ligger i local oracleDB")
class FinnPersonerHvorÅrsavregningSkalOpprettesIT(
    @Autowired private val finnPersonerHvorÅrsavregningSkalOpprettes: FinnPersonerHvorÅrsavregningSkalOpprettes,
) : OracleTestContainerBase() {

    @Test
    fun `finn personer og send kafka meldinger`() {
        finnPersonerHvorÅrsavregningSkalOpprettes.finnSakerOgLeggPåKø(false)
    }

}
