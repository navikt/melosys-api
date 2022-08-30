package no.nav.melosys.itest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.arkiv.ArkivDokument
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode
import no.nav.melosys.domain.kodeverk.Avsendertyper
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.saksflyt.ProsessStatus
import no.nav.melosys.domain.saksflyt.ProsessType
import no.nav.melosys.melosysmock.oppgave.Oppgave
import no.nav.melosys.melosysmock.sak.SakRepo
import no.nav.melosys.melosysmock.testdata.TestDataGenerator
import no.nav.melosys.repository.ProsessinstansRepository
import no.nav.melosys.service.felles.dto.SoeknadslandDto
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.journalforing.dto.DokumentDto
import no.nav.melosys.service.journalforing.dto.FagsakDto
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto
import no.nav.melosys.service.journalforing.dto.PeriodeDto
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull

@Import(KodeverkStub::class)
class JournalfoeringIT(
    @Autowired private val testDataGenerator: TestDataGenerator,
    @Autowired private val journalføringService: JournalfoeringService,
    @Autowired private val oppgaveService: OppgaveService,
    @Autowired private val prosessinstansRepository: ProsessinstansRepository
) : ComponentTestBase() {

    private val mockServer: WireMockServer =
        WireMockServer(WireMockConfiguration.wireMockConfig().port(8094))

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
    }

    @AfterEach
    fun after() {
        mockServer.stop()
    }

    @Test
    fun journalførOgOpprettSak_EU_EOS_prosesserKjørerAlleSteg() {
        val jfrOppgave: Oppgave = lagJfrOppgave()
        val now = LocalDateTime.now()
        val journalfoeringOpprettDto = lagJournalfoeringOpprettDto(jfrOppgave)

        ThreadLocalAccessInfo.executeProcess("journalførOgOpprettSak") {
            journalføringService.journalførOgOpprettSak(journalfoeringOpprettDto)
            oppgaveService.ferdigstillOppgave(journalfoeringOpprettDto.oppgaveID)
        }
        val prossesId = finnProssesID(ProsessType.JFR_NY_SAK_BRUKER, now)
        listOf(
            prossesId,
            finnProssesID(ProsessType.OPPRETT_OG_DISTRIBUER_BREV, now)
        ).forEach {
            sjekkAtProssessHarStatusFerdig(it)
        }
        val prosessinstans = prosessinstansRepository.findById(prossesId).get()
        val behandling = prosessinstans.behandling

        behandling.apply {
            status.shouldBe(Behandlingsstatus.OPPRETTET)
            type.shouldBe(Behandlingstyper.SOEKNAD)
            tema.shouldBe(Behandlingstema.UTSENDT_ARBEIDSTAKER)
        }
        behandling.fagsak.apply {
            type.shouldBe(Sakstyper.EU_EOS)
            status.shouldBe(Saksstatuser.OPPRETTET)
            registrertAv.shouldBe("MELOSYS")
            tema.shouldBe(Sakstemaer.MEDLEMSKAP_LOVVALG)
        }
        behandling.behandlingsgrunnlag.behandlingsgrunnlagdata.shouldBeInstanceOf<Soeknad>()
            .shouldBeEqualToComparingFields(Soeknad().apply {
                soeknadsland.apply {
                    landkoder = listOf(Landkoder.IE.kode)
                    erUkjenteEllerAlleEosLand = false
                }
                periode = Periode(
                    periodeFOM,
                    periodeFOM
                )
            })
    }

    private fun lagJournalfoeringOpprettDto(jfrOppgave: Oppgave): JournalfoeringOpprettDto {
        var hentJournalpost: Journalpost? = null
        ThreadLocalAccessInfo.executeProcess("journalførOgOpprettSak") {
            hentJournalpost = journalføringService.hentJournalpost(jfrOppgave.journalpostId)
        }
        return lagJournalføringDto(jfrOppgave, hentJournalpost!!.hoveddokument)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun sjekkAtProssessHarStatusFerdig(prosessID: UUID) =
        await.until {
            prosessinstansRepository.findById(prosessID)
                .getOrNull()?.status == ProsessStatus.FERDIG
        }

    private fun finnProssesID(prosessType: ProsessType, now: LocalDateTime): UUID =
        await.timeout(30, TimeUnit.SECONDS).untilNotNull {
            prosessinstansRepository.findAll()
                .find { it.registrertDato > now && it.type == prosessType }?.id
        }

    private fun lagJfrOppgave(): Oppgave =
        testDataGenerator.opprettJfrOppgave(tilordnetRessurs = "Z123456", forVirksomhet = false)

    private fun lagJournalføringDto(oppgave: Oppgave, dokument: ArkivDokument) =
        JournalfoeringOpprettDto().apply {
            this.journalpostID = oppgave.journalpostId
            avsenderID = "30056928150"
            avsenderNavn = "KARAFFEL TRIVIELL"
            brukerID = "30056928150"
            virksomhetOrgnr = null
            oppgaveID = oppgave.id.toString()
            vedlegg = emptyList()
            mottattDato = LocalDate.now()
            arbeidsgiverID = null
            behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
            representantID = null
            representantKontaktPerson = null
            representererKode = null
            isIkkeSendForvaltingsmelding = false
            avsenderType = Avsendertyper.PERSON
            isSkalTilordnes = true
            fagsak = FagsakDto().apply {
                sakstype = Sakstyper.EU_EOS.kode
                soknadsperiode = PeriodeDto(
                    periodeFOM,
                    periodeTOM,
                )
                land = SoeknadslandDto(
                    listOf(Landkoder.IE.kode), false
                )
            }
            hoveddokument = DokumentDto(dokument.dokumentId, dokument.tittel).apply {
                logiskeVedlegg = emptyList()
            }
        }

    companion object {
        val periodeFOM = LocalDate.of(2001, 1, 1)
        val periodeTOM = LocalDate.of(2001, 1, 2)
    }
}
