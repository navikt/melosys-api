package no.nav.melosys.service.avgift

import io.kotest.assertions.throwables.shouldNotThrow
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
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningValidering.INNTEKTSPERIODER_EMPTY
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningValidering.SKATTEFORHOLDSPERIODER_EMPTY
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningValidering.SKATTEPLIKTTYPE_LIK_FOR_ALLE_PERIODER
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

data class ValideringsInput(
    val medlemskapsperioder: List<Medlemskapsperiode>,
    val skatteforholdsperioder: List<SkatteforholdTilNorge>,
    val inntektsperioder: List<Inntektsperiode>,
    val feilmelding: String
) {
    constructor(
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsperioder: List<Inntektsperiode>,
        feilmelding: String
    ) : this(emptyList<Medlemskapsperiode>(), skatteforholdsperioder, inntektsperioder, feilmelding)

    override fun toString(): String {
        return feilmelding
    }
}

class TrygdeavgiftsberegningValideringTest {

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
                TrygdeavgiftsberegningValidering.validerForTrygdeavgiftberegning(
                    behandlingsresultatMock,
                    skatteforholdsPerioder,
                    emptyList()
                )
            }.message shouldBe TrygdeavgiftsberegningValidering.MEDLEMSKAPSPERIODER_EMPTY
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
                TrygdeavgiftsberegningValidering.validerForTrygdeavgiftberegning(behandlingsresultatMock, skatteforholdsPerioder, listOf())
            }.message shouldBe TrygdeavgiftsberegningValidering.UTLED_MEDLEMSKAPSPERIODE_FOM_MANGLER
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
                TrygdeavgiftsberegningValidering.validerForTrygdeavgiftberegning(behandlingsresultatMock, skatteforholdsPerioder, listOf())
            }.message shouldBe TrygdeavgiftsberegningValidering.UTLED_MEDLEMSKAPSPERIODE_TOM_MANGLER
        }

        @ParameterizedTest
        @MethodSource("valideringsDataPeriodermedFeilScenarios")
        fun shouldThrowExceptionWhenPerioderHarFeil(valideringsInput: ValideringsInput) {
            val behandlingsresultat = Behandlingsresultat().apply {
                medlemskapsperioder = valideringsInput.medlemskapsperioder
            }

            shouldThrow<FunksjonellException> {
                TrygdeavgiftsberegningValidering.validerForTrygdeavgiftberegning(
                    behandlingsresultat,
                    valideringsInput.skatteforholdsperioder,
                    valideringsInput.inntektsperioder
                )
            }.message shouldBe valideringsInput.feilmelding
        }

        @ParameterizedTest
        @MethodSource("valideringsDataPerioderDekkesScenarios")
        fun shouldBeValidPeriodeWhenInntektsPerioderDekkerHelePerioden(valideringsInput: ValideringsInput) {
            val behandlingsresultat = Behandlingsresultat().apply {
                medlemskapsperioder = valideringsInput.medlemskapsperioder
            }

            shouldNotThrow<FunksjonellException> {
                TrygdeavgiftsberegningValidering.validerForTrygdeavgiftberegning(
                    behandlingsresultat,
                    valideringsInput.skatteforholdsperioder,
                    valideringsInput.inntektsperioder
                )
            }
        }

        @ParameterizedTest
        @MethodSource("valideringsData")
        fun shouldThrowFunksjonellExceptionWhenInntektsPerioiderIsEmpty(valideringsInput: ValideringsInput) {
            val behandlingsresultatMock = lagGyldigBehandlingsresultat()
            val skatteforholdsPerioder = valideringsInput.skatteforholdsperioder
            val inntektsPerioder = valideringsInput.inntektsperioder

            shouldThrow<FunksjonellException> {
                TrygdeavgiftsberegningValidering.validerForTrygdeavgiftberegning(behandlingsresultatMock, skatteforholdsPerioder, inntektsPerioder)
            }.message shouldBe valideringsInput.feilmelding
        }

        fun valideringsDataPeriodermedFeilScenarios(): List<ValideringsInput> = listOf(
            ValideringsInput(                                                               // skatteforhold overlapper samme dag
                listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(10)
                }),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now().plusDays(5)
                        tomDato = LocalDate.now().plusDays(10)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ), listOf(Inntektsperiode().apply {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now().plusDays(10)
                }), TrygdeavgiftsberegningValidering.SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE
            ),

            ValideringsInput(                                                               // Inntektsperioder kan ikke overlappe
                listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(10)
                }),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(10)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ), listOf(Inntektsperiode().apply {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now().plusDays(5)
                }, Inntektsperiode().apply {
                    fomDato = LocalDate.now().plusDays(5)
                    tomDato = LocalDate.now().plusDays(10)
                }), TrygdeavgiftsberegningValidering.INNTEKTSPERIODENE_KAN_IKKE_OVERLAPPE
            ),

            ValideringsInput(                                                               // Skatteforhold dekker ikke hele perioden
                listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(1)
                }),
                listOf(
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
                ), listOf(mockk<Inntektsperiode>()), TrygdeavgiftsberegningValidering.SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE
            ),

            ValideringsInput(                                                               // Skatteforhold dekker ikke hele perioden
                listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(3)
                }),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now()
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now().plusDays(1)
                        tomDato = LocalDate.now().plusDays(1)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ), listOf(mockk<Inntektsperiode>()), TrygdeavgiftsberegningValidering.SKATTEFORHOLDSPERIODE_DEKKER_IKKE_HELE_PERIODEN
            ),

            ValideringsInput(                                                               // Inntektsperioder dekker ikke hele perioden
                listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(3)
                }),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now().plusDays(0)
                        tomDato = LocalDate.now().plusDays(3)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                ), listOf(Inntektsperiode().apply {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now()
                    type = Inntektskildetype.ARBEIDSINNTEKT
                    isArbeidsgiversavgiftBetalesTilSkatt = true
                    avgiftspliktigMndInntekt = mockk<Penger>()
                }), TrygdeavgiftsberegningValidering.INNTEKTSPERIODE_DEKKER_IKKE_HELE_PERIODEN
            ),

            ValideringsInput(                                                               // Inntektsperioder overlapper
                listOf(Medlemskapsperiode().apply {
                    fom = LocalDate.of(2023, 1, 1)
                    tom = LocalDate.of(2023, 1, 8)
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                }),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.of(2023, 1, 1)
                        tomDato = LocalDate.of(2023, 1, 8)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                ), listOf(
                    Inntektsperiode().apply {
                        fomDato = LocalDate.of(2023, 1, 1)
                        tomDato = LocalDate.of(2023, 1, 6)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    },
                    Inntektsperiode().apply {
                        fomDato = LocalDate.of(2023, 1, 5)
                        tomDato = LocalDate.of(2023, 1, 8)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ), TrygdeavgiftsberegningValidering.INNTEKTSPERIODENE_KAN_IKKE_OVERLAPPE),
        )

        fun valideringsDataPerioderDekkesScenarios(): List<ValideringsInput> = listOf(
            ValideringsInput(                                                               //
                listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.of(2023, 1, 1)
                    tom = LocalDate.of(2023, 1, 8)
                }),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.of(2023, 1, 1)
                        tomDato = LocalDate.of(2023, 1, 8)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                ), listOf(Inntektsperiode().apply {
                    fomDato = LocalDate.of(2023, 1, 1)
                    tomDato = LocalDate.of(2023, 1, 4)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                    isArbeidsgiversavgiftBetalesTilSkatt = true
                    avgiftspliktigMndInntekt = mockk<Penger>()
                }, Inntektsperiode().apply {
                    fomDato = LocalDate.of(2023, 1, 5)
                    tomDato = LocalDate.of(2023, 1, 8)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                    isArbeidsgiversavgiftBetalesTilSkatt = true
                    avgiftspliktigMndInntekt = mockk<Penger>()
                }), TrygdeavgiftsberegningValidering.SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE
            ),


            ValideringsInput(                                                               // SammePeriodeForAllePerioder
                listOf(Medlemskapsperiode().apply {
                    fom = LocalDate.of(2023, 1, 1)
                    tom = LocalDate.of(2023, 1, 8)
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                }),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.of(2023, 1, 1)
                        tomDato = LocalDate.of(2023, 1, 8)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                ), listOf(
                    Inntektsperiode().apply {
                        fomDato = LocalDate.of(2023, 1, 1)
                        tomDato = LocalDate.of(2023, 1, 8)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ), ""),

            ValideringsInput(                                                               // EnMedlemskapOgInntektPeriodeToSkatteforholdPerioder
                listOf(Medlemskapsperiode().apply {
                    fom = LocalDate.of(2023, 1, 1)
                    tom = LocalDate.of(2023, 1, 8)
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                }),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.of(2023, 1, 1)
                        tomDato = LocalDate.of(2023, 1, 4)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.of(2023, 1, 5)
                        tomDato = LocalDate.of(2023, 1, 8)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ), listOf(
                    Inntektsperiode().apply {
                        fomDato = LocalDate.of(2023, 1, 1)
                        tomDato = LocalDate.of(2023, 1, 8)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ), ""),

            ValideringsInput(                                                               // EnMedlemskapOgSkatteforholdPeriodeToInntektsperioder
                listOf(Medlemskapsperiode().apply {
                    fom = LocalDate.of(2023, 1, 1)
                    tom = LocalDate.of(2023, 1, 8)
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                }),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.of(2023, 1, 1)
                        tomDato = LocalDate.of(2023, 1, 8)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                ), listOf(
                    Inntektsperiode().apply {
                        fomDato = LocalDate.of(2023, 1, 1)
                        tomDato = LocalDate.of(2023, 1, 4)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    },
                    Inntektsperiode().apply {
                        fomDato = LocalDate.of(2023, 1, 5)
                        tomDato = LocalDate.of(2023, 1, 8)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ), ""),

            ValideringsInput(                                                               // ToMedlemskapOgToSkatteforholdPeriodeOgToInntektsperioder
                listOf(Medlemskapsperiode().apply {
                    fom = LocalDate.of(2023, 1, 1)
                    tom = LocalDate.of(2023, 1, 4)
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                }, Medlemskapsperiode().apply {
                    fom = LocalDate.of(2023, 1, 5)
                    tom = LocalDate.of(2023, 1, 8)
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                }),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.of(2023, 1, 1)
                        tomDato = LocalDate.of(2023, 1, 4)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.of(2023, 1, 5)
                        tomDato = LocalDate.of(2023, 1, 8)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ), listOf(
                    Inntektsperiode().apply {
                        fomDato = LocalDate.of(2023, 1, 1)
                        tomDato = LocalDate.of(2023, 1, 4)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    },
                    Inntektsperiode().apply {
                        fomDato = LocalDate.of(2023, 1, 5)
                        tomDato = LocalDate.of(2023, 1, 8)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ), "")
        )

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
            TrygdeavgiftsberegningValidering.erAllePerioderSkattepliktige(skatteforholdsperioder) shouldBe true
        }

        @Test
        fun shouldReturnFalseWhenNotAllePerioderIsSkattepliktige() {
            val skatteforholdsperioder = listOf(
                SkatteforholdTilNorge().apply {
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }, SkatteforholdTilNorge().apply {
                    skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                })
            TrygdeavgiftsberegningValidering.erAllePerioderSkattepliktige(skatteforholdsperioder) shouldBe false
        }
    }
}
