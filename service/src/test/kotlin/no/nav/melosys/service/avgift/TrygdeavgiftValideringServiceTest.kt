package no.nav.melosys.service.avgift

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.trygdeavgift.dto.DatoPeriodeDto
import no.nav.melosys.service.avgift.TrygdeavgiftValideringService.INNTEKTSPERIODER_EMPTY
import no.nav.melosys.service.avgift.TrygdeavgiftValideringService.SKATTEFORHOLDSPERIODER_EMPTY
import no.nav.melosys.service.avgift.TrygdeavgiftValideringService.SKATTEPLIKTTYPE_LIK_FOR_ALLE_PERIODER
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

data class ValideringsInput(
    val skatteforholdsperioder: List<SkatteforholdTilNorge>,
    val inntektsperioder: List<Inntektsperiode>,
    val feilmelding: String
) {
    override fun toString(): String {
        return feilmelding
    }
}

class TrygdeavgiftValideringServiceTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ValiderForTrygdeavgiftberegning {

        @Test
        fun shouldThrowFunksjonellExceptionWhenMedlemskapsPerioderIsEmpty() {
            val behandlingsresultatMock = mockk<Behandlingsresultat>()
            every { behandlingsresultatMock.medlemskapsperioder } returns emptyList()
            every { behandlingsresultatMock.utledMedlemskapsperiodeFom() } returns LocalDate.now()

            val skatteforholdsPerioder = listOf(
                SkatteforholdTilNorge().apply {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now()
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }
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
                SkatteforholdTilNorge().apply {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now()
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }
            )

            shouldThrow<FunksjonellException> {
                TrygdeavgiftValideringService.validerForTrygdeavgiftberegning(behandlingsresultatMock, skatteforholdsPerioder, listOf())
            }.message shouldBe TrygdeavgiftValideringService.UTLED_MEDLEMSKAPSPERIODE_FOM_MANGLER
        }

        @Test
        fun shouldThrowFunksjonellExceptionWhenUtledMedlemskapsperiodeTomIsNull() {
            val behandlingsresultatMock = mockk<Behandlingsresultat>()
            every { behandlingsresultatMock.medlemskapsperioder } returns listOf(Medlemskapsperiode())
            every { behandlingsresultatMock.utledMedlemskapsperiodeFom() } returns LocalDate.now()
            every { behandlingsresultatMock.utledMedlemskapsperiodeTom() } returns null

            val skatteforholdsPerioder = listOf(
                SkatteforholdTilNorge().apply {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now()
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }
            )

            shouldThrow<FunksjonellException> {
                TrygdeavgiftValideringService.validerForTrygdeavgiftberegning(behandlingsresultatMock, skatteforholdsPerioder, listOf())
            }.message shouldBe TrygdeavgiftValideringService.UTLED_MEDLEMSKAPSPERIODE_TOM_MANGLER
        }

        @Test
        fun shouldThrowFunksjonellExceptionWhenSkatteforholdPerioderOverlapper() {
            val behandlingsresultat = Behandlingsresultat().apply {
                medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(1)
                })
            }

            val skatteforholdsPerioder = listOf(
                SkatteforholdTilNorge().apply {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now().plusDays(1)
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                },
                SkatteforholdTilNorge().apply {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now().plusDays(1)
                    skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                }
            )


            shouldThrow<FunksjonellException> {
                TrygdeavgiftValideringService.validerForTrygdeavgiftberegning(
                    behandlingsresultat,
                    skatteforholdsPerioder,
                    listOf(mockk<Inntektsperiode>())
                )
            }.message shouldBe TrygdeavgiftValideringService.SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE
        }

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
                SkatteforholdTilNorge().apply {
                    fomDato = periode1.fom
                    tomDato = periode1.tom
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                },
                SkatteforholdTilNorge().apply {
                    fomDato = periode2.fom
                    tomDato = periode2.tom
                    skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                }
            )

            shouldThrow<FunksjonellException> {
                TrygdeavgiftValideringService.validerForTrygdeavgiftberegning(
                    behandlingsresultat,
                    skatteforholdsPerioder,
                    listOf(mockk<Inntektsperiode>())
                )
            }.message shouldBe TrygdeavgiftValideringService.SKATTEFORHOLDSPERIODE_DEKKER_IKKE_HELE_PERIODEN
        }

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

            val skatteforholdsPerioder = listOf(
                SkatteforholdTilNorge().apply {
                    fomDato = skatteforholdsperiode.fom
                    tomDato = skatteforholdsperiode.tom
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }
            )

            val inntektsperioder = listOf(Inntektsperiode().apply {
                fomDato = inntektsperiode.fom
                tomDato = inntektsperiode.tom
                type = Inntektskildetype.ARBEIDSINNTEKT
                isArbeidsgiversavgiftBetalesTilSkatt = true
                avgiftspliktigInntekt = mockk<Penger>()
                isErMaanedsbelop = true
            })

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
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now()
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                emptyList(),
                INNTEKTSPERIODER_EMPTY
            ),
            ValideringsInput(
                emptyList(),
                listOf(mockk<Inntektsperiode>()),
                SKATTEFORHOLDSPERIODER_EMPTY
            ),
            ValideringsInput(
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now()
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }, SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now()
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(mockk<Inntektsperiode>()),
                SKATTEPLIKTTYPE_LIK_FOR_ALLE_PERIODER
            )
        )

        fun lagGyldigBehandlingsresultat() = mockk<Behandlingsresultat>().apply {
            every { medlemskapsperioder } returns listOf(Medlemskapsperiode())
            every { utledMedlemskapsperiodeFom() } returns LocalDate.now()
            every { utledMedlemskapsperiodeTom() } returns LocalDate.now()
        }
    }

    @Nested
    inner class ErAllePerioderSkattepliktige {
        @Test
        fun shouldReturnTrueWhenAllePerioderIsSkattepliktige() {
            val skatteforholdsperioder = listOf(
                SkatteforholdTilNorge().apply {
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }, SkatteforholdTilNorge().apply {
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                })
            TrygdeavgiftValideringService.erAllePerioderSkattepliktige(skatteforholdsperioder) shouldBe true
        }

        @Test
        fun shouldReturnFalseWhenNotAllePerioderIsSkattepliktige() {
            val skatteforholdsperioder = listOf(
                SkatteforholdTilNorge().apply {
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }, SkatteforholdTilNorge().apply {
                    skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                })
            TrygdeavgiftValideringService.erAllePerioderSkattepliktige(skatteforholdsperioder) shouldBe false
        }
    }
}
