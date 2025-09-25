package no.nav.melosys.service.avgift

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

data class EøsValideringInput(
    val helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode,
    val skatteforholdsperioder: List<SkatteforholdTilNorge>,
    val inntektsperioder: List<Inntektsperiode>,
    val feilmelding: String
) {

    override fun toString(): String {
        return feilmelding
    }
}

class EøsPensjonistTrygdeavgiftsberegningValidatorTest {

    val unleash: FakeUnleash = FakeUnleash()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ValiderForTrygdeavgiftberegning {

        @BeforeAll
        fun setup() {
            unleash.enableAll()
        }


        @ParameterizedTest
        @MethodSource("valideringsDataperiodermedFeilScenariosNyVurdering")
        fun shouldThrowExceptionWhenPerioderHarFeilNyVurdering(valideringsInput: EøsValideringInput) {
            val behandlingsresultat = Behandlingsresultat().apply {
                behandling = Behandling.forTest {
                    tema = Behandlingstema.PENSJONIST
                    status = Behandlingsstatus.OPPRETTET
                    type = Behandlingstyper.NY_VURDERING
                }
                helseutgiftDekkesPeriode = valideringsInput.helseutgiftDekkesPeriode
                årsavregning = Årsavregning.forTest()
            }

            shouldThrow<FunksjonellException> {
                EøsPensjonistTrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
                    valideringsInput.helseutgiftDekkesPeriode,
                    valideringsInput.skatteforholdsperioder,
                    valideringsInput.inntektsperioder,
                    behandlingsresultat,
                    unleash
                )
            }.message shouldBe valideringsInput.feilmelding
        }

        @Test
        fun `ny vurdering - inntekt og skatteperioder skal kun dekke inneværende og fremtidige perioder`() {
            val EøsValideringInput =
                EøsValideringInput(
                    HelseutgiftDekkesPeriode(
                        behandlingsresultat = Behandlingsresultat(),
                        fomDato = LocalDate.now().minusYears(1),
                        tomDato = LocalDate.now().plusDays(5),
                        bostedLandkode = Land_iso2.BE,
                    ),
                    listOf(
                        SkatteforholdTilNorge().apply {
                            fomDato = LocalDate.now().withDayOfYear(1)
                            tomDato = LocalDate.now().plusDays(5)
                            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                        }
                    ), listOf(Inntektsperiode().apply {
                        fomDato = LocalDate.now().withMonth(1).withDayOfMonth(1)
                        tomDato = LocalDate.now().plusDays(3)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }, Inntektsperiode().apply {
                        fomDato = LocalDate.now().plusDays(4)
                        tomDato = LocalDate.now().plusDays(5)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }), ""
                )

            val behandlingsresultat = Behandlingsresultat().apply {
                behandling = Behandling.forTest {
                    tema = Behandlingstema.PENSJONIST
                    status = Behandlingsstatus.OPPRETTET
                    type = Behandlingstyper.NY_VURDERING
                }
                helseutgiftDekkesPeriode = EøsValideringInput.helseutgiftDekkesPeriode
                årsavregning = Årsavregning.forTest()
            }

            shouldNotThrow<FunksjonellException> {
                EøsPensjonistTrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
                    EøsValideringInput.helseutgiftDekkesPeriode,
                    EøsValideringInput.skatteforholdsperioder,
                    EøsValideringInput.inntektsperioder,
                    behandlingsresultat,
                    unleash
                )
            }
        }

        fun valideringsDataperiodermedFeilScenariosNyVurdering(): List<EøsValideringInput> = listOf(
            EøsValideringInput(                                                       // Inntektskilder dekker ikke hele perioden i inneværende og fremtidige år kaster exception
                HelseutgiftDekkesPeriode(
                    mockk<Behandlingsresultat>(),
                    fomDato = LocalDate.now().minusYears(1),
                    tomDato = LocalDate.now().plusDays(5),
                    bostedLandkode = Land_iso2.BE,
                ),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now().withMonth(1).withDayOfMonth(1)
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                ), listOf(Inntektsperiode().apply {
                    fomDato = LocalDate.now().withMonth(1).withDayOfMonth(1)
                    tomDato = LocalDate.now().plusDays(2)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }, Inntektsperiode().apply {
                    fomDato = LocalDate.now().plusDays(4)
                    tomDato = LocalDate.now().plusDays(5)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }), EøsPensjonistTrygdeavgiftsberegningValidator.INNTEKT_OG_SKATT_MÅ_DEKKE_HELSEUTGIFTPERIODE_FOR_INNVÆRENDE_OG_FREMTIDIG
            ),
            EøsValideringInput(                                                       // Skatteforhold dekker ikke hele perioden i inneværende og fremtidige år kaster exception
                HelseutgiftDekkesPeriode(
                    mockk<Behandlingsresultat>(),
                    fomDato = LocalDate.now().minusYears(1),
                    tomDato = LocalDate.now().plusDays(5),
                    bostedLandkode = Land_iso2.BE,
                ),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now().withMonth(1).withDayOfMonth(1)
                        tomDato = LocalDate.now().plusDays(2)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now().plusDays(4)
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ), listOf(Inntektsperiode().apply {
                    fomDato = LocalDate.now().withMonth(1).withDayOfMonth(1)
                    tomDato = LocalDate.now().plusDays(5)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }), EøsPensjonistTrygdeavgiftsberegningValidator.INNTEKT_OG_SKATT_MÅ_DEKKE_HELSEUTGIFTPERIODE_FOR_INNVÆRENDE_OG_FREMTIDIG
            ),
            EøsValideringInput(                                                               // skatteforhold overlapper samme dag
                HelseutgiftDekkesPeriode(
                    mockk<Behandlingsresultat>(),
                    fomDato = LocalDate.now().minusYears(1),
                    tomDato = LocalDate.now().plusDays(10),
                    bostedLandkode = Land_iso2.BE,
                ),
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
                }), EøsPensjonistTrygdeavgiftsberegningValidator.SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE
            ),

            EøsValideringInput(                                                               // Skatteforholdsperiodene kan ikke overlappe
                HelseutgiftDekkesPeriode(
                    mockk<Behandlingsresultat>(),
                    fomDato = LocalDate.now().minusYears(1),
                    tomDato = LocalDate.now().plusDays(1),
                    bostedLandkode = Land_iso2.BE,
                ),
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
                }), EøsPensjonistTrygdeavgiftsberegningValidator.SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE
            ),

            EøsValideringInput(                                                       // Skatteforhold kan ikke være i tidligere år
                HelseutgiftDekkesPeriode(
                    mockk<Behandlingsresultat>(),
                    fomDato = LocalDate.now().minusYears(1),
                    tomDato = LocalDate.now().plusDays(5),
                    bostedLandkode = Land_iso2.BE,
                ),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now().minusYears(1)
                        tomDato = LocalDate.now().plusDays(2)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now().plusDays(3)
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ), listOf(Inntektsperiode().apply {
                    fomDato = LocalDate.now().withMonth(1).withDayOfMonth(1)
                    tomDato = LocalDate.now().plusDays(5)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }), TrygdeavgiftsberegningValidator.INNTEKT_OG_SKATT_IKKE_TIDLIGERE_ÅR
            ),
            EøsValideringInput(                                                       // Inntektsperioder kan ikke være i tidligere år
                HelseutgiftDekkesPeriode(
                    mockk<Behandlingsresultat>(),
                    fomDato = LocalDate.now().minusYears(1),
                    tomDato = LocalDate.now().plusDays(5),
                    bostedLandkode = Land_iso2.BE,
                ),
                listOf(
                    SkatteforholdTilNorge().apply {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                ), listOf(Inntektsperiode().apply {
                    fomDato = LocalDate.now().minusYears(1)
                    tomDato = LocalDate.now().plusDays(5)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }), TrygdeavgiftsberegningValidator.INNTEKT_OG_SKATT_IKKE_TIDLIGERE_ÅR
            ),
        )


        private fun lagGyldigBehandlingsresultat() = Behandlingsresultat().apply {
            medlemskapsperioder = listOf(
                Medlemskapsperiode(
                ).apply {
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(5)
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                })
            behandling = Behandling.forTest { status = Behandlingsstatus.OPPRETTET }
            årsavregning = Årsavregning.forTest()
        }
    }
}

