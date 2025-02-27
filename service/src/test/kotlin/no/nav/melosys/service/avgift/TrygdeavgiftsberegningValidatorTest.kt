package no.nav.melosys.service.avgift

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningValidator.INNTEKTSPERIODER_EMPTY
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningValidator.SKATTEFORHOLDSPERIODER_EMPTY
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningValidator.SKATTEPLIKTTYPE_LIK_FOR_ALLE_PERIODER
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

data class ValideringInput(
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

class TrygdeavgiftsberegningValidatorTest {

    val unleash: FakeUnleash = FakeUnleash()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ValiderForTrygdeavgiftberegning {

        @BeforeAll
        fun setup() {
            unleash.enableAll()
        }

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
                TrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
                    behandlingsresultatMock,
                    skatteforholdsPerioder,
                    emptyList(),
                    unleash
                )
            }.message shouldBe TrygdeavgiftsberegningValidator.MEDLEMSKAPSPERIODER_EMPTY
        }

        @Test
        fun shouldThrowFunksjonellExceptionWhenInntektsperioderIsPensjon() {
            val behandlingsresultatMock = lagGyldigBehandlingsresultat()

            val skatteforholdsPerioder = listOf(
                SkatteforholdTilNorge().apply {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now()
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }
            )

            val inntektsperioder = listOf(Inntektsperiode().apply {
                fomDato = LocalDate.now()
                tomDato = LocalDate.now().plusDays(2)
                type = Inntektskildetype.PENSJON_UFØRETRYGD
            })

            shouldThrow<FunksjonellException> {
                TrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
                    behandlingsresultatMock,
                    skatteforholdsPerioder,
                    inntektsperioder,
                    unleash
                )
            }.message shouldBe TrygdeavgiftsberegningValidator.MINST_EN_ANNEN_INNTEKT_I_TILLEGG_TIL_PENSJON
        }

        @Test
        fun shouldThrowFunksjonellExceptionWhenUtledMedlemskapsperiodeFomIsNull() {
            val behandlingsresultatMock = mockk<Behandlingsresultat>()
            every { behandlingsresultatMock.medlemskapsperioder } returns listOf(Medlemskapsperiode().apply {
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
            })
            every { behandlingsresultatMock.utledMedlemskapsperiodeFom() } returns null
            every { behandlingsresultatMock.årsavregning } returns Årsavregning()

            val skatteforholdsPerioder = listOf(
                SkatteforholdTilNorge().apply {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now()
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }
            )

            shouldThrow<FunksjonellException> {
                TrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(behandlingsresultatMock, skatteforholdsPerioder, listOf(), unleash)
            }.message shouldBe TrygdeavgiftsberegningValidator.UTLED_MEDLEMSKAPSPERIODE_FOM_MANGLER
        }

        @Test
        fun shouldThrowFunksjonellExceptionWhenUtledMedlemskapsperiodeTomIsNull() {
            val behandlingsresultatMock = mockk<Behandlingsresultat>()
            every { behandlingsresultatMock.medlemskapsperioder } returns listOf(Medlemskapsperiode().apply {
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
            })
            every { behandlingsresultatMock.utledMedlemskapsperiodeFom() } returns LocalDate.now()
            every { behandlingsresultatMock.utledMedlemskapsperiodeTom() } returns null
            every { behandlingsresultatMock.årsavregning } returns Årsavregning()

            val skatteforholdsPerioder = listOf(
                SkatteforholdTilNorge().apply {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now()
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }
            )

            shouldThrow<FunksjonellException> {
                TrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(behandlingsresultatMock, skatteforholdsPerioder, listOf(), unleash)
            }.message shouldBe TrygdeavgiftsberegningValidator.UTLED_MEDLEMSKAPSPERIODE_TOM_MANGLER
        }

        @ParameterizedTest
        @MethodSource("valideringsDataperiodermedFeilScenarios")
        fun shouldThrowExceptionWhenPerioderHarFeil(valideringsInput: ValideringInput) {
            val behandlingsresultat = Behandlingsresultat().apply {
                behandling = Behandling().apply {
                    tema = Behandlingstema.PENSJONIST
                }
                medlemskapsperioder = valideringsInput.medlemskapsperioder
                årsavregning = Årsavregning()
            }

            shouldThrow<FunksjonellException> {
                TrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
                    behandlingsresultat,
                    valideringsInput.skatteforholdsperioder,
                    valideringsInput.inntektsperioder,
                    unleash
                )
            }.message shouldBe valideringsInput.feilmelding
        }

        @ParameterizedTest
        @MethodSource("valideringsDataPerioderDekkesScenarios")
        fun shouldBeValidPeriodeWhenInntektsperioderDekkerHelePerioden(valideringInput: ValideringInput) {
            val behandlingsresultat = Behandlingsresultat().apply {
                behandling = Behandling().apply {
                    tema = Behandlingstema.ARBEID_KUN_NORGE
                }
                medlemskapsperioder = valideringInput.medlemskapsperioder
            }

            shouldNotThrow<FunksjonellException> {
                TrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
                    behandlingsresultat,
                    valideringInput.skatteforholdsperioder,
                    valideringInput.inntektsperioder,
                    unleash
                )
            }
        }

        @ParameterizedTest
        @MethodSource("valideringsData")
        fun shouldThrowFunksjonellExceptionWhenInntektsperioderIsEmpty(valideringsInput: ValideringInput) {
            val behandlingsresultatMock = lagGyldigBehandlingsresultat()
            val skatteforholdsperioder = valideringsInput.skatteforholdsperioder
            val inntektsperioder = valideringsInput.inntektsperioder

            shouldThrow<FunksjonellException> {
                TrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
                    behandlingsresultatMock,
                    skatteforholdsperioder,
                    inntektsperioder,
                    unleash
                )
            }.message shouldBe valideringsInput.feilmelding
        }

        fun valideringsDataperiodermedFeilScenarios(): List<ValideringInput> = listOf(
            ValideringInput(                                                       // Inntektskilder dekker ikke hele perioden kaster exception
                listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(5)
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                }),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                ), listOf(Inntektsperiode().apply {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now().plusDays(2)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }, Inntektsperiode().apply {
                    fomDato = LocalDate.now().plusDays(4)
                    tomDato = LocalDate.now().plusDays(5)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }), TrygdeavgiftsberegningValidator.INNTEKTSPERIODE_DEKKER_IKKE_HELE_PERIODEN
            ),
            ValideringInput(                                                       // Skatteforhold dekker ikke hele perioden kaster exception
                listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(5)
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                }),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(2)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now().plusDays(4)
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ), listOf(Inntektsperiode().apply {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now().plusDays(5)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }), TrygdeavgiftsberegningValidator.SKATTEFORHOLDSPERIODE_DEKKER_IKKE_HELE_PERIODEN
            ),
            ValideringInput(                                                               // skatteforhold overlapper samme dag
                listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(10)
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
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
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }), TrygdeavgiftsberegningValidator.SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE
            ),

            ValideringInput(                                                               // Skatteforholdsperiodene kan ikke overlappe
                listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(1)
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
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
                ), listOf(Inntektsperiode().apply {
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }), TrygdeavgiftsberegningValidator.SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE
            ),

            ValideringInput(                                                               // Inntektsperiode kan ikke være utenfor periode
                listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(1)
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                }),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(1)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                ),
                listOf(Inntektsperiode().apply {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now().plusDays(3)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                    isArbeidsgiversavgiftBetalesTilSkatt = true
                    avgiftspliktigMndInntekt = mockk<Penger>()
                }), TrygdeavgiftsberegningValidator.INNTEKTSPERIODE_ER_UTENFOR_MEDLEMSKAPSPERIODE
            ),

            ValideringInput(                                                               // Bestemmelser kan ikke være ulike for medlemskapsperioder kan ikke være utenfor periode
                listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(1)
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                },
                    Medlemskapsperiode().apply {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now()
                        tom = LocalDate.now().plusDays(1)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5
                    }),
                listOf(SkatteforholdTilNorge().apply {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now().plusDays(1)
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }),
                listOf(Inntektsperiode().apply {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now().plusDays(3)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                    isArbeidsgiversavgiftBetalesTilSkatt = true
                    avgiftspliktigMndInntekt = mockk<Penger>()
                }), TrygdeavgiftsberegningValidator.MEDLEMSKAPSPERIODER_HAR_FORSKJELLIGE_BESTEMMELSER
            ),


            ValideringInput(                                                               // Skatteforhold dekker ikke hele perioden
                listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(3)
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
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
                ), listOf(Inntektsperiode().apply {
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }), TrygdeavgiftsberegningValidator.SKATTEFORHOLDSPERIODE_DEKKER_IKKE_HELE_PERIODEN
            ),

            ValideringInput(                                                               // Inntektsperioder dekker ikke hele perioden
                listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(3)
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
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
                }), TrygdeavgiftsberegningValidator.INNTEKTSPERIODE_DEKKER_IKKE_HELE_PERIODEN
            ),
        )

        private fun valideringsDataPerioderDekkesScenarios(): List<ValideringInput> = listOf(
            ValideringInput(                                                       // Inntektsperioder skal kunne overlappe
                listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(30)
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                }),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(30)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                ), listOf(Inntektsperiode().apply {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now().plusDays(30)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }, Inntektsperiode().apply {
                    fomDato = LocalDate.now().plusDays(2)
                    tomDato = LocalDate.now().plusDays(5)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }), ""
            ),


            ValideringInput(                                                       // Inntektsperioder skal kunne overlappe
                listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(5)
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                }),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                ), listOf(Inntektsperiode().apply {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now().plusDays(3)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }, Inntektsperiode().apply {
                    fomDato = LocalDate.now().plusDays(2)
                    tomDato = LocalDate.now().plusDays(5)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }), ""
            ),

            ValideringInput(
                listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.of(2023, 3, 3)
                    tom = LocalDate.of(2023, 3, 5)
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                }),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.of(2023, 3, 3)
                        tomDato = LocalDate.of(2023, 3, 3)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }, SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.of(2023, 3, 4)
                        tomDato = LocalDate.of(2023, 3, 5)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ), listOf(Inntektsperiode().apply {
                    fomDato = LocalDate.of(2023, 3, 3)
                    tomDato = LocalDate.of(2023, 3, 4)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                    isArbeidsgiversavgiftBetalesTilSkatt = true
                    avgiftspliktigMndInntekt = mockk<Penger>()
                }, Inntektsperiode().apply {
                    fomDato = LocalDate.of(2023, 3, 5)
                    tomDato = LocalDate.of(2023, 3, 5)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                    isArbeidsgiversavgiftBetalesTilSkatt = true
                    avgiftspliktigMndInntekt = mockk<Penger>()
                }), ""
            ),

            ValideringInput(
                listOf(Medlemskapsperiode().apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.of(2023, 1, 1)
                    tom = LocalDate.of(2023, 1, 8)
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
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
                }), ""
            ),

            ValideringInput(                                                               // SammePeriodeForAllePerioder
                listOf(Medlemskapsperiode().apply {
                    fom = LocalDate.of(2023, 1, 1)
                    tom = LocalDate.of(2023, 1, 8)
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
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

            ValideringInput(                                                               // EnMedlemskapOgInntektPeriodeToSkatteforholdPerioder
                listOf(Medlemskapsperiode().apply {
                    fom = LocalDate.of(2023, 1, 1)
                    tom = LocalDate.of(2023, 1, 8)
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
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

            ValideringInput(                                                               // EnMedlemskapOgSkatteforholdPeriodeToInntektsperioder
                listOf(Medlemskapsperiode().apply {
                    fom = LocalDate.of(2023, 1, 1)
                    tom = LocalDate.of(2023, 1, 8)
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
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

            ValideringInput(                                                               // ToMedlemskapOgToSkatteforholdPeriodeOgToInntektsperioder
                listOf(Medlemskapsperiode().apply {
                    fom = LocalDate.of(2023, 1, 1)
                    tom = LocalDate.of(2023, 1, 4)
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                }, Medlemskapsperiode().apply {
                    fom = LocalDate.of(2023, 1, 5)
                    tom = LocalDate.of(2023, 1, 8)
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
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

        fun valideringsData(): List<ValideringInput> = listOf(

            ValideringInput(
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
            ValideringInput(
                emptyList(),
                listOf(Inntektsperiode().apply {
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }),
                SKATTEFORHOLDSPERIODER_EMPTY
            ),
            ValideringInput(
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
                listOf(Inntektsperiode().apply {
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }),
                SKATTEPLIKTTYPE_LIK_FOR_ALLE_PERIODER
            )
        )

        private fun lagGyldigBehandlingsresultat() = Behandlingsresultat().apply {
            medlemskapsperioder = listOf(
                Medlemskapsperiode(
                ).apply {
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(5)
                })
            behandling = Behandling()
            årsavregning = Årsavregning()
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
            TrygdeavgiftsberegningValidator.erAllePerioderSkattepliktige(skatteforholdsperioder) shouldBe true
        }

        @Test
        fun shouldReturnFalseWhenNotAllePerioderIsSkattepliktige() {
            val skatteforholdsperioder = listOf(
                SkatteforholdTilNorge().apply {
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }, SkatteforholdTilNorge().apply {
                    skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                })
            TrygdeavgiftsberegningValidator.erAllePerioderSkattepliktige(skatteforholdsperioder) shouldBe false
        }
    }
}
