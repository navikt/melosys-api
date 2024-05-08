package no.nav.melosys.service.sak

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.BeregnTotalBeløpDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaseriePeriodeDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class ÅrsavregningServiceTest {
    @RelaxedMockK
    private lateinit var faktureringskomponentenConsumer: FaktureringskomponentenConsumer

    private lateinit var årsavregningService: ÅrsavregningService

    @BeforeEach
    fun setup() {
        årsavregningService = ÅrsavregningService(faktureringskomponentenConsumer)
    }

    @Test
    fun `test hentTotalTrygdeavgiftForPeriode henter totalbeløp`() {
        val fakturaseriePeriodeDto = FakturaseriePeriodeDto(
            enhetsprisPerManed = BigDecimal(100), startDato = LocalDate.now().minusYears(1), sluttDato = LocalDate.now(), beskrivelse = "test")

        val dto = BeregnTotalBeløpDto("Z123456", listOf(fakturaseriePeriodeDto, fakturaseriePeriodeDto, fakturaseriePeriodeDto))
        val totalTrygdeavgiftMockResult = BigDecimal.valueOf(1234.55)
        every { faktureringskomponentenConsumer.hentTotalTrygdeavgiftForPeriode(dto) } returns totalTrygdeavgiftMockResult

        val result = årsavregningService.beregnTotalTrygdeavgiftForPeriode(dto)

        verify { faktureringskomponentenConsumer.hentTotalTrygdeavgiftForPeriode(dto) }
        assert(result == totalTrygdeavgiftMockResult)
        }
}
