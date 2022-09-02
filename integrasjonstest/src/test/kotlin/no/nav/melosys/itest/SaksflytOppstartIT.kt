package no.nav.melosys.itest

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.Application
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.RegistreringsInfo
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.saksflyt.ProsessDataKey
import no.nav.melosys.domain.saksflyt.ProsessStatus
import no.nav.melosys.domain.saksflyt.ProsessType
import no.nav.melosys.domain.saksflyt.Prosessinstans
import no.nav.melosys.integrasjon.joark.saf.SafConsumer
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.*
import no.nav.melosys.melosysmock.sak.SakRepo
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.repository.ProsessinstansRepository
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

@Disabled
@ActiveProfiles("test")
@SpringBootTest(
    classes = [Application::class, SaksflytTestConfig::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableMockOAuth2Server
@DirtiesContext
internal class SaksflytOppstartIT(
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val applicationEventPublisher: ApplicationEventPublisher,
) : OracleTestContainerBase() {

    @MockkBean
    lateinit var safConsumer: SafConsumer

    @BeforeEach
    fun before() {
        SakRepo.clear()
    }

    @Test
    fun `prosessinstansBehandler laster og publiserer prosesser som har hengt ved oppstart`() {
        val fagsak = lagFagsak().also { fagsakRepository.save(it) }
        val behandling = lagBehandling(fagsak).also { behandlingRepository.save(it) }

        val prosessinstansSomTrengerRekjøring = lagProsessinstans(
            ProsessType.MOTTAK_SED, ProsessStatus.PÅ_VENT, behandling, LocalDateTime.now().minusDays(2)
        ).apply {
            this.setData(ProsessDataKey.EESSI_MELDING, eessiMelding())
        }.also { prosessinstansRepository.save(it) }

        lagProsessinstans(
            ProsessType.MOTTAK_SED, ProsessStatus.PÅ_VENT, behandling, LocalDateTime.now().minusHours(3)
        ).also { prosessinstansRepository.save(it) }

        lagProsessinstans(
            ProsessType.UTPEKING_AVVIS, ProsessStatus.FERDIG, behandling, LocalDateTime.now().minusDays(4)
        ).also { prosessinstansRepository.save(it) }

        every { safConsumer.hentJournalpost(eessiMelding().journalpostId) } returns journalpost()


        applicationEventPublisher.publishEvent(applicationReadyEvent())
        Awaitility.await().timeout(Duration.ofMinutes(1)).pollInterval(Duration.ofSeconds(2)).until {
            prosessinstansRepository.findAllByStatusIn(
                ProsessStatus.hentAktiveStatuser(),
            ).size == 1
        }


        assertThat(prosessinstansRepository.findById(prosessinstansSomTrengerRekjøring.id)).isPresent.get()
            .matches { it.status == ProsessStatus.FERDIG }
    }

    private fun journalpost(): Journalpost = Journalpost(
        "jpID",
        "Tittel",
        Journalstatus.JOURNALFOERT,
        Tema.MED.kode,
        Journalposttype.I,
        Sak("MEL-123"),
        Bruker("123123", Brukertype.FNR),
        AvsenderMottaker("010101", AvsenderMottakerType.ORGNR, "Org AS", null),
        "SKAN_NETS",
        setOf(RelevantDato(LocalDateTime.now(), Datotype.DATO_REGISTRERT)),
        listOf(DokumentInfo("123", "hoveddokument kommer først", null, listOf(), listOf()))
    )

    private fun lagProsessinstans(
        type: ProsessType, status: ProsessStatus, behandling: Behandling? = null, endretDato: LocalDateTime
    ): Prosessinstans {
        return Prosessinstans().apply {
            this.behandling = behandling
            this.type = type
            this.status = status
            sistFullførtSteg = null
            registrertDato = endretDato
            this.endretDato = endretDato
        }
    }

    private fun eessiMelding(): MelosysEessiMelding = MelosysEessiMelding().apply {
        journalpostId = "jpID"
        sedType = "X001"
        sedId = "komptest oppstart"
        rinaSaksnummer = "Rina brief case"
    }

    fun applicationReadyEvent(): ApplicationReadyEvent {
        return ApplicationReadyEvent(mockk(), emptyArray(), mockk())
    }

    private fun lagFagsak(): Fagsak = Fagsak().apply {
        saksnummer = "MEL-1007"
        type = Sakstyper.EU_EOS
        tema = Sakstemaer.MEDLEMSKAP_LOVVALG
        status = Saksstatuser.OPPRETTET
        leggTilRegisteringInfo()
    }

    private fun lagBehandling(fagsak: Fagsak): Behandling = Behandling().apply {
        id = 1
        this.fagsak = fagsak
        type = Behandlingstyper.SED
        tema = Behandlingstema.ØVRIGE_SED_MED
        status = Behandlingsstatus.OPPRETTET
        behandlingsfrist = LocalDate.now().plusMonths(1)
        leggTilRegisteringInfo()
    }

    private fun RegistreringsInfo.leggTilRegisteringInfo() {
        registrertDato = Instant.now()
        endretDato = Instant.now()
        endretAv = "bla"
    }
}
