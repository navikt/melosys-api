package no.nav.melosys.service.avgift

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.inntektForTest
import no.nav.melosys.domain.avgift.skatteforholdForTest
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.helseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.årsavregning
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
                    behandlingsresultat.helseutgiftDekkesPerioder.first(),
                    testCase.skatteforholdsperioder,
                    testCase.inntektsperioder,
                    behandlingsresultat,
                    unleash
                )
            }.message shouldBe expectedError
        } else {
            shouldNotThrow<FunksjonellException> {
                EøsPensjonistTrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
                    behandlingsresultat.helseutgiftDekkesPerioder.first(),
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
                behandlingsresultat.helseutgiftDekkesPerioder.first(),
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
                    skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
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

    private fun opprettBehandlingsresultat(testCase: EøsValideringsTestCase) = Behandlingsresultat.forTest {
        behandling {
            tema = Behandlingstema.PENSJONIST
            status = Behandlingsstatus.OPPRETTET
            type = Behandlingstyper.NY_VURDERING
        }
        helseutgiftDekkesPeriode {
            fomDato = testCase.helseutgiftDekkesPeriode.fomDato
            tomDato = testCase.helseutgiftDekkesPeriode.tomDato
            bostedLandkode = testCase.helseutgiftDekkesPeriode.bostedLandkode
        }
        årsavregning { }
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
        private var helseutgiftDekkesPeriode: HelseutgiftDekkesPeriodeData? = null
        private val skatteforholdsperioderList = mutableListOf<SkatteforholdTilNorge>()
        private val inntektsperioderList = mutableListOf<Inntektsperiode>()

        fun helseutgiftPeriode(init: HelseutgiftDekkesPeriodeBuilder.() -> Unit) {
            helseutgiftDekkesPeriode = HelseutgiftDekkesPeriodeBuilder().apply(init).build()
        }

        fun skatteforhold(init: SkatteforholdBuilder.() -> Unit) {
            skatteforholdsperioderList.add(SkatteforholdBuilder().apply(init).build())
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

    class HelseutgiftDekkesPeriodeBuilder {
        var fomDato: LocalDate = LocalDate.now()
        var tomDato: LocalDate = LocalDate.now().plusDays(1)
        var bostedLandkode: Land_iso2 = Land_iso2.NO

        fun build() = HelseutgiftDekkesPeriodeData(fomDato, tomDato, bostedLandkode)
    }

    class SkatteforholdBuilder {
        var fomDato: LocalDate = LocalDate.now()
        var tomDato: LocalDate = LocalDate.now().plusDays(30)
        var skatteplikttype: Skatteplikttype = Skatteplikttype.SKATTEPLIKTIG

        fun build(): SkatteforholdTilNorge = skatteforholdForTest {
            this.fomDato = this@SkatteforholdBuilder.fomDato
            this.tomDato = this@SkatteforholdBuilder.tomDato
            this.skatteplikttype = this@SkatteforholdBuilder.skatteplikttype
        }
    }

    class InntektBuilder {
        var fomDato: LocalDate = LocalDate.now()
        var tomDato: LocalDate = LocalDate.now().plusDays(30)
        var type: Inntektskildetype = Inntektskildetype.ARBEIDSINNTEKT

        fun build(): Inntektsperiode = inntektForTest {
            this.fomDato = this@InntektBuilder.fomDato
            this.tomDato = this@InntektBuilder.tomDato
            this.type = this@InntektBuilder.type
        }
    }

    class SkatteforholdsperioderBuilder(private val list: MutableList<SkatteforholdTilNorge>) {
        fun periode(init: SkatteforholdBuilder.() -> Unit) {
            list.add(SkatteforholdBuilder().apply(init).build())
        }
    }

    class InntektsperioderBuilder(private val list: MutableList<Inntektsperiode>) {
        fun periode(init: InntektBuilder.() -> Unit) {
            list.add(InntektBuilder().apply(init).build())
        }
    }

    data class HelseutgiftDekkesPeriodeData(
        val fomDato: LocalDate,
        val tomDato: LocalDate,
        val bostedLandkode: Land_iso2
    )

    data class EøsValideringsTestCase(
        val helseutgiftDekkesPeriode: HelseutgiftDekkesPeriodeData,
        val skatteforholdsperioder: List<SkatteforholdTilNorge>,
        val inntektsperioder: List<Inntektsperiode>
    )
}
