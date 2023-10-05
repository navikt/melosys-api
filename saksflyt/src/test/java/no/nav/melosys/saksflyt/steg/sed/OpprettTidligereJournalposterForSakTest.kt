package no.nav.melosys.saksflyt.steg.sed;

import io.getunleash.FakeUnleash
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.saksflyt.ProsessDataKey
import no.nav.melosys.domain.saksflyt.Prosessinstans
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.service.dokument.sed.EessiService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class OpprettTidligereJournalposterForSakTest {

    private val eessiService: EessiService = mockk()
    private val joarkFasade: JoarkFasade = mockk()

    private lateinit var opprettTidligereJournalposterForSak: OpprettTidligereJournalposterForSak

    private val JOURNALPOST_ID = "123"

    @BeforeEach
    fun setup() {
        opprettTidligereJournalposterForSak = OpprettTidligereJournalposterForSak(joarkFasade, eessiService)
    }

    @Test
    fun utfør_journalpostUtenEessiMelding_verifiserOpprettJournalposter() {
        val prosessinstans = Prosessinstans()
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, JOURNALPOST_ID)

        val fagsak = Fagsak()
        fagsak.gsakSaksnummer = 123L
        val behandling = Behandling()
        behandling.fagsak = fagsak
        prosessinstans.behandling = behandling

        val journalpost = Journalpost(JOURNALPOST_ID)
        journalpost.mottaksKanal = "EESSI"
        every { joarkFasade.hentJournalpost(JOURNALPOST_ID) } returns journalpost

        val melosysEessiMelding = MelosysEessiMelding()
        melosysEessiMelding.bucType = "LA_BUC_04"
        melosysEessiMelding.rinaSaksnummer = "321323"
        every { eessiService.hentSedTilknyttetJournalpost(JOURNALPOST_ID) } returns melosysEessiMelding
        every { eessiService.opprettJournalpostForTidligereSed(any()) } returns Unit

        opprettTidligereJournalposterForSak.utfør(prosessinstans)

        verify(exactly = 1) {
            eessiService.opprettJournalpostForTidligereSed(melosysEessiMelding.rinaSaksnummer)
        }
    }

    @Test
    fun utfør_journalpostMedEessimelding_verifiserOpprettJournalposter() {
        val melosysEessiMelding = MelosysEessiMelding()
        melosysEessiMelding.bucType = "LA_BUC_04"
        melosysEessiMelding.rinaSaksnummer = "321323"
        melosysEessiMelding.journalpostId = JOURNALPOST_ID

        val prosessinstans = Prosessinstans()
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding)

        val fagsak = Fagsak()
        fagsak.gsakSaksnummer = 123L
        val behandling = Behandling()
        behandling.fagsak = fagsak
        prosessinstans.behandling = behandling

        every { eessiService.opprettJournalpostForTidligereSed(any()) } returns Unit

        opprettTidligereJournalposterForSak.utfør(prosessinstans)

        verify(exactly = 1) {
            eessiService.opprettJournalpostForTidligereSed(melosysEessiMelding.rinaSaksnummer)
        }
    }
}
