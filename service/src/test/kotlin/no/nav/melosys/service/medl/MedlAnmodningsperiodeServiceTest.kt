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

    @BeforeEach
    fun setup() {
        medlAnmodningsperiodeService = MedlAnmodningsperiodeService(
            medlService,
            behandlingsresultatService,
            anmodningsperiodeRepository
        )
    }

    @Test
    fun `avsluttTidligereAnmodningsperiode skal avslutte tidligere anmodningsperiode`() {
        val fagsak = Fagsak.forTest {
            behandling {
                id = 1L
                tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
                registrertDato = Instant.now().minusSeconds(5)
            }
            behandling {
                id = 2L
                tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
                registrertDato = Instant.now()
            }
        }
        val nyBehandling = fagsak.behandlinger.first { it.id == 2L }
        val behandlingsresultat = lagBehandlingsresultatMedAnmodningsperiode()
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat


        medlAnmodningsperiodeService.avsluttTidligereAnmodningsperiode(nyBehandling)


        verify { medlService.avvisPeriode(MOCKED_FORRIGE_BEHANDLING_MEDL_PERIODE_ID, StatusaarsakMedl.AVVIST) }
    }

    @Test
    fun `avsluttTidligereAnmodningsperiode skal avslutte tidligere anmodningsperiode med flere tidligere behandlinger`() {
        val fagsak = Fagsak.forTest {
            behandling {
                id = 1L
                tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
                registrertDato = Instant.now().minusSeconds(15)
            }
            behandling {
                id = 2L
                tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
                registrertDato = Instant.now().minusSeconds(10)
            }
            behandling {
                id = 3L
                tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
                registrertDato = Instant.now().minusSeconds(5)
            }
            behandling {
                id = 4L
                tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
                registrertDato = Instant.now()
            }
        }
        val nyBehandling = fagsak.behandlinger.first { it.id == 4L }
        val behandlingsresultat = lagBehandlingsresultatMedAnmodningsperiode()
        every { behandlingsresultatService.hentBehandlingsresultat(3L) } returns behandlingsresultat


        medlAnmodningsperiodeService.avsluttTidligereAnmodningsperiode(nyBehandling)


        verify { medlService.avvisPeriode(MOCKED_FORRIGE_BEHANDLING_MEDL_PERIODE_ID, StatusaarsakMedl.AVVIST) }
    }

    private fun lagBehandlingsresultatMedAnmodningsperiode(): Behandlingsresultat =
        Behandlingsresultat.forTest {
            anmodningsperiode {
                medlPeriodeID = MOCKED_FORRIGE_BEHANDLING_MEDL_PERIODE_ID
            }
        }

    companion object {
        private const val MOCKED_FORRIGE_BEHANDLING_MEDL_PERIODE_ID = 123456780L
    }
}
