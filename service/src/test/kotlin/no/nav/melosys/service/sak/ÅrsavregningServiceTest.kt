package no.nav.melosys.service.sak

import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.BeregnTotalBeløpDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaseriePeriodeDto
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
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

    val SAKSBEHANDLER_IDENT = "Z990007"

    @BeforeEach
    fun setup() {
        årsavregningService = ÅrsavregningService(faktureringskomponentenConsumer)
        SpringSubjectHandler.set(TestSubjectHandler())
    }

    @Test
    fun `test beregnTotalTrygdeavgiftForPeriode beregner totalbeløp for 1 år`() {
        val fakturaseriePeriodeDto = FakturaseriePeriodeDto(
            enhetsprisPerManed = BigDecimal(100), startDato = LocalDate.now().minusYears(1), sluttDato = LocalDate.now(), beskrivelse = "test")
        val dto = BeregnTotalBeløpDto(listOf(fakturaseriePeriodeDto, fakturaseriePeriodeDto, fakturaseriePeriodeDto))
        årsavregningService.beregnTotalTrygdeavgiftForPeriode(dto)

        verify(exactly = 1) { faktureringskomponentenConsumer.hentTotalTrygdeavgiftForPeriode((eq(dto)), eq(SAKSBEHANDLER_IDENT)) }
    }
}
