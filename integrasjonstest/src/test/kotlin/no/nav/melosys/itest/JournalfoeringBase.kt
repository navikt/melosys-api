package no.nav.melosys.itest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.melosys.domain.arkiv.ArkivDokument
import no.nav.melosys.domain.kodeverk.Avsendertyper
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.saksflyt.ProsessStatus
import no.nav.melosys.domain.saksflyt.ProsessType
import no.nav.melosys.domain.saksflyt.Prosessinstans
import no.nav.melosys.melosysmock.oppgave.Oppgave
import no.nav.melosys.melosysmock.sak.SakRepo
import no.nav.melosys.melosysmock.testdata.TestDataGenerator
import no.nav.melosys.repository.ProsessinstansRepository
import no.nav.melosys.service.felles.dto.SoeknadslandDto
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.journalforing.dto.*
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.context.annotation.Import
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit

@Import(KodeverkStub::class)
class JournalfoeringBase(
    protected val testDataGenerator: TestDataGenerator,
    protected val journalføringService: JournalfoeringService,
    protected val oppgaveService: OppgaveService,
    protected val prosessinstansRepository: ProsessinstansRepository,
) : ComponentTestBase() {

    private val mockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(8094))

    private val processUUID = UUID.randomUUID()

    @BeforeEach
    fun before() {
        SakRepo.clear()
        mockServer.start()
        mockServer.stubFor(
            WireMock.post("/api/inngangsvilkaar").willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{ \"kvalifisererForEf883_2004\" : false, \"feilmeldinger\" : [] }")
            )
        )
        mockServer.stubFor(
            WireMock.post("/api/v1/mal/saksbehandlingstid_soknad/lag-pdf?somKopi=false&utkast=false").willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(ByteArray(0))
            )
        )
        ThreadLocalAccessInfo.beforeExecuteProcess(processUUID, "steg")
    }

    @AfterEach
    fun after() {
        ThreadLocalAccessInfo.afterExecuteProcess(processUUID)
        mockServer.stop()
    }

    protected fun journalførOgVentTilProsesserErFerdige(journalfoeringOpprettDto: JournalfoeringOpprettDto): Prosessinstans {
        val jfrOppgave: Oppgave = lagJfrOppgave()
        val lagJournalfoeringOpprettDto = lagJournalfoeringOpprettDto(jfrOppgave, journalfoeringOpprettDto)

        return executeAndWait(ProsessType.JFR_NY_SAK_BRUKER, listOf(ProsessType.OPPRETT_OG_DISTRIBUER_BREV)) {
            journalføringService.journalførOgOpprettSak(lagJournalfoeringOpprettDto)
            oppgaveService.ferdigstillOppgave(lagJournalfoeringOpprettDto.oppgaveID)
        }
    }

    protected fun executeAndWait(
        waitForprosessType: ProsessType,
        alsoWaitForprosessType: List<ProsessType> = listOf(),
        process: () -> Unit
    ): Prosessinstans {
        val startTime = LocalDateTime.now()
        process()
        val journalføringProsessID = finnProsessID(waitForprosessType, startTime)
        alsoWaitForprosessType.forEach { finnProsessID(it, startTime) }
        return prosessinstansRepository.findById(journalføringProsessID).get()
    }

    protected fun finnProsessID(prosessType: ProsessType, now: LocalDateTime): UUID =
        await.timeout(30, TimeUnit.SECONDS).untilNotNull {
            prosessinstansRepository.findAll()
                .find { it.registrertDato > now && it.type == prosessType && it.status == ProsessStatus.FERDIG }?.id
        }

    protected fun lagJfrOppgave(): Oppgave =
        testDataGenerator.opprettJfrOppgave(tilordnetRessurs = "Z123456", forVirksomhet = false)

    protected fun lagJournalfoeringOpprettDto(
        jfrOppgave: Oppgave,
        journalfoeringOpprettDto: JournalfoeringOpprettDto
    ): JournalfoeringOpprettDto {
        val hentJournalpost = journalføringService.hentJournalpost(jfrOppgave.journalpostId)
        return lagJournalføringDto(jfrOppgave, hentJournalpost!!.hoveddokument, journalfoeringOpprettDto)
    }

    private fun lagJournalføringDto(
        oppgave: Oppgave, dokument: ArkivDokument,
        journalfoeringOpprettDto: JournalfoeringOpprettDto
    ): JournalfoeringOpprettDto {
        return journalfoeringOpprettDto.apply {
            this.journalpostID = oppgave.journalpostId
            oppgaveID = oppgave.id.toString()
            hoveddokument = DokumentDto(dokument.dokumentId, dokument.tittel).apply {
                logiskeVedlegg = emptyList()
            }
        }.apply {
            behandlingstemaKode.shouldNotBeNull()
            behandlingstypeKode.shouldNotBeNull()
            fagsak.sakstype.shouldNotBeNull()
            fagsak.sakstema.shouldNotBeNull()
        }
    }

    protected fun defaultJournalføringDto() =
        JournalfoeringOpprettDto().apply {
            avsenderID = "30056928150"
            avsenderNavn = "KARAFFEL TRIVIELL"
            brukerID = "30056928150"
            virksomhetOrgnr = null
            vedlegg = emptyList()
            mottattDato = LocalDate.now()
            arbeidsgiverID = null
            representantID = null
            representantKontaktPerson = null
            representererKode = null
            isIkkeSendForvaltingsmelding = false
            avsenderType = Avsendertyper.PERSON
            isSkalTilordnes = true
            fagsak = FagsakDto().apply {
                soknadsperiode = PeriodeDto(
                    periodeFOM,
                    periodeTOM,
                )
                land = SoeknadslandDto(
                    listOf(Landkoder.IE.kode), false
                )
            }
        }

    protected fun lagJournalfoeringTilordneDto(
        saksnummer: String,
        jfrOppgave: Oppgave = lagJfrOppgave(),
        journalfoeringTilordneDto: JournalfoeringTilordneDto = defaultJournalfoeringTilordneDto()
    ): JournalfoeringTilordneDto {
        val hentJournalpost = journalføringService.hentJournalpost(jfrOppgave.journalpostId)
        return lagJournalfoeringTilordneDto(
            jfrOppgave, hentJournalpost!!.hoveddokument, saksnummer, journalfoeringTilordneDto
        )
    }

    private fun lagJournalfoeringTilordneDto(
        oppgave: Oppgave,
        dokument: ArkivDokument,
        saksnummer: String,
        journalfoeringTilordneDto: JournalfoeringTilordneDto
    ) =
        journalfoeringTilordneDto.apply {
            this.saksnummer = saksnummer
            this.journalpostID = oppgave.journalpostId
            oppgaveID = oppgave.id.toString()
            hoveddokument = DokumentDto(dokument.dokumentId, dokument.tittel).apply {
                logiskeVedlegg = emptyList()
            }
        }.apply {
            behandlingstemaKode.shouldNotBeNull()
            behandlingstypeKode.shouldNotBeNull()
        }

    protected fun defaultJournalfoeringTilordneDto() =
        JournalfoeringTilordneDto().apply {
            avsenderID = "30056928150"
            avsenderNavn = "KARAFFEL TRIVIELL"
            brukerID = "30056928150"
            virksomhetOrgnr = null
            vedlegg = emptyList()
            mottattDato = LocalDate.now()
            isIkkeSendForvaltingsmelding = false
            avsenderType = Avsendertyper.PERSON
            isSkalTilordnes = true
        }

    companion object {
        val periodeFOM = LocalDate.of(2001, 1, 1)
        val periodeTOM = LocalDate.of(2001, 1, 2)
    }
}
