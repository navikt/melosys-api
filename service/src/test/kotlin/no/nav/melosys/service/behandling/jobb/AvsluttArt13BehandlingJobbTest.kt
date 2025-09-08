package no.nav.melosys.service.behandling.jobb

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.service.behandling.BehandlingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AvsluttArt13BehandlingJobbTest {

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
    fun `avsluttBehandlingArt13 femBehandlinger serviceBlirKalt`() {
        val id1 = 111L
        val id2 = 222L

        every { behandlingService.hentBehandlingIderMedStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING) } returns listOf(id1, id2)
        every { avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(any()) } returns Unit

        avsluttArt13BehandlingJobb.avsluttBehandlingArt13()

        verify { avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(id1) }
        verify { avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(id2) }
    }
}
