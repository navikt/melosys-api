package no.nav.melosys.itest

import no.nav.melosys.Application
import no.nav.melosys.ProsessRegister
import no.nav.melosys.ProsessinstansTestManager
import no.nav.melosys.domain.arsavregning.Skattehendelse
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest(
    classes = [Application::class, SaksflytTestConfig::class],
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
class ArsavregningBehandlingTest(
    @Autowired private val prosessinstansService: ProsessinstansService,
    @Autowired private val prosessRegister: ProsessRegister,
    @Autowired private val prosessinstansTestManager: ProsessinstansTestManager,
    @Autowired private val skatteHendelseMeldingKafkaTemplate: KafkaTemplate<String, Skattehendelse>
):ComponentTestBase() {


    @Test
    fun name() {
        val skattehendelse = Skattehendelse("2023", "123", 1, false)
        skatteHendelseMeldingKafkaTemplate.send("teammelosys.skattehendelser.v1-local", skattehendelse)

        prosessinstansTestManager.executeAndWait(
            waitForprosessType = ProsessType.OPPRETT_NY_BEHANDLING_ARSAVREGNING,
            waitForProcessCount = 1
        ) {
            prosessRegister.registrer("manglendeInnbetaling-1-Prosess") {
                prosessinstansService.opprettArsavregningsBehandlingProsessflyt(skattehendelse)
            }
        }
    }
}
