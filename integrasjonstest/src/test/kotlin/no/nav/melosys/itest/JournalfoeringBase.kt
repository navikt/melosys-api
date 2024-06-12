package no.nav.melosys.itest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.Extension
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.melosys.ProsessinstansTestManager
import no.nav.melosys.domain.arkiv.ArkivDokument
import no.nav.melosys.domain.kodeverk.Avsendertyper
import no.nav.melosys.domain.kodeverk.ForvaltningsmeldingMottaker
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.melosysmock.oppgave.Oppgave
import no.nav.melosys.melosysmock.sak.SakRepo
import no.nav.melosys.melosysmock.testdata.TestDataGenerator
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.felles.dto.SoeknadslandDto
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.journalforing.dto.*
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.*

class JournalfoeringBase(
    protected val testDataGenerator: TestDataGenerator,
    protected val journalføringService: JournalfoeringService,
    protected val oppgaveService: OppgaveService,
    extensionForWireMock: Extension? = null
) : ComponentTestBase() {

    protected val mockServer: WireMockServer =
        WireMockServer(
            if (extensionForWireMock == null) WireMockConfiguration.options().port(8094) else
                WireMockConfiguration
                    .options().extensions(extensionForWireMock)
                    .port(8094)
        )

    private val processUUID = UUID.randomUUID()

    @Autowired
    private lateinit var prosessinstansTestManager: ProsessinstansTestManager

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
            WireMock.post(WireMock.urlPathMatching("/api/v1/mal/.*/lag-pdf"))
                .withQueryParam("somKopi", equalTo("false"))
                .withQueryParam("utkast", equalTo("false"))
                .willReturn(
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
        prosessinstansTestManager.clear()
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
        count: Int = 0,
        process: () -> Unit
    ): Prosessinstans = prosessinstansTestManager.executeAndWait(waitForprosessType, alsoWaitForprosessType, count, process)

    protected fun lagJfrOppgave(): Oppgave =
        testDataGenerator.opprettJfrOppgave(tilordnetRessurs = "Z123456", forVirksomhet = false)

    protected fun lagJournalfoeringOpprettDto(
        jfrOppgave: Oppgave,
        journalfoeringOpprettDto: JournalfoeringOpprettDto
    ): JournalfoeringOpprettDto {

        val hentJournalpost = if (jfrOppgave.journalpostId != null) journalføringService.hentJournalpost(jfrOppgave.journalpostId) else null
        return lagJournalføringDto(jfrOppgave, hentJournalpost?.hoveddokument, journalfoeringOpprettDto)
    }

    private fun lagJournalføringDto(
        oppgave: Oppgave,
        dokument: ArkivDokument?,
        journalfoeringOpprettDto: JournalfoeringOpprettDto
    ): JournalfoeringOpprettDto {
        return journalfoeringOpprettDto.apply {
            this.journalpostID = oppgave.journalpostId
            oppgaveID = oppgave.id.toString()
            hoveddokument = DokumentDto(dokument?.dokumentId, dokument?.tittel).apply {
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
        val hentJournalpost = if (jfrOppgave.journalpostId != null) journalføringService.hentJournalpost(jfrOppgave.journalpostId) else null
        return journalfoeringTilordneDto
            .apply {
                this.saksnummer = saksnummer
                journalpostID = jfrOppgave.journalpostId
                oppgaveID = jfrOppgave.id.toString()
                hoveddokument = DokumentDto(hentJournalpost?.hoveddokument?.dokumentId, hentJournalpost?.hoveddokument?.tittel).apply {
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
