package no.nav.melosys.service.behandling.jobb

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.service.behandling.BehandlingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AvsluttArt13BehandlingJobbKtTest {

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var avsluttArt13BehandlingService: AvsluttArt13BehandlingService

    private lateinit var avsluttArt13BehandlingJobb: AvsluttArt13BehandlingJobb

    @BeforeEach
    fun setup() {
        avsluttArt13BehandlingJobb = AvsluttArt13BehandlingJobb(behandlingService, avsluttArt13BehandlingService)
    }

    @Test
    fun `avsluttBehandlingArt13_femBehandlinger_serviceBlirKalt`() {
        val b1 = BehandlingTestFactory.builderWithDefaults()
            .medId(111L)
            .build()

        val b2 = BehandlingTestFactory.builderWithDefaults()
            .medId(222L)
            .build()

        every { behandlingService.hentBehandlingerMedstatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING) } returns listOf(b1, b2)

        avsluttArt13BehandlingJobb.avsluttBehandlingArt13()
        verify { avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(b1.id) }
        verify { avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(b2.id) }
    }
}
