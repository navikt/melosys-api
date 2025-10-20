package no.nav.melosys.service.medl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.integrasjon.medl.MedlService
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl
import no.nav.melosys.repository.AnmodningsperiodeRepository
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class MedlAnmodningsperiodeServiceTest {

    private val medlService = mockk<MedlService>(relaxed = true)
    private val behandlingsresultatService = mockk<BehandlingsresultatService>()
    private val anmodningsperiodeRepository = mockk<AnmodningsperiodeRepository>()
    private lateinit var medlAnmodningsperiodeService: MedlAnmodningsperiodeService
    private lateinit var nyBehandling: Behandling
    private lateinit var behandlingsresultat: Behandlingsresultat
    private lateinit var fagsak: Fagsak

    @BeforeEach
    fun setup() {
        medlAnmodningsperiodeService = MedlAnmodningsperiodeService(
            medlService,
            behandlingsresultatService,
            anmodningsperiodeRepository
        )

        nyBehandling = Behandling.forTest {
            tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
            registrertDato = Instant.now()
        }
        fagsak = Fagsak.forTest { }
        behandlingsresultat = lagBehandlingsresultatMedAnmodningsperiode()
    }

    @Test
    fun `avsluttTidligereAnmodningsperiode skal avslutte tidligere anmodningsperiode`() {
        fagsak.leggTilBehandling(lagA001Behandling(1L, Instant.now().minusSeconds(5)))
        fagsak.leggTilBehandling(nyBehandling)
        nyBehandling.id = 2L
        nyBehandling.fagsak = fagsak
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat


        medlAnmodningsperiodeService.avsluttTidligereAnmodningsperiode(nyBehandling)


        verify { medlService.avvisPeriode(MOCKED_FORRIGE_BEHANDLING_MEDL_PERIODE_ID, StatusaarsakMedl.AVVIST) }
    }

    @Test
    fun `avsluttTidligereAnmodningsperiode skal avslutte tidligere anmodningsperiode med flere tidligere behandlinger`() {
        fagsak.leggTilBehandling(lagA001Behandling(1L, Instant.now().minusSeconds(15)))
        fagsak.leggTilBehandling(lagA001Behandling(2L, Instant.now().minusSeconds(10)))
        fagsak.leggTilBehandling(lagA001Behandling(3L, Instant.now().minusSeconds(5)))
        fagsak.leggTilBehandling(nyBehandling)
        nyBehandling.id = 4L
        nyBehandling.fagsak = fagsak
        every { behandlingsresultatService.hentBehandlingsresultat(3L) } returns behandlingsresultat


        medlAnmodningsperiodeService.avsluttTidligereAnmodningsperiode(nyBehandling)


        verify { medlService.avvisPeriode(MOCKED_FORRIGE_BEHANDLING_MEDL_PERIODE_ID, StatusaarsakMedl.AVVIST) }
    }

    private fun lagBehandlingsresultatMedAnmodningsperiode(): Behandlingsresultat =
        Behandlingsresultat().apply {
            val anmodningsperiode = Anmodningsperiode().apply {
                medlPeriodeID = MOCKED_FORRIGE_BEHANDLING_MEDL_PERIODE_ID
            }
            anmodningsperioder = mutableSetOf(anmodningsperiode)
        }

    private fun lagA001Behandling(id: Long, registrertDato: Instant): Behandling =
        Behandling.forTest {
            this.id = id
            tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
            this.registrertDato = registrertDato
        }

    companion object {
        private const val MOCKED_FORRIGE_BEHANDLING_MEDL_PERIODE_ID = 123456780L
    }
}
