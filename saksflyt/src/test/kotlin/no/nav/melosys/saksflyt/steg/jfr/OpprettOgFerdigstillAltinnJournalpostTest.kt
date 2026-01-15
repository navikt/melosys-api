package no.nav.melosys.saksflyt.steg.jfr

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.arkiv.BrukerIdType
import no.nav.melosys.domain.arkiv.OpprettJournalpost
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.mottatteOpplysninger
import no.nav.melosys.domain.msm.AltinnDokument
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.altinn.AltinnSoeknadService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataFasade
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

private const val SOKNAD_ID = "soknadid1"
private const val BRUKER_AKTOR_ID = "3321231"
private const val IDENT = "00000000000"

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

    private val capturedJournalpost = slot<OpprettJournalpost>()

    @BeforeEach
    fun setup() {
        opprettOgFerdigstillAltinnJournalpost = OpprettOgFerdigstillAltinnJournalpost(
            altinnSoeknadService, behandlingService, eregFasade, joarkFasade, persondataFasade
        )

        val søknadDokument = AltinnDokument(
            SOKNAD_ID, "dokumentid1", "tittel1",
            AltinnDokument.AltinnDokumentType.SOKNAD.name, "pdf", Instant.now()
        )
        val fullmaktDokument = AltinnDokument(
            SOKNAD_ID, "dokumentid2", "tittel2",
            AltinnDokument.AltinnDokumentType.FULLMAKT.name, "pdf", Instant.now()
        )

        val dokumenter = mutableListOf(søknadDokument, fullmaktDokument)

        every { altinnSoeknadService.hentDokumenterTilknyttetSoknad(SOKNAD_ID) } returns dokumenter
        every { persondataFasade.hentFolkeregisterident(any()) } returns IDENT
        every { eregFasade.hentOrganisasjonNavn(any()) } returns "Fullmektig Avsender"
        every { joarkFasade.opprettJournalpost(any(), any()) } returns "journalpostid123"
        every { behandlingService.lagre(any()) } just Runs
    }

    @Test
    fun `utfør journalpostBlirOpprettet verifiser`() {
        val prosessinstans = lagProsessinstans {
            gsakSaksnummer = FagsakTestFactory.GSAK_SAKSNUMMER
            medBruker { aktørId = BRUKER_AKTOR_ID }
            medFullmektig {
                orgnr = "fullmektigOrgnr"
                setFullmaktstyper(listOf(Fullmaktstype.FULLMEKTIG_SØKNAD, Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER))
            }
        }
        val behandling = prosessinstans.behandling!!

        opprettOgFerdigstillAltinnJournalpost.utfør(prosessinstans)

        verify { persondataFasade.hentFolkeregisterident(any()) }
        verify { joarkFasade.opprettJournalpost(capture(capturedJournalpost), eq(true)) }
        verify { behandlingService.lagre(behandling) }

        val opprettJournalpost = capturedJournalpost.captured
        run {
            opprettJournalpost.tema shouldBe "MED"
            opprettJournalpost.mottaksKanal shouldBe "ALTINN"
            opprettJournalpost.saksnummer shouldBe FagsakTestFactory.SAKSNUMMER
            opprettJournalpost.brukerId shouldBe IDENT
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
        val arbeidsgiverOrgnr = "arbOrgnr"
        val prosessinstans = lagProsessinstans {
            gsakSaksnummer = FagsakTestFactory.GSAK_SAKSNUMMER
            medBruker { aktørId = BRUKER_AKTOR_ID }
            medArbeidsgiver { orgnr = arbeidsgiverOrgnr }
        }
        val behandling = prosessinstans.behandling!!

        every { eregFasade.hentOrganisasjonNavn(arbeidsgiverOrgnr) } returns "Arbeidsgiver"

        opprettOgFerdigstillAltinnJournalpost.utfør(prosessinstans)

        verify { persondataFasade.hentFolkeregisterident(any()) }
        verify { joarkFasade.opprettJournalpost(capture(capturedJournalpost), eq(true)) }
        verify { behandlingService.lagre(behandling) }

        val opprettJournalpost = capturedJournalpost.captured
        opprettJournalpost.korrespondansepartNavn shouldBe "Arbeidsgiver"
    }

    private fun lagProsessinstans(
        fagsakInit: FagsakTestFactory.Builder.() -> Unit = {}
    ) = Prosessinstans.forTest {
        medData(ProsessDataKey.MOTTATT_SOKNAD_ID, SOKNAD_ID)
        behandling {
            fagsak(fagsakInit)
            mottatteOpplysninger {
                originalData = "Original Can't Touch This"
            }
        }
    }
}
