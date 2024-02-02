package no.nav.melosys.service.sak

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AnnullerSakServiceTest {
    @RelaxedMockK
    lateinit var prosessinstansService: ProsessinstansService

    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    @RelaxedMockK
    lateinit var medlemskapsperiodeService: MedlemskapsperiodeService

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var oppgaveService: OppgaveService

    @InjectMockKs
    lateinit var annullerSakService: AnnullerSakService

    @Test
    fun `annuller sak - ferdigstill oppgave, slett medlemskapsperioder, oppdater behandlingsresultatstatus og opprett prosess`() {
        val saksnummer = "78945613"
        val behandlingId = 12L
        val fagsak = Fagsak().apply {
            this.saksnummer = saksnummer
            behandlinger = listOf(
                Behandling().apply {
                    id = behandlingId
                    status = Behandlingsstatus.OPPRETTET
                }
            )
        }
        val behandlingsresultat = Behandlingsresultat().apply {
            id = behandlingId
        }

        every { fagsakService.hentFagsak(saksnummer) } returns fagsak
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat


        annullerSakService.annullerSak(saksnummer)


        verify { oppgaveService.ferdigstillOppgaveMedSaksnummer(saksnummer) }
        verify { medlemskapsperiodeService.slettMedlemskapsperioder(behandlingId) }
        verify { behandlingsresultatService.oppdaterBehandlingsresultattype(behandlingId, Behandlingsresultattyper.ANNULLERT) }
        verify { prosessinstansService.opprettAnnullerFagsakProsessflyt(fagsak) }
    }
}
