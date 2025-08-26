package no.nav.melosys.saksflyt.steg.jfr

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.arkiv.BrukerIdType
import no.nav.melosys.domain.arkiv.OpprettJournalpost
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.msm.AltinnDokument
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.altinn.AltinnSoeknadService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataFasade
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
class OpprettOgFerdigstillAltinnJournalpostTest {
    @MockK
    private lateinit var altinnSoeknadService: AltinnSoeknadService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var eregFasade: EregFasade

    @MockK
    private lateinit var joarkFasade: JoarkFasade

    @MockK
    private lateinit var persondataFasade: PersondataFasade

    private lateinit var opprettOgFerdigstillAltinnJournalpost: OpprettOgFerdigstillAltinnJournalpost

    private val prosessinstans = Prosessinstans.forTest()
    private val behandling = lagBehandling()

    private val bruker = Aktoer().apply {
        rolle = Aktoersroller.BRUKER
        aktørId = "3321231"
    }
    private val ident = "00000000000"

    private val capturedJournalpost = slot<OpprettJournalpost>()

    @BeforeEach
    fun setup() {
        val søknadID = "soknadid1"
        prosessinstans.setData(ProsessDataKey.MOTTATT_SOKNAD_ID, søknadID)

        opprettOgFerdigstillAltinnJournalpost = OpprettOgFerdigstillAltinnJournalpost(
            altinnSoeknadService, behandlingService, eregFasade, joarkFasade, persondataFasade
        )

        val søknadDokument = AltinnDokument(
            søknadID, "dokumentid1", "tittel1",
            AltinnDokument.AltinnDokumentType.SOKNAD.name, "pdf", Instant.now()
        )
        val fullmaktDokument = AltinnDokument(
            søknadID, "dokumentid2", "tittel2",
            AltinnDokument.AltinnDokumentType.FULLMAKT.name, "pdf", Instant.now()
        )

        prosessinstans.behandling = behandling

        val dokumenter = arrayListOf<AltinnDokument>().apply {
            add(søknadDokument)
            add(fullmaktDokument)
        }

        every { altinnSoeknadService.hentDokumenterTilknyttetSoknad(søknadID) } returns dokumenter
        every { persondataFasade.hentFolkeregisterident(any()) } returns ident
        every { eregFasade.hentOrganisasjonNavn(any()) } returns "Fullmektig Avsender"
        every { joarkFasade.opprettJournalpost(any(), any()) } returns "journalpostid123"
        every { behandlingService.lagre(any()) } just Runs
    }

    @Test
    fun `utfør journalpostBlirOpprettet verifiser`() {
        val fullmektig = Aktoer().apply {
            rolle = Aktoersroller.FULLMEKTIG
            orgnr = "fullmektigOrgnr"
            setFullmaktstyper(listOf(Fullmaktstype.FULLMEKTIG_SØKNAD, Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER))
        }
        behandling.fagsak.leggTilAktør(bruker)
        behandling.fagsak.leggTilAktør(fullmektig)


        opprettOgFerdigstillAltinnJournalpost.utfør(prosessinstans)


        verify { persondataFasade.hentFolkeregisterident(any()) }
        verify { joarkFasade.opprettJournalpost(capture(capturedJournalpost), eq(true)) }
        verify { behandlingService.lagre(behandling) }

        val opprettJournalpost = capturedJournalpost.captured
        run {
            opprettJournalpost.tema shouldBe "MED"
            opprettJournalpost.mottaksKanal shouldBe "ALTINN"
            opprettJournalpost.saksnummer shouldBe FagsakTestFactory.SAKSNUMMER
            opprettJournalpost.brukerId shouldBe ident
            opprettJournalpost.brukerIdType shouldBe BrukerIdType.FOLKEREGISTERIDENT
        }
        opprettJournalpost.innhold shouldNotBe null
        run {
            opprettJournalpost.hoveddokument.dokumentId shouldBe null
            opprettJournalpost.hoveddokument.tittel shouldBe "Søknad om A1 for utsendte arbeidstakere i EØS/Sveits"
        }
        opprettJournalpost.vedlegg shouldHaveSize 1
        opprettJournalpost.vedlegg.map { it.tittel } shouldContainExactly listOf("Fullmakt")
        opprettJournalpost.korrespondansepartNavn shouldBe "Fullmektig Avsender"
    }

    @Test
    fun `utfør ingenFullmektigForBruker avsenderNavnErArbeidsgiverOrganisasjonNavn`() {
        val arbeidsgiver = Aktoer().apply {
            rolle = Aktoersroller.ARBEIDSGIVER
            orgnr = "arbOrgnr"
        }
        behandling.fagsak.leggTilAktør(bruker)
        behandling.fagsak.leggTilAktør(arbeidsgiver)

        every { eregFasade.hentOrganisasjonNavn(arbeidsgiver.orgnr) } returns "Arbeidsgiver"


        opprettOgFerdigstillAltinnJournalpost.utfør(prosessinstans)


        verify { persondataFasade.hentFolkeregisterident(any()) }
        verify { joarkFasade.opprettJournalpost(capture(capturedJournalpost), eq(true)) }
        verify { behandlingService.lagre(behandling) }

        val opprettJournalpost = capturedJournalpost.captured
        opprettJournalpost.korrespondansepartNavn shouldBe "Arbeidsgiver"
    }

    private fun lagBehandling() = Behandling.forTest {
        fagsak {
            gsakSaksnummer = FagsakTestFactory.GSAK_SAKSNUMMER
        }
        mottatteOpplysninger = MottatteOpplysninger().apply {
            originalData = "Original Can't Touch This"
        }
    }
}
