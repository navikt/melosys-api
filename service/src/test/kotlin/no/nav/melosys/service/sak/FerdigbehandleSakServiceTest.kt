package no.nav.melosys.service.sak

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.FagsakTestFactory.BEHANDLING_ID
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class FerdigbehandleSakServiceTest {

    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    @RelaxedMockK
    lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var oppgaveService: OppgaveService

    @RelaxedMockK
    lateinit var prosessinstansService: ProsessinstansService

    private lateinit var ferdigbehandleSakService: FerdigbehandleSakService

    @BeforeEach
    fun setup() {
        ferdigbehandleSakService = FerdigbehandleSakService(fagsakService, behandlingService, behandlingsresultatService, oppgaveService)
    }

    @Test
    fun `ferdigbehandle med kun en behandling setter behandlingsresultat til FERDIGBEHANDLET og avslutter fagsak`() {
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandlingID: Long = BEHANDLING_ID
        val behandling = Behandling().apply {
            id = behandlingID
            this.fagsak = fagsak
        }

        fagsak.behandlinger.add(behandling)

        every { behandlingService.hentBehandling(behandlingID) } returns behandling
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak


        ferdigbehandleSakService.ferdigbehandle(BEHANDLING_ID)


        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.AVSLUTTET) }
        verify { behandlingsresultatService.oppdaterBehandlingsresultattype(behandling.id, Behandlingsresultattyper.FERDIGBEHANDLET) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(behandlingID) }
    }

    @Test
    fun `ferdigbehandle med flere behandlinger setter behandlingsresultat til FERDIGBEHANDLET og endrer ikke fagsak`() {
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandlingID: Long = BEHANDLING_ID
        val førstegangsBehandling = Behandling().apply {
            id = 123
            this.fagsak = fagsak
        }
        val annengangsBehandling = Behandling().apply {
            id = behandlingID
            this.fagsak = fagsak
        }

        fagsak.behandlinger.add(førstegangsBehandling)
        fagsak.behandlinger.add(annengangsBehandling)

        every { behandlingService.hentBehandling(behandlingID) } returns annengangsBehandling
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak


        ferdigbehandleSakService.ferdigbehandle(BEHANDLING_ID)


        verify(exactly = 0) { fagsakService.avsluttFagsakOgBehandling(fagsak, any(), any()) }
        verify { behandlingsresultatService.oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.FERDIGBEHANDLET) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(behandlingID) }
    }

    @Test
    fun `ferdigbehandle med årsavregning tømmer behandlingsresultat`() {
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandlingID: Long = BEHANDLING_ID
        val behandling = Behandling().apply {
            id = behandlingID
            type = Behandlingstyper.ÅRSAVREGNING
            this.fagsak = fagsak
        }

        fagsak.behandlinger.add(behandling)

        every { behandlingService.hentBehandling(behandlingID) } returns behandling
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak


        ferdigbehandleSakService.ferdigbehandle(BEHANDLING_ID)

        verify { behandlingsresultatService.tømBehandlingsresultat(BEHANDLING_ID) }
    }

    @Test
    fun `ferdigbehandle ikke årsavregning tømmer ikke behandlingsresultat`() {
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandlingID: Long = BEHANDLING_ID
        val behandling = Behandling().apply {
            id = behandlingID
            type = Behandlingstyper.FØRSTEGANG
            this.fagsak = fagsak
        }

        fagsak.behandlinger.add(behandling)

        every { behandlingService.hentBehandling(behandlingID) } returns behandling
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak


        ferdigbehandleSakService.ferdigbehandle(BEHANDLING_ID)

        verify(exactly = 0) { behandlingsresultatService.tømBehandlingsresultat(any()) }
    }
}
