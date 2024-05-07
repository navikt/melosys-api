package no.nav.melosys.saksflyt.steg.oppgave

import no.nav.melosys.domain.*
import no.nav.melosys.domain.FagsakTestFactory.builder
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.oppgave.OppgaveService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Set

@ExtendWith(MockitoExtension::class)
internal class OpprettAvgiftsoppgaveTest {
    @Mock
    private val behandlingsresultatService: BehandlingsresultatService? = null

    @Mock
    private val oppgaveService: OppgaveService? = null

    @Captor
    private val oppgave: ArgumentCaptor<Oppgave>? = null
    private var opprettAvgiftsoppgave: OpprettAvgiftsoppgave? = null
    private var behandlingsresultat: Behandlingsresultat? = null
    @BeforeEach
    fun setUp() {
        opprettAvgiftsoppgave = OpprettAvgiftsoppgave(behandlingsresultatService!!, oppgaveService!!)
        behandlingsresultat = Behandlingsresultat()
        behandlingsresultat!!.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        Mockito.`when`(behandlingsresultatService.hentBehandlingsresultat(ArgumentMatchers.anyLong()))
            .thenReturn(behandlingsresultat)
    }

    @Test
    fun utfør_oppretterRiktigOppgave() {
        behandlingsresultat!!.lovvalgsperioder.add(lagLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2))
        opprettAvgiftsoppgave!!.utfør(lagProsessinstans())
        Mockito.verify(oppgaveService).opprettOppgave(oppgave!!.capture())
        Assertions.assertThat(oppgave.value.tema).isEqualTo(Tema.TRY)
        Assertions.assertThat(oppgave.value.oppgavetype).isEqualTo(Oppgavetyper.VUR)
        Assertions.assertThat(oppgave.value.journalpostId).isNotBlank()
        Assertions.assertThat(oppgave.value.behandlesAvApplikasjon).isEqualTo(Fagsystem.INTET)
        Assertions.assertThat(oppgave.value.aktørId).isEqualTo(FagsakTestFactory.BRUKER_AKTØR_ID)
        Assertions.assertThat(oppgave.value.beskrivelse).isEqualTo(OpprettAvgiftsoppgave.AVGIFTSVURDERING_BESKRIVELSE)
    }

    @Test
    fun utfør_avslag_oppretterIkkeOppgave() {
        val lovvalgsperiode = lagLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1)
        lovvalgsperiode.innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
        behandlingsresultat!!.lovvalgsperioder = Set.of(lovvalgsperiode)
        Mockito.`when`(behandlingsresultatService!!.hentBehandlingsresultat(ArgumentMatchers.anyLong()))
            .thenReturn(behandlingsresultat)
        opprettAvgiftsoppgave!!.utfør(lagProsessinstans())
        Mockito.verify(oppgaveService, Mockito.never()).opprettOppgave(ArgumentMatchers.any())
    }

    @Test
    fun utfør_saktypeErftrl_oppretterIkkeOppgave() {
        val lovvalgsperiode = lagLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1)
        lovvalgsperiode.innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        behandlingsresultat!!.lovvalgsperioder = Set.of(lovvalgsperiode)
        Mockito.`when`(behandlingsresultatService!!.hentBehandlingsresultat(ArgumentMatchers.anyLong()))
            .thenReturn(behandlingsresultat)
        val prosessinstans = Prosessinstans()
        val fagsak = lagFagsak(Sakstyper.FTRL)
        prosessinstans.behandling = lagBehandling(fagsak)
        opprettAvgiftsoppgave!!.utfør(prosessinstans)
        Mockito.verify(oppgaveService, Mockito.never()).opprettOppgave(ArgumentMatchers.any())
    }

    @Test
    fun utfør_art13_oppretterIkkeOppgave() {
        behandlingsresultat!!.lovvalgsperioder.add(lagLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A))
        opprettAvgiftsoppgave!!.utfør(lagProsessinstans())
        Mockito.verify(oppgaveService, Mockito.never()).opprettOppgave(ArgumentMatchers.any())
    }

    companion object {
        private fun lagProsessinstans(): Prosessinstans {
            val prosessinstans = Prosessinstans()
            prosessinstans.behandling =
                lagBehandling(lagFagsak(Sakstyper.EU_EOS))
            return prosessinstans
        }

        private fun lagFagsak(sakstype: Sakstyper): Fagsak {
            return builder().type(sakstype).medBruker().build()
        }

        private fun lagBehandling(fagsak: Fagsak): Behandling {
            val behandling = Behandling()
            behandling.id = 1L
            behandling.type = Behandlingstyper.FØRSTEGANG
            behandling.fagsak = fagsak
            behandling.initierendeJournalpostId = "JOURNALPOSTID"
            return behandling
        }

        private fun lagLovvalgsperiode(bestemmelse: LovvalgBestemmelse): Lovvalgsperiode {
            val lovvalgsperiode = Lovvalgsperiode()
            lovvalgsperiode.bestemmelse = bestemmelse
            lovvalgsperiode.lovvalgsland = Land_iso2.NO
            lovvalgsperiode.innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            return lovvalgsperiode
        }
    }
}
