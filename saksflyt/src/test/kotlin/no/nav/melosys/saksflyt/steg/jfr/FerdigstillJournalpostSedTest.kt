package no.nav.melosys.saksflyt.steg.jfr

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.arkiv.Journalposttype
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.oppgave.OppgaveFactory
import no.nav.melosys.service.persondata.PersondataFasade
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class FerdigstillJournalpostSedTest {
    @MockK
    private lateinit var joarkFasade: JoarkFasade

    @MockK
    private lateinit var persondataFasade: PersondataFasade

    @MockK
    private lateinit var eessiService: EessiService

    private lateinit var ferdigstillJournalpostSed: FerdigstillJournalpostSed

    private val oppgaveFactory = OppgaveFactory()

    @BeforeEach
    fun setUp() {
        every { joarkFasade.oppdaterOgFerdigstillJournalpost(any(), any()) } just Runs
        every { eessiService.opprettJournalpostForTidligereSed(any()) } just Runs

        ferdigstillJournalpostSed = FerdigstillJournalpostSed(joarkFasade, persondataFasade, oppgaveFactory, eessiService)
    }

    @Test
    fun `utfør skal oppdatere journalpost når journalpost ikke er ferdigstilt`() {
        every { persondataFasade.hentFolkeregisterident(FagsakTestFactory.BRUKER_AKTØR_ID) } returns BRUKER_ID
        val journalpost = Journalpost(JOURNALPOST_ID).apply {
            setErFerdigstilt(false)
            journalposttype = Journalposttype.INN
        }
        every { joarkFasade.hentJournalpost(JOURNALPOST_ID) } returns journalpost

        val prosessinstans = hentProsessinstans()


        ferdigstillJournalpostSed.utfør(prosessinstans)


        val forventetOppdatering = JournalpostOppdatering.Builder()
            .medBrukerID(BRUKER_ID)
            .medSaksnummer(FagsakTestFactory.SAKSNUMMER)
            .medTittel(TITTEL)
            .build()
        verify { joarkFasade.oppdaterOgFerdigstillJournalpost(JOURNALPOST_ID, forventetOppdatering) }
    }

    @Test
    fun `utfør skal oppdatere journalpost når journalpost ikke er ferdigstilt og bruke fagsak tema`() {
        every { persondataFasade.hentFolkeregisterident(FagsakTestFactory.BRUKER_AKTØR_ID) } returns BRUKER_ID
        val journalpost = Journalpost(JOURNALPOST_ID).apply {
            setErFerdigstilt(false)
            journalposttype = Journalposttype.INN
        }
        every { joarkFasade.hentJournalpost(JOURNALPOST_ID) } returns journalpost

        val prosessinstans = hentProsessinstans()


        ferdigstillJournalpostSed.utfør(prosessinstans)


        val forventetOppdatering = JournalpostOppdatering.Builder()
            .medBrukerID(BRUKER_ID)
            .medSaksnummer(FagsakTestFactory.SAKSNUMMER)
            .medTittel(TITTEL)
            .build()
        verify { joarkFasade.oppdaterOgFerdigstillJournalpost(JOURNALPOST_ID, forventetOppdatering) }
    }

    @Test
    fun `utfør skal ikke gjøre noe når journalpost er ferdigstilt`() {
        val journalpost = Journalpost(JOURNALPOST_ID).apply {
            isErFerdigstilt = true
            journalposttype = Journalposttype.INN
        }
        every { joarkFasade.hentJournalpost(JOURNALPOST_ID) } returns journalpost

        val prosessinstans = hentProsessinstans()


        ferdigstillJournalpostSed.utfør(prosessinstans)


        verify { joarkFasade.hentJournalpost(JOURNALPOST_ID) }
        verify(exactly = 0) { joarkFasade.oppdaterOgFerdigstillJournalpost(any(), any()) }
    }

    @Test
    fun `utfør skal ikke gjøre noe når journalpost er utgått`() {
        val journalpost = Journalpost(JOURNALPOST_ID).apply {
            isErUtgått = true
            journalposttype = Journalposttype.INN
        }
        every { joarkFasade.hentJournalpost(JOURNALPOST_ID) } returns journalpost

        val prosessinstans = hentProsessinstans()


        ferdigstillJournalpostSed.utfør(prosessinstans)


        verify { joarkFasade.hentJournalpost(JOURNALPOST_ID) }
        verify(exactly = 0) { joarkFasade.oppdaterOgFerdigstillJournalpost(any(), any()) }
    }

    private fun hentProsessinstans() = Prosessinstans.forTest {
        behandling {
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            type = Behandlingstyper.FØRSTEGANG
            fagsak {
                gsakSaksnummer = FagsakTestFactory.GSAK_SAKSNUMMER
                tema = Sakstemaer.TRYGDEAVGIFT
                medBruker()
            }
        }
        medData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding().apply {
            journalpostId = JOURNALPOST_ID
        })
        medData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, TITTEL)
        medBehandling(behandling)
    }

    companion object {
        private const val JOURNALPOST_ID = "jp123"
        private const val BRUKER_ID = "bruker123"
        private const val TITTEL = "tittel123"
    }
}
