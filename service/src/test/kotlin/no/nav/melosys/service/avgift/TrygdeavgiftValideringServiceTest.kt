package no.nav.melosys.service.avgift

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.trygdeavgift.dto.DatoPeriodeDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.InntektsperiodeDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.SkatteforholdsperiodeDto
import no.nav.melosys.service.avgift.TrygdeavgiftValideringService.INNTEKTSPERIODER_EMPTY
import no.nav.melosys.service.avgift.TrygdeavgiftValideringService.SKATTEFORHOLDSPERIODER_EMPTY
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.util.*

data class ValideringsInput(
    val skatteforholdsperioder: List<SkatteforholdsperiodeDto>,
    val inntektsperioder: List<InntektsperiodeDto>,
    val feilmelding: String
) {
    override fun toString(): String {
        return feilmelding
    }
}

class TrygdeavgiftValideringServiceTest {
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ValiderTrygdeavgiftberegningRequest {
        @Test
        fun shouldThrowFunksjonellExceptionWhenMedlemskapsPerioderIsEmpty() {
            val behandlingsresultatMock = mockk<Behandlingsresultat>()
            every { behandlingsresultatMock.medlemskapsperioder } returns emptyList()
            every { behandlingsresultatMock.utledMedlemskapsperiodeFom() } returns LocalDate.now()

            val oppdaterTrygdeAvgiftsGrunnlagRequest = OppdaterTrygdeavgiftsgrunnlagRequest(emptyList(), emptyList())

            shouldThrow<FunksjonellException> {
                TrygdeavgiftValideringService.validerTrygdeavgiftberegningRequest(oppdaterTrygdeAvgiftsGrunnlagRequest, behandlingsresultatMock)
            }.message shouldBe TrygdeavgiftValideringService.MEDLEMSKAPSPERIODER_EMPTY
        }

        @Test
        fun shouldThrowFunksjonellExceptionWhenUtledMedlemskapsperiodeFomIsNull() {
            val behandlingsresultatMock = mockk<Behandlingsresultat>()
            every { behandlingsresultatMock.medlemskapsperioder } returns listOf(Medlemskapsperiode())
            every { behandlingsresultatMock.utledMedlemskapsperiodeFom() } returns null

            val oppdaterTrygdeAvgiftsGrunnlagRequest = OppdaterTrygdeavgiftsgrunnlagRequest(emptyList(), emptyList())


            shouldThrow<FunksjonellException> {
                TrygdeavgiftValideringService.validerTrygdeavgiftberegningRequest(oppdaterTrygdeAvgiftsGrunnlagRequest, behandlingsresultatMock)
            }.message shouldBe TrygdeavgiftValideringService.UTLED_MEDLEMSKAPSPERIODE_FOM_MANGLER
        }

        @ParameterizedTest
        @MethodSource("valideringsData")
        fun shouldThrowFunksjonellExceptionWhenInntektsPerioiderIsEmpty(valideringsInput: ValideringsInput) {
            val behandlingsresultatMock = lagGyldigBehandlingsresultat()
            val skatteforholdsPerioder = valideringsInput.skatteforholdsperioder
            val inntektsPerioder = valideringsInput.inntektsperioder


            shouldThrow<FunksjonellException> {
                TrygdeavgiftValideringService.validerForTrygdeavgiftberegning(behandlingsresultatMock, skatteforholdsPerioder, inntektsPerioder)
            }.message shouldBe valideringsInput.feilmelding
        }

        fun valideringsData(): List<ValideringsInput> = listOf(
            ValideringsInput(
                listOf(
                    SkatteforholdsperiodeDto(
                        UUID.randomUUID(),
                        DatoPeriodeDto(LocalDate.now(), LocalDate.now()),
                        Skatteplikttype.IKKE_SKATTEPLIKTIG
                    )
                ),
                emptyList(),
                INNTEKTSPERIODER_EMPTY
            ),
            ValideringsInput(
                emptyList(),
                listOf(mockk<InntektsperiodeDto>()),
                SKATTEFORHOLDSPERIODER_EMPTY
            )
        )

        fun lagGyldigBehandlingsresultat() = mockk<Behandlingsresultat>().apply {
            every { medlemskapsperioder } returns listOf(Medlemskapsperiode())
            every { utledMedlemskapsperiodeFom() } returns LocalDate.now()
        }
    }
}
