package no.nav.melosys.service.avgift

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.avgift.Aarsavregning
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.BeregnTotalBeløpDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaseriePeriodeDto
import no.nav.melosys.repository.AarsavregningRepository
import no.nav.melosys.service.avgift.aarsavregning.Årsavregning
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
internal class ÅrsavregningServiceTest {
    @RelaxedMockK
    private lateinit var faktureringskomponentenConsumer: FaktureringskomponentenConsumer
    @RelaxedMockK
    private lateinit var aarsavregningRepository: AarsavregningRepository

    private lateinit var årsavregningService: ÅrsavregningService

    val SAKSBEHANDLER_IDENT = "Z990007"

    @BeforeEach
    fun setup() {
        årsavregningService = ÅrsavregningService(faktureringskomponentenConsumer, aarsavregningRepository)
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

    @Test
    fun `hentÅrsavregning for ny årsavregning uten info i Melosys`() {
        val årsavregningEntity = Aarsavregning().apply {
            aar = 2023
        }
        every { aarsavregningRepository.findById(any()) }.returns(Optional.of(årsavregningEntity))

        årsavregningService.hentÅrsavregning(1) shouldBe Årsavregning(
            aar = 2023,
            tidligereGrunnlag = null,
            tidligereAvgift = emptyList(),
            nyttGrunnlag = null,
            endeligAvgift = emptyList(),
            tidligereFakturertBeloep = null,
            nyttTotalbeloep = null,
            tilFaktureringBeloep = null
        )
    }

}
