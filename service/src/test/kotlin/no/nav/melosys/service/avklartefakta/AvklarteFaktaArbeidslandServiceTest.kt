package no.nav.melosys.service.avklartefakta

import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test

@ExtendWith(MockKExtension::class)
class AvklarteFaktaArbeidslandServiceTest {

    @MockK(relaxed = true)
    private lateinit var avklartefaktaService: AvklartefaktaService

    private lateinit var service: AvklarteFaktaArbeidslandService

    @BeforeEach
    fun setUp() {
        service = AvklarteFaktaArbeidslandService(avklartefaktaService)
    }

    @Test
    fun `test lagreArbeidslandSomAvklartefakta med flere land`() {
        val behandlingID = 123L
        val countries = listOf("NO", "SE")

        service.lagreArbeidslandSomAvklartefakta(behandlingID, countries)

        verify {
            avklartefaktaService.slettAvklarteFakta(behandlingID, Avklartefaktatyper.ARBEIDSLAND)
            for (country in countries) {
                avklartefaktaService.leggTilAvklarteFakta(behandlingID, Avklartefaktatyper.ARBEIDSLAND,
                    Avklartefaktatyper.ARBEIDSLAND.kode, country, Avklartefakta.VALGT_FAKTA)
            }
        }
    }

    @Test
    fun `test lagreArbeidslandSomAvklartefakta med ett land`() {
        val behandlingID = 123L
        val country = "SE"

        service.lagreArbeidslandSomAvklartfakta(country, behandlingID)

        verify {
            avklartefaktaService.leggTilAvklarteFakta(behandlingID, Avklartefaktatyper.ARBEIDSLAND,
                Avklartefaktatyper.ARBEIDSLAND.kode, country, Avklartefakta.VALGT_FAKTA)
        }
    }
}
