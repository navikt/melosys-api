package no.nav.melosys.service.eessi

import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.eessi.ruting.SvarAnmodningUnntakSedRuter
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class SvarAnmodningUnntakSedRuterTest {

    @RelaxedMockK
    lateinit var prosessinstansService: ProsessinstansService

    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    @RelaxedMockK
    lateinit var anmodningsperiodeService: AnmodningsperiodeService

    @RelaxedMockK
    lateinit var oppgaveService: OppgaveService

    private lateinit var svarAnmodningUnntakSedRuter: SvarAnmodningUnntakSedRuter

    @BeforeEach
    fun setUp() {
        svarAnmodningUnntakSedRuter = SvarAnmodningUnntakSedRuter(
            prosessinstansService,
            fagsakService,
            anmodningsperiodeService,
            oppgaveService
        )
    }

    @Test
    fun `finnSakOgBestemRuting anmodningsperiodeUtenSvarFinnes verifiserKorrektResultat`() {
        val fagsak = hentFagsak(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingsstatus.ANMODNING_UNNTAK_SENDT)
        every { fagsakService.hentFagsakFraArkivsakID(any()) } returns fagsak
        every { anmodningsperiodeService.hentAnmodningsperioder(any()) } returns Collections.singleton(Anmodningsperiode())
        val prosessinstans = Prosessinstans()
        val eessiMelding = melosysEessiMelding()
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, eessiMelding)


        svarAnmodningUnntakSedRuter.rutSedTilBehandling(prosessinstans, 1L)


        verify {
            prosessinstansService.opprettProsessinstansMottattSvarAnmodningUnntak(
                fagsak.finnAktivBehandlingIkkeÅrsavregning(),
                eessiMelding
            )
        }
    }

    @Test
    fun `finnSakOgBestemRuting anmodningsperiodeSvarRegistrert verifiserKorrektResultat`() {
        val anmodningsperiode = Anmodningsperiode().apply {
            anmodningsperiodeSvar = AnmodningsperiodeSvar()
        }
        val fagsak = hentFagsak(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingsstatus.ANMODNING_UNNTAK_SENDT)
        every { fagsakService.hentFagsakFraArkivsakID(any()) } returns fagsak
        every { anmodningsperiodeService.hentAnmodningsperioder(any()) } returns Collections.singleton(anmodningsperiode)
        val prosessinstans = Prosessinstans()
        val eessiMelding = melosysEessiMelding()
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, eessiMelding)


        svarAnmodningUnntakSedRuter.rutSedTilBehandling(prosessinstans, 1L)


        verify {
            prosessinstansService.opprettProsessinstansSedJournalføring(
                fagsak.finnAktivBehandlingIkkeÅrsavregning(),
                eessiMelding
            )
        }
    }

    @Test
    fun `finnSakOgBestemRuting behandlingstypeFørstegangIkkeYrkesaktiv oppgaveOppdateres`() {
        val fagsak = hentFagsak(Behandlingstema.IKKE_YRKESAKTIV, Behandlingsstatus.ANMODNING_UNNTAK_SENDT)
        every { fagsakService.hentFagsakFraArkivsakID(any()) } returns fagsak
        val prosessinstans = Prosessinstans()
        val eessiMelding = melosysEessiMelding()
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, eessiMelding)
        every {
            oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(fagsak.saksnummer)
        } returns Optional.of(Oppgave.Builder().build())


        svarAnmodningUnntakSedRuter.rutSedTilBehandling(prosessinstans, 1L)


        verify { oppgaveService.oppdaterOppgave(any(), any<OppgaveOppdatering>()) }
        verify {
            prosessinstansService.opprettProsessinstansSedJournalføring(
                fagsak.finnAktivBehandlingIkkeÅrsavregning(),
                eessiMelding
            )
        }
    }

    @Test
    fun `finnSakOgBestemRuting ingenAnmodningsperiode forventException`() {
        val prosessinstans = Prosessinstans()
        val eessiMelding = melosysEessiMelding()
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, eessiMelding)
        every {
            fagsakService.hentFagsakFraArkivsakID(any())
        } returns hentFagsak(Behandlingstema.UTSENDT_SELVSTENDIG, Behandlingsstatus.FORELOEPIG_LOVVALG)
        every { anmodningsperiodeService.hentAnmodningsperioder(any()) } returns Collections.emptyList()


        val exception = org.junit.jupiter.api.assertThrows<FunksjonellException> {
            svarAnmodningUnntakSedRuter.rutSedTilBehandling(prosessinstans, 1L)
        }


        exception.message shouldContain "men behandlingen har ingen anmodningsperiode"
    }

    @Test
    fun `finnSakOgBestemRuting ingenTilhørendeArkivsak opprettJfrOppgave`() {
        val prosessinstans = Prosessinstans()
        val eessiMelding = melosysEessiMelding()
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, eessiMelding)


        svarAnmodningUnntakSedRuter.rutSedTilBehandling(prosessinstans, null)


        verify {
            oppgaveService.opprettJournalføringsoppgave(
                melosysEessiMelding().journalpostId,
                melosysEessiMelding().aktoerId
            )
        }
    }

    private fun hentFagsak(behandlingstema: Behandlingstema, behandlingsstatus: Behandlingsstatus): Fagsak {
        val behandling = Behandling.forTest {
            id = 123L
            tema = behandlingstema
            status = behandlingsstatus
        }

        val aktoer = Aktoer().apply {
            rolle = Aktoersroller.BRUKER
            aktørId = "223345325342"
        }

        return Fagsak.forTest {
            behandlinger(behandling)
            aktører(aktoer)
        }
    }

    private fun melosysEessiMelding(): MelosysEessiMelding = MelosysEessiMelding().apply {
        sedType = "A002"
        journalpostId = "test-journalpost-id"
        aktoerId = "test-aktoer-id"
    }
}
