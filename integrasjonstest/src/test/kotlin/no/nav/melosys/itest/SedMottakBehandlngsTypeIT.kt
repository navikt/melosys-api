package no.nav.melosys.itest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.domain.eessi.*
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.*
import no.nav.melosys.melosysmock.journalpost.JournalpostRepo
import no.nav.melosys.melosysmock.oppgave.OppgaveRepo
import no.nav.melosys.melosysmock.testdata.TestDataGenerator
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.oppgave.OppgaveBehandlingstema
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.OpprettBehandlingForSak
import no.nav.melosys.service.sak.OpprettSakDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.KafkaTemplate
import java.time.LocalDate
import java.util.*

@Import(OAuthMockServer::class)
class SedMottakBehandlngsTypeIT(
    @Autowired @Qualifier("melosysEessiMelding") private val melosysEessiMeldingKafkaTemplate: KafkaTemplate<String, MelosysEessiMelding>,
    @Autowired private val eessiMeldingTestDataFactory: EessiMeldingTestDataFactory,
    @Autowired private val opprettBehandlingForSak: OpprettBehandlingForSak,
    @Autowired private val oppgaveRepo: OppgaveRepo,
    @Autowired private val journalpostRepo: JournalpostRepo,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val unleash: FakeUnleash,

    @Autowired testDataGenerator: TestDataGenerator,
    @Autowired journalføringService: JournalfoeringService,
    @Autowired oppgaveService: OppgaveService,
    @Autowired prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val oAuthMockServer: OAuthMockServer
) : JournalfoeringBase(testDataGenerator, journalføringService, oppgaveService, prosessinstansRepository) {

    private val kafkaTopic = "teammelosys.eessi.v1-local"

    @BeforeEach
    fun setup() {
        oAuthMockServer.start()
        oppgaveRepo.repo.clear()
        unleash.resetAll()
    }

    @AfterEach
    fun afterEach() {
        oAuthMockServer.stop()
    }

    @Test
    fun `A003 skal føre til riktig oppgave i gosys`() {
        val eessiMeldingA003 = eessiMeldingTestDataFactory.melosysEessiMelding(
            bucType = BucType.LA_BUC_02,
            rinaSaksnummer = Random().nextInt(100000).toString(),
            sedType = SedType.A003,
            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1)),
            artikkel = "13_1_a",
            lovvalgsland = "NO"
        )


        executeAndWait(
            ProsessType.MOTTAK_SED, listOf(
                ProsessType.ARBEID_FLERE_LAND_NY_SAK
            )
        ) {
            melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingA003)
        }


        oppgaveRepo.repo.values
            .shouldHaveSize(1)
            .first()
            .apply {
                behandlingstema.shouldBe(OppgaveBehandlingstema.EU_EOS_NORGE_ER_UTPEKT_SOM_LOVVALGSLAND.kode)
                behandlingstype.shouldBe(null)
                // eksempel: --- 18.04.2023 08:22 (srvmelosys, Melosys) ---\n A003 - MEL-41\n
                beskrivelse.shouldContain("A003")
                oppgavetype.shouldBe(Oppgavetyper.BEH_SED.kode)
            }

    }


    @Test
    fun `A003 andre gangsbehandling`() {
        val eessiMeldingA003 = eessiMeldingTestDataFactory.melosysEessiMelding(
            bucType = BucType.LA_BUC_02,
            rinaSaksnummer = Random().nextInt(100000).toString(),
            sedType = SedType.A003,
            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1)),
            artikkel = "13_1_a",
            lovvalgsland = "NO"
        )
        val prosessinstansArbeidFlereLand =
            executeAndWait(ProsessType.ARBEID_FLERE_LAND_NY_SAK) {
                melosysEessiMeldingKafkaTemplate.send(kafkaTopic, eessiMeldingA003)
            }

        val behandling = prosessinstansArbeidFlereLand.behandling
        behandling.status = Behandlingsstatus.AVSLUTTET
        behandlingRepository.save(behandling)

        val opprettSakDto = OpprettSakDto().apply {
            behandlingstema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
            behandlingstype = Behandlingstyper.NY_VURDERING
            skalTilordnes = true
            mottaksdato = LocalDate.now()
            behandlingsaarsakType = Behandlingsaarsaktyper.SED
        }


        val saksnummer: String = behandling.fagsak.saksnummer
        executeAndWait(ProsessType.OPPRETT_REPLIKERT_BEHANDLING_FOR_SAK) {
            opprettBehandlingForSak.opprettBehandling(saksnummer, opprettSakDto)
        }


        fagsakRepository.findBySaksnummer(saksnummer).get().behandlinger
            .shouldHaveSize(2)
    }

    @Test
    @Disabled
    fun `A003 lag data og skriv ut så det kan brukes i mock`() {
        `A003 skal føre til riktig oppgave i gosys`()

        oppgaveRepo.repo.forEach {
            println(it.toJsonNode.toPrettyString())
        }
        journalpostRepo.repo.forEach {
            println(it.toJsonNode.toPrettyString())
        }
    }


    private val Any.toJsonNode: JsonNode
        get() {
            return jacksonObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(JavaTimeModule())
                .valueToTree(this)
        }
}
