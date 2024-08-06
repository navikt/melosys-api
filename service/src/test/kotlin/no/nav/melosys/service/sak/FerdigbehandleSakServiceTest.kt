package no.nav.melosys.service.sak

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class FerdigbehandleSakServiceTest {

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var oppgaveService: OppgaveService

    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    @RelaxedMockK
    lateinit var prosessinstansService: ProsessinstansService

    private lateinit var ferdigbehandleSakService: FerdigbehandleSakService

    @BeforeEach
    fun setup() {
        ferdigbehandleSakService = FerdigbehandleSakService(fagsakService, behandlingsresultatService, oppgaveService)
    }

    @Test
    fun ferdigbehandleSak_saksstatusOPPRETTET_lagrerKorrekt() {
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandlingID : Long = 1
        val behandling = Behandling().apply {
            id = behandlingID
            this.fagsak = fagsak
        }
        fagsak.behandlinger.add(behandling)
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak


        ferdigbehandleSakService.ferdigbehandleSak(FagsakTestFactory.SAKSNUMMER)


        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.AVSLUTTET) }
        verify { behandlingsresultatService.oppdaterBehandlingsresultattype(behandling.id, Behandlingsresultattyper.FERDIGBEHANDLET) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(behandlingID) }
    }

    @Test
    fun ferdigbehandleSak_saksstatusAnnetEnnOPPRETTET_lagrerKorrekt() {
        val fagsak = FagsakTestFactory.builder().apply {
            status = Saksstatuser.LOVVALG_AVKLART
        }.build()
        val behandlingID: Long = 1
        val behandling = Behandling().apply {
            id = behandlingID
            this.fagsak = fagsak

        }
        fagsak.behandlinger.add(behandling)
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak


        ferdigbehandleSakService.ferdigbehandleSak(FagsakTestFactory.SAKSNUMMER)


        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.LOVVALG_AVKLART) }
        verify { behandlingsresultatService.oppdaterBehandlingsresultattype(behandlingID, Behandlingsresultattyper.FERDIGBEHANDLET) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(behandlingID) }
    }
}
