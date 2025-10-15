package no.nav.melosys.saksflyt.steg.sed

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeGodkjenning
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class BestemBehandlingsmåteSedTest {

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var oppgaveService: OppgaveService

    @MockK
    private lateinit var unntaksperiodeService: UnntaksperiodeService

    private lateinit var bestemBehandlingsmåteSed: BestemBehandlingsmåteSed

    private lateinit var behandling: Behandling
    private val behandlingsresultat = Behandlingsresultat()
    private val prosessinstans = Prosessinstans.forTest {
        type = ProsessType.OPPRETT_SAK
        status = ProsessStatus.KLAR
    }

    @BeforeEach
    fun setUp() {
        bestemBehandlingsmåteSed = BestemBehandlingsmåteSed(behandlingService, behandlingsresultatService, oppgaveService, unntaksperiodeService)
        behandling = Behandling.forTest {
            id = 234L
            fagsak {
                medBruker()
            }
        }
        prosessinstans.behandling = behandling

        every { behandlingService.hentBehandlingMedSaksopplysninger(eq(behandling.id)) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat
        every { behandlingsresultatService.oppdaterBehandlingsMaate(any(), any()) } just Runs
    }

    @Test
    fun `utfør temaRegistreringUnntakIngenTreffIRegister prosessOpprettes`() {
        behandling.tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
        every { unntaksperiodeService.godkjennPeriode(any(), any()) } just Runs


        bestemBehandlingsmåteSed.utfør(prosessinstans)


        val forventetUnntaksperiodeGodkjenning = UnntaksperiodeGodkjenning.builder()
            .varsleUtland(false)
            .fritekst(null)
            .build()
        verify { unntaksperiodeService.godkjennPeriode(eq(behandling.id), eq(forventetUnntaksperiodeGodkjenning)) }
    }

    @Test
    fun `utfør temaRegistreringUnntakMedTreffIRegister oppgaveOpprettes`() {
        behandling.tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
        behandlingsresultat.kontrollresultater = mutableSetOf(Kontrollresultat().apply {
            begrunnelse = Kontroll_begrunnelser.FEIL_I_PERIODEN
        })

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat
        every { oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(any(), any(), any(), any(), any()) } just Runs


        bestemBehandlingsmåteSed.utfør(prosessinstans)


        verify { oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(eq(behandling), any(), any(), any(), any()) }
    }
}
