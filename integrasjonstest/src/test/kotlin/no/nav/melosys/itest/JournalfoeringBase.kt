package no.nav.melosys.itest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.melosys.AwaitUtil.awaitWithFailOnLogErrors
import no.nav.melosys.domain.arkiv.ArkivDokument
import no.nav.melosys.domain.kodeverk.Avsendertyper
import no.nav.melosys.domain.kodeverk.ForvaltningsmeldingMottaker
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.melosysmock.oppgave.Oppgave
import no.nav.melosys.melosysmock.sak.SakRepo
import no.nav.melosys.melosysmock.testdata.TestDataGenerator
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.felles.dto.SoeknadslandDto
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.journalforing.dto.*
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit

class JournalfoeringBase(
    protected val testDataGenerator: TestDataGenerator,
    protected val journalføringService: JournalfoeringService,
    protected val oppgaveService: OppgaveService,
    protected val prosessinstansRepository: ProsessinstansRepository,
) : ComponentTestBase() {

    protected val mockServer: WireMockServer =
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

    protected fun journalførOgVentTilProsesserErFerdige(
        journalfoeringOpprettDto: JournalfoeringOpprettDto,
        waitFor: ProsessType = ProsessType.JFR_NY_SAK_BRUKER,
        alsoWaitForprosessType: List<ProsessType> = listOf()
    ): Prosessinstans {
        val jfrOppgave: Oppgave = lagJfrOppgave()
        val lagJournalfoeringOpprettDto = lagJournalfoeringOpprettDto(jfrOppgave, journalfoeringOpprettDto)

        return executeAndWait(waitFor, alsoWaitForprosessType) {
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
        val journalføringProsessID = finnProsess(waitForprosessType, startTime)
        alsoWaitForprosessType.forEach { finnProsess(it, startTime) }
        return prosessinstansRepository.findById(journalføringProsessID).get()
    }

    protected fun finnProsess(prosessType: ProsessType, startTid: LocalDateTime): UUID {
        awaitWithFailOnLogErrors {
            pollDelay(1, TimeUnit.SECONDS)
                .timeout(30, TimeUnit.SECONDS)
                .untilNotNull {
                    prosessinstansRepository.findAllAfterDate(startTid)
                }.map { it.type }.shouldContain(prosessType)
        }

        return awaitWithFailOnLogErrors {
            timeout(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilNotNull {
                    prosessinstansRepository.findAllAfterDate(startTid)
                        .find { it.type == prosessType && it.status == ProsessStatus.FERDIG }?.id
                }
        }
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
            forvaltningsmeldingMottaker = ForvaltningsmeldingMottaker.BRUKER
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

    protected fun lagJournalfoeringOppgaveOgTilordneDto(
        saksnummer: String,
        journalfoeringTilordneDto: JournalfoeringTilordneDto = defaultJournalfoeringTilordneDto()
    ): JournalfoeringTilordneDto {
        val jfrOppgave: Oppgave = lagJfrOppgave()
        val hentJournalpost = journalføringService.hentJournalpost(jfrOppgave.journalpostId)
        return journalfoeringTilordneDto
            .apply {
                this.saksnummer = saksnummer
                journalpostID = jfrOppgave.journalpostId
                oppgaveID = jfrOppgave.id.toString()
                hoveddokument = DokumentDto(hentJournalpost!!.hoveddokument.dokumentId, hentJournalpost.hoveddokument.tittel).apply {
                    logiskeVedlegg = emptyList()
                }
            }.apply {
                behandlingstemaKode.shouldNotBeNull()
                behandlingstypeKode.shouldNotBeNull()
            }
    }

    protected fun defaultJournalfoeringTilordneDto() =
        JournalfoeringTilordneDto().apply {
            avsenderID = "30056928150"
            avsenderNavn = "KARAFFEL TRIVIELL"
            brukerID = "30056928150"
            virksomhetOrgnr = null
            vedlegg = emptyList()
            mottattDato = LocalDate.now()
            forvaltningsmeldingMottaker = ForvaltningsmeldingMottaker.BRUKER
            avsenderType = Avsendertyper.PERSON
            isSkalTilordnes = true
        }

    companion object {
        val periodeFOM = LocalDate.of(2001, 1, 1)
        val periodeTOM = LocalDate.of(2001, 1, 2)
    }
}
