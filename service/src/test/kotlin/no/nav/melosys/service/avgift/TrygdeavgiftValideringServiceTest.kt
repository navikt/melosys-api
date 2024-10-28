package no.nav.melosys.service.avgift

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.trygdeavgift.dto.DatoPeriodeDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.InntektsperiodeDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.PengerDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.SkatteforholdsperiodeDto
import no.nav.melosys.service.avgift.TrygdeavgiftValideringService.INNTEKTSPERIODER_EMPTY
import no.nav.melosys.service.avgift.TrygdeavgiftValideringService.SKATTEFORHOLDSPERIODER_EMPTY
import no.nav.melosys.service.avgift.TrygdeavgiftValideringService.SKATTEPLIKTTYPE_LIK_FOR_ALLE_PERIODER
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

            val skatteforholdsPerioder = listOf(
                SkatteforholdsperiodeDto(
                    UUID.randomUUID(),
                    DatoPeriodeDto(fom = LocalDate.now(), tom = LocalDate.now()),
                    Skatteplikttype.SKATTEPLIKTIG
                )
            )

            shouldThrow<FunksjonellException> {
                TrygdeavgiftValideringService.validerForTrygdeavgiftberegning(
                    behandlingsresultatMock,
                    skatteforholdsPerioder,
                    emptyList()
                )
            }.message shouldBe TrygdeavgiftValideringService.MEDLEMSKAPSPERIODER_EMPTY
        }

        @Test
        fun shouldThrowFunksjonellExceptionWhenUtledMedlemskapsperiodeFomIsNull() {
            val behandlingsresultatMock = mockk<Behandlingsresultat>()
            every { behandlingsresultatMock.medlemskapsperioder } returns listOf(Medlemskapsperiode())
            every { behandlingsresultatMock.utledMedlemskapsperiodeFom() } returns null

            val skatteforholdsPerioder = listOf(
                SkatteforholdsperiodeDto(
                    UUID.randomUUID(),
                    DatoPeriodeDto(fom = LocalDate.now(), tom = LocalDate.now()),
                    Skatteplikttype.SKATTEPLIKTIG
                )
            )

            shouldThrow<FunksjonellException> {
                TrygdeavgiftValideringService.validerForTrygdeavgiftberegning(behandlingsresultatMock, skatteforholdsPerioder, listOf())
            }.message shouldBe TrygdeavgiftValideringService.UTLED_MEDLEMSKAPSPERIODE_FOM_MANGLER
        }

        @Test // TODO rewrite without request
        fun shouldThrowFunksjonellExceptionWhenUtledMedlemskapsperiodeTomIsNull() {
            val behandlingsresultatMock = mockk<Behandlingsresultat>()
            every { behandlingsresultatMock.medlemskapsperioder } returns listOf(Medlemskapsperiode())
            every { behandlingsresultatMock.utledMedlemskapsperiodeFom() } returns LocalDate.now()
            every { behandlingsresultatMock.utledMedlemskapsperiodeTom() } returns null

            val skatteforholdsPerioder = listOf(
                SkatteforholdsperiodeDto(
                    UUID.randomUUID(),
                    DatoPeriodeDto(fom = LocalDate.now(), tom = LocalDate.now()),
                    Skatteplikttype.SKATTEPLIKTIG
                )
            )

            shouldThrow<FunksjonellException> {
                TrygdeavgiftValideringService.validerForTrygdeavgiftberegning(behandlingsresultatMock, skatteforholdsPerioder, listOf())
            }.message shouldBe TrygdeavgiftValideringService.UTLED_MEDLEMSKAPSPERIODE_TOM_MANGLER
        }

        // SKATTEFORHOLDSPERIODER
        @Test
        fun shouldThrowFunksjonellExceptionWhenSkatteforholdPerioderOverlapper() {
            val behandlingsresultat = Behandlingsresultat().apply {
                medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(1)
                })
            }

            val periode = DatoPeriodeDto(fom = LocalDate.now(), tom = LocalDate.now().plusDays(1))

            val skatteforholdsPerioder = listOf(
                SkatteforholdsperiodeDto(UUID.randomUUID(), periode, Skatteplikttype.SKATTEPLIKTIG),
                SkatteforholdsperiodeDto(UUID.randomUUID(), periode, Skatteplikttype.IKKE_SKATTEPLIKTIG)
            )


            shouldThrow<FunksjonellException> {
                TrygdeavgiftValideringService.validerForTrygdeavgiftberegning(
                    behandlingsresultat,
                    skatteforholdsPerioder,
                    listOf(mockk<InntektsperiodeDto>())
                )
            }.message shouldBe TrygdeavgiftValideringService.SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE
        }

        // SKATTEFORHOLDSPERIODER
        @Test
        fun shouldThrowFunksjonellExceptionWhenSkatteforholdPerioderDekkerIkkeHelePerioden() {
            val behandlingsresultat = Behandlingsresultat().apply {
                medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(3)
                })
            }

            val periode1 = DatoPeriodeDto(fom = LocalDate.now(), tom = LocalDate.now())
            val periode2 = DatoPeriodeDto(fom = LocalDate.now().plusDays(1), tom = LocalDate.now().plusDays(1))

            val skatteforholdsPerioder = listOf(
                SkatteforholdsperiodeDto(UUID.randomUUID(), periode1, Skatteplikttype.SKATTEPLIKTIG),
                SkatteforholdsperiodeDto(UUID.randomUUID(), periode2, Skatteplikttype.IKKE_SKATTEPLIKTIG)
            )


            shouldThrow<FunksjonellException> {
                TrygdeavgiftValideringService.validerForTrygdeavgiftberegning(
                    behandlingsresultat,
                    skatteforholdsPerioder,
                    listOf(mockk<InntektsperiodeDto>())
                )
            }.message shouldBe TrygdeavgiftValideringService.SKATTEFORHOLDSPERIODE_DEKKER_IKKE_HELE_PERIODEN
        }

        // INNTEKTSPERIODER
        @Test
        fun shouldThrowFunksjonellExceptionWhenInntektsPerioderDekkerIkkeHelePerioden() {
            val behandlingsresultat = Behandlingsresultat().apply {
                medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(3)
                })
            }

            val skatteforholdsperiode = DatoPeriodeDto(fom = LocalDate.now().plusDays(0), tom = LocalDate.now().plusDays(3))
            val inntektsperiode = DatoPeriodeDto(fom = LocalDate.now(), tom = LocalDate.now())

            val skatteforholdsPerioder = listOf(SkatteforholdsperiodeDto(UUID.randomUUID(), skatteforholdsperiode, Skatteplikttype.SKATTEPLIKTIG))

            val inntektsperioder = listOf(
                InntektsperiodeDto(
                    UUID.randomUUID(),
                    inntektsperiode,
                    Inntektskildetype.ARBEIDSINNTEKT,
                    arbeidsgiverBetalerAvgift = true,
                    mockk<PengerDto>(),
                    true
                )
            )


            shouldThrow<FunksjonellException> {
                TrygdeavgiftValideringService.validerForTrygdeavgiftberegning(
                    behandlingsresultat,
                    skatteforholdsPerioder,
                    inntektsperioder
                )
            }.message shouldBe TrygdeavgiftValideringService.INNTEKTSPERIODE_DEKKER_IKKE_HELE_PERIODEN
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
            ),
            ValideringsInput(
                listOf(
                    SkatteforholdsperiodeDto(
                        UUID.randomUUID(),
                        DatoPeriodeDto(LocalDate.now(), LocalDate.now()),
                        Skatteplikttype.IKKE_SKATTEPLIKTIG
                    ), SkatteforholdsperiodeDto(
                        UUID.randomUUID(),
                        DatoPeriodeDto(LocalDate.now(), LocalDate.now()),
                        Skatteplikttype.IKKE_SKATTEPLIKTIG
                    )
                ),
                listOf(mockk<InntektsperiodeDto>()),
                SKATTEPLIKTTYPE_LIK_FOR_ALLE_PERIODER
            )
        )

        fun lagGyldigBehandlingsresultat() = mockk<Behandlingsresultat>().apply {
            every { medlemskapsperioder } returns listOf(Medlemskapsperiode())
            every { utledMedlemskapsperiodeFom() } returns LocalDate.now()
            every { utledMedlemskapsperiodeTom() } returns LocalDate.now()
        }
    }
}
