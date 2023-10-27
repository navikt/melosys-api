package no.nav.melosys.service.sak

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.saksflyt.ProsessinstansService
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
    private val SAKSNUMMER = "MEL-1"

    @BeforeEach
    fun setup() {
        ferdigbehandleSakService = FerdigbehandleSakService(fagsakService, behandlingsresultatService, oppgaveService, prosessinstansService)
    }

    @Test
    fun ferdigbehandleSak_saksstatusOPPRETTET_lagrerKorrekt() {
        val fagsak = Fagsak().apply { status = Saksstatuser.OPPRETTET }
        val behandling = Behandling().apply { this.fagsak = fagsak }
        fagsak.behandlinger.add(behandling)
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns fagsak


        ferdigbehandleSakService.ferdigbehandleSak(SAKSNUMMER)


        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.AVSLUTTET) }
        verify { behandlingsresultatService.oppdaterBehandlingsresultattype(behandling.id, Behandlingsresultattyper.FERDIGBEHANDLET) }
        verify { oppgaveService.ferdigstillOppgaveMedSaksnummer(fagsak.saksnummer) }
        verify { prosessinstansService.opprettProsessinstansOppdaterFaktura(fagsak.saksnummer) }
    }

    @Test
    fun ferdigbehandleSak_saksstatusAnnetEnnOPPRETTET_lagrerKorrekt() {
        val fagsak = Fagsak().apply { status = Saksstatuser.LOVVALG_AVKLART }
        val behandling = Behandling().apply { this.fagsak = fagsak }
        fagsak.behandlinger.add(behandling)
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns fagsak


        ferdigbehandleSakService.ferdigbehandleSak(SAKSNUMMER)


        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.LOVVALG_AVKLART) }
        verify { behandlingsresultatService.oppdaterBehandlingsresultattype(behandling.id, Behandlingsresultattyper.FERDIGBEHANDLET) }
        verify { oppgaveService.ferdigstillOppgaveMedSaksnummer(fagsak.saksnummer) }
        verify { prosessinstansService.opprettProsessinstansOppdaterFaktura(fagsak.saksnummer) }
    }
}
