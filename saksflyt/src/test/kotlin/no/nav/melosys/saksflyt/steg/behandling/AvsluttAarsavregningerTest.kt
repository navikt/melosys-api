package no.nav.melosys.saksflyt.steg.behandling

import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AvsluttAarsavregningerTest {

    @MockK
    private lateinit var oppgaveService: OppgaveService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    private lateinit var avsluttAarsavregninger: AvsluttAarsavregninger

    @BeforeEach
    fun setUp() {
        avsluttAarsavregninger = AvsluttAarsavregninger(behandlingService, oppgaveService, behandlingsresultatService)
    }

    @Test
    fun `avslutter aktive årsavregninger på saken`() {
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.ANNULLER_SAK
            status = ProsessStatus.KLAR
            behandling {
                id = 123L
                type = Behandlingstyper.NY_VURDERING
                tema = Behandlingstema.YRKESAKTIV
                fagsak {
                    behandling {
                        id = 11L
                        type = Behandlingstyper.ÅRSAVREGNING
                        status = Behandlingsstatus.OPPRETTET
                    }
                    behandling {
                        id = 12L
                        type = Behandlingstyper.ÅRSAVREGNING
                        status = Behandlingsstatus.OPPRETTET
                    }
                }
            }
        }

        justRun { oppgaveService.ferdigstillOppgaveMedBehandlingID(any()) }
        justRun { behandlingsresultatService.oppdaterBehandlingsresultattype(any(), Behandlingsresultattyper.FERDIGBEHANDLET) }
        justRun { behandlingService.avsluttBehandling(any()) }

        avsluttAarsavregninger.utfør(prosessinstans)


        verify(exactly = 2) { oppgaveService.ferdigstillOppgaveMedBehandlingID(any()) }
        verify(exactly = 2) { behandlingsresultatService.oppdaterBehandlingsresultattype(any(), Behandlingsresultattyper.FERDIGBEHANDLET) }
        verify(exactly = 2) { behandlingService.avsluttBehandling(any()) }
    }
}
