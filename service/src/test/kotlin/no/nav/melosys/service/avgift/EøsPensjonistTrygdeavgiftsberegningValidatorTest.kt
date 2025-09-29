package no.nav.melosys.service.avgift

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EøsPensjonistTrygdeavgiftsberegningValidatorTest {

    private val unleash: FakeUnleash = FakeUnleash()

    @BeforeAll
    fun setup() {
        unleash.enableAll()
    }

    @ParameterizedTest(name = "{index} - {argumentSetName}")
    @MethodSource("valideringsDataperiodermedFeilScenariosNyVurdering")
    fun `validering kaster forventet exception med korrekt melding`(
        testCase: EøsValideringsTestCase,
        expectedError: String?
    ) {
        val behandlingsresultat = opprettBehandlingsresultat(testCase)

        if (expectedError != null) {
            shouldThrow<FunksjonellException> {
                EøsPensjonistTrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
                    testCase.helseutgiftDekkesPeriode,
                    testCase.skatteforholdsperioder,
                    testCase.inntektsperioder,
                    behandlingsresultat,
                    unleash
                )
            }.message shouldBe expectedError
        } else {
            shouldNotThrow<FunksjonellException> {
                EøsPensjonistTrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
                    testCase.helseutgiftDekkesPeriode,
                    testCase.skatteforholdsperioder,
                    testCase.inntektsperioder,
                    behandlingsresultat,
                    unleash
                )
            }
        }
    }

    @Test
    fun `ny vurdering - inntekt og skatteperioder skal kun dekke inneværende og fremtidige perioder`() {
        val testCase = eøsValideringsTestCase {
            helseutgiftPeriode {
                fomDato = LocalDate.now().minusYears(1)
                tomDato = LocalDate.now().plusDays(5)
                bostedLandkode = Land_iso2.BE
            }
            skatteforhold {
                fomDato = LocalDate.now().withDayOfYear(1)
                tomDato = LocalDate.now().plusDays(5)
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
            inntektsperioder {
                periode {
                    fomDato = LocalDate.now().withMonth(1).withDayOfMonth(1)
                    tomDato = LocalDate.now().plusDays(3)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }
                periode {
                    fomDato = LocalDate.now().plusDays(4)
                    tomDato = LocalDate.now().plusDays(5)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }
            }
        }

        val behandlingsresultat = opprettBehandlingsresultat(testCase)

        shouldNotThrow<FunksjonellException> {
            EøsPensjonistTrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
                testCase.helseutgiftDekkesPeriode,
                testCase.skatteforholdsperioder,
                testCase.inntektsperioder,
                behandlingsresultat,
                unleash
            )
        }
    }

    fun valideringsDataperiodermedFeilScenariosNyVurdering(): List<Arguments.ArgumentSet> = listOf(
        eøsValideringsScenario {
            name = "Inntektskilder dekker ikke hele perioden i inneværende og fremtidige år"
            testCase = eøsValideringsTestCase {
                helseutgiftPeriode {
                    fomDato = LocalDate.now().minusYears(1)
                    tomDato = LocalDate.now().plusDays(5)
                    bostedLandkode = Land_iso2.BE
                }
                skatteforhold {
                    fomDato = LocalDate.now().withMonth(1).withDayOfMonth(1)
                    tomDato = LocalDate.now().plusDays(5)
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }
                inntektsperioder {
                    periode {
                        fomDato = LocalDate.now().withMonth(1).withDayOfMonth(1)
                        tomDato = LocalDate.now().plusDays(2)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                    periode {
                        fomDato = LocalDate.now().plusDays(4)
                        tomDato = LocalDate.now().plusDays(5)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                }
            }
            expectedError = EøsPensjonistTrygdeavgiftsberegningValidator.INNTEKT_OG_SKATT_MÅ_DEKKE_HELSEUTGIFTPERIODE_FOR_INNEVÆRENDE_OG_FREMTIDIG
        },

        eøsValideringsScenario {
            name = "Skatteforhold dekker ikke hele perioden i inneværende og fremtidige år"
            testCase = eøsValideringsTestCase {
                helseutgiftPeriode {
                    fomDato = LocalDate.now().minusYears(1)
                    tomDato = LocalDate.now().plusDays(5)
                    bostedLandkode = Land_iso2.BE
                }
                skatteforholdsperioder {
                    periode {
                        fomDato = LocalDate.now().withMonth(1).withDayOfMonth(1)
                        tomDato = LocalDate.now().plusDays(2)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                    periode {
                        fomDato = LocalDate.now().plusDays(4)
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
                inntektsperioder {
                    periode {
                        fomDato = LocalDate.now().withMonth(1).withDayOfMonth(1)
                        tomDato = LocalDate.now().plusDays(5)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                }
            }
            expectedError = EøsPensjonistTrygdeavgiftsberegningValidator.INNTEKT_OG_SKATT_MÅ_DEKKE_HELSEUTGIFTPERIODE_FOR_INNEVÆRENDE_OG_FREMTIDIG
        },

        eøsValideringsScenario {
            name = "Skatteforhold overlapper samme dag"
            testCase = eøsValideringsTestCase {
                helseutgiftPeriode {
                    fomDato = LocalDate.now().minusYears(1)
                    tomDato = LocalDate.now().plusDays(10)
                    bostedLandkode = Land_iso2.BE
                }
                skatteforholdsperioder {
                    periode {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                    periode {
                        fomDato = LocalDate.now().plusDays(5)
                        tomDato = LocalDate.now().plusDays(10)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
                inntektsperioder {
                    periode {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(10)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                }
            }
            expectedError = EøsPensjonistTrygdeavgiftsberegningValidator.SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE
        },

        eøsValideringsScenario {
            name = "Skatteforholdsperiodene kan ikke overlappe - eksakt samme periode"
            testCase = eøsValideringsTestCase {
                helseutgiftPeriode {
                    fomDato = LocalDate.now().minusYears(1)
                    tomDato = LocalDate.now().plusDays(1)
                    bostedLandkode = Land_iso2.BE
                }
                skatteforholdsperioder {
                    periode {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(1)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                    periode {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(1)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
                inntektsperioder {
                    periode {
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                }
            }
            expectedError = EøsPensjonistTrygdeavgiftsberegningValidator.SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE
        },

        eøsValideringsScenario {
            name = "Skatteforhold kan ikke være i tidligere år"
            testCase = eøsValideringsTestCase {
                helseutgiftPeriode {
                    fomDato = LocalDate.now().minusYears(1)
                    tomDato = LocalDate.now().plusDays(5)
                    bostedLandkode = Land_iso2.BE
                }
                skatteforholdsperioder {
                    periode {
                        fomDato = LocalDate.now().minusYears(1)
                        tomDato = LocalDate.now().plusDays(2)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                    periode {
                        fomDato = LocalDate.now().plusDays(3)
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
                inntektsperioder {
                    periode {
                        fomDato = LocalDate.now().withMonth(1).withDayOfMonth(1)
                        tomDato = LocalDate.now().plusDays(5)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                }
            }
            expectedError = TrygdeavgiftsberegningValidator.INNTEKT_OG_SKATT_IKKE_TIDLIGERE_ÅR
        },

        eøsValideringsScenario {
            name = "Inntektsperioder kan ikke være i tidligere år"
            testCase = eøsValideringsTestCase {
                helseutgiftPeriode {
                    fomDato = LocalDate.now().minusYears(1)
                    tomDato = LocalDate.now().plusDays(5)
                    bostedLandkode = Land_iso2.BE
                }
                skatteforholdsperioder {
                    periode {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                }
                inntektsperioder {
                    periode {
                        fomDato = LocalDate.now().minusYears(1)
                        tomDato = LocalDate.now().plusDays(5)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                }
            }
            expectedError = TrygdeavgiftsberegningValidator.INNTEKT_OG_SKATT_IKKE_TIDLIGERE_ÅR
        }
    )

    private fun opprettBehandlingsresultat(testCase: EøsValideringsTestCase) = Behandlingsresultat().apply {
        behandling = Behandling.forTest {
            tema = Behandlingstema.PENSJONIST
            status = Behandlingsstatus.OPPRETTET
            type = Behandlingstyper.NY_VURDERING
        }
        helseutgiftDekkesPeriode = testCase.helseutgiftDekkesPeriode
        årsavregning = Årsavregning.forTest()
    }

    private fun eøsValideringsScenario(init: EøsValideringsScenarioBuilder.() -> Unit): Arguments.ArgumentSet =
        EøsValideringsScenarioBuilder().apply(init).build()

    private fun eøsValideringsTestCase(init: EøsValideringsTestCaseBuilder.() -> Unit): EøsValideringsTestCase =
        EøsValideringsTestCaseBuilder().apply(init).build()

    class EøsValideringsScenarioBuilder {
        lateinit var name: String
        lateinit var testCase: EøsValideringsTestCase
        var expectedError: String? = null

        fun build(): Arguments.ArgumentSet = argumentSet(name, testCase, expectedError)
    }

    class EøsValideringsTestCaseBuilder {
        private var helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode? = null
        private val skatteforholdsperioderList = mutableListOf<SkatteforholdTilNorge>()
        private val inntektsperioderList = mutableListOf<Inntektsperiode>()

        fun helseutgiftPeriode(init: HelseutgiftDekkesPeriode.() -> Unit) {
            helseutgiftDekkesPeriode = HelseutgiftDekkesPeriode.forTest {
                behandlingsresultat = mockk<Behandlingsresultat>()
            }.apply(init)
        }

        fun skatteforhold(init: SkatteforholdTilNorge.() -> Unit) {
            skatteforholdsperioderList.add(createSkatteforhold(init))
        }

        fun skatteforholdsperioder(init: SkatteforholdsperioderBuilder.() -> Unit) {
            SkatteforholdsperioderBuilder(skatteforholdsperioderList).apply(init)
        }

        fun inntektsperioder(init: InntektsperioderBuilder.() -> Unit) {
            InntektsperioderBuilder(inntektsperioderList).apply(init)
        }

        fun build(): EøsValideringsTestCase = EøsValideringsTestCase(
            helseutgiftDekkesPeriode = helseutgiftDekkesPeriode ?: throw IllegalStateException("helseutgiftPeriode må settes"),
            skatteforholdsperioder = skatteforholdsperioderList.toList(),
            inntektsperioder = inntektsperioderList.toList()
        )
    }

    class SkatteforholdsperioderBuilder(private val list: MutableList<SkatteforholdTilNorge>) {
        fun periode(init: SkatteforholdTilNorge.() -> Unit) {
            list.add(createSkatteforhold(init))
        }
    }

    class InntektsperioderBuilder(private val list: MutableList<Inntektsperiode>) {
        fun periode(init: Inntektsperiode.() -> Unit) {
            list.add(createInntektsperiode(init))
        }
    }

    data class EøsValideringsTestCase(
        val helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode,
        val skatteforholdsperioder: List<SkatteforholdTilNorge>,
        val inntektsperioder: List<Inntektsperiode>
    )

    companion object {
        private fun createSkatteforhold(init: SkatteforholdTilNorge.() -> Unit): SkatteforholdTilNorge =
            SkatteforholdTilNorge().apply {
                fomDato = LocalDate.now()
                tomDato = LocalDate.now().plusDays(30)
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }.apply(init)

        private fun createInntektsperiode(init: Inntektsperiode.() -> Unit): Inntektsperiode =
            Inntektsperiode().apply {
                fomDato = LocalDate.now()
                tomDato = LocalDate.now().plusDays(30)
                type = Inntektskildetype.ARBEIDSINNTEKT
                isArbeidsgiversavgiftBetalesTilSkatt = false
            }.apply(init)
    }
}
