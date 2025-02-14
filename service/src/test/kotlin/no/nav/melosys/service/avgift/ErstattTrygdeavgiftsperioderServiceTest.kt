package no.nav.melosys.service.avgift

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ErstattTrygdeavgiftsperioderServiceTest() {
    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @InjectMockKs
    private lateinit var erstattTrygdeavgiftsperioderService: ErstattTrygdeavgiftsperioderService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this) // Initializes the mocks
    }

    @Nested
    inner class ErstattTrygdeavgiftsperioderTest {
        @Test
        fun `erstatter eksisterende Trygdeavgiftsperioder`() {
            val id = 1L

            val medlemskap = Medlemskapsperiode().apply { this.id = id }
            val behandlingsresultat = Behandlingsresultat().apply {
                behandling = Behandling()
                medlemskapsperioder = listOf(medlemskap)
            }

            val trygdeavgiftsperiodeMock = mockk<Trygdeavgiftsperiode>(relaxed = true).apply {
                every { grunnlagMedlemskapsperiode?.id } returns id
            }

            every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat


            erstattTrygdeavgiftsperioderService.erstattTrygdeavgiftsperioder(1337L, listOf(trygdeavgiftsperiodeMock))


            verify { medlemskap.addTrygdeavgiftsperiode(trygdeavgiftsperiodeMock) }
        }

        @Test
        fun erPliktigMedlemskapSkattepliktig() {
        }

        @Test
        fun erstattTrygdeavgiftsperioder() {
        }

        @Test
        fun leggTilTrygdeavgiftsperiodeForPliktigMedlemskapSkattepliktig() {
        }

    }
}
