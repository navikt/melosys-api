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
import no.nav.melosys.domain.avgift.AvgiftspliktigPeriode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.avgift.forTest
import no.nav.melosys.domain.avgift.inntektForTest
import no.nav.melosys.domain.avgift.skatteforholdForTest
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.medlemskapsperiode
import no.nav.melosys.domain.medlemskapsperiodeForTest
import no.nav.melosys.domain.årsavregning
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningValidator.BEHANDLING_IKKE_AKTIV
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
    val feilmelding: String,
    val beskrivelse: String = ""
) {
    constructor(
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsperioder: List<Inntektsperiode>,
        feilmelding: String
    ) : this(emptyList<Medlemskapsperiode>(), skatteforholdsperioder, inntektsperioder, feilmelding)

    override fun toString(): String {
        if (beskrivelse.isNotEmpty()) return beskrivelse
        if (feilmelding.isNotEmpty()) return feilmelding

        val medlemskapInfo = if (medlemskapsperioder.isNotEmpty()) {
            "Medlemskap: ${medlemskapsperioder.size} periode(r) ${medlemskapsperioder.first().fom}→${medlemskapsperioder.last().tom}"
        } else "Ingen medlemskap"

        val skatteInfo = "Skatt: ${skatteforholdsperioder.size} periode(r)"
        val inntektInfo = "Inntekt: ${inntektsperioder.size} periode(r)"

        return "$medlemskapInfo, $skatteInfo, $inntektInfo"
    }
}

class TrygdeavgiftsberegningValidatorTest {

    val unleash: FakeUnleash = FakeUnleash()
    val inneværendeÅr = LocalDate.now().year

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ValiderForTrygdeavgiftberegning {

        @BeforeAll
        fun setup() {
            unleash.enableAll()
        }

        @Test
        fun shouldThrowFunksjonellExceptionWhenAvgiftspliktigperioderIsEmpty() {
            val behandlingsresultatMock = mockk<Behandlingsresultat>()
            val behandling = Behandling.forTest {
                status = Behandlingsstatus.OPPRETTET
                fagsak {
                    type = Sakstyper.FTRL
                }
            }
            every { behandlingsresultatMock.behandling } returns behandling
            every { behandlingsresultatMock.hentBehandling() } returns behandling
            every { behandlingsresultatMock.finnAvgiftspliktigPerioder() } returns listOf()
            every { behandlingsresultatMock.utledAvgiftspliktigperioderFom() } returns LocalDate.now()

            val skatteforholdsPerioder = listOf(
                skatteforholdForTest {
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
            }.message shouldBe TrygdeavgiftsberegningValidator.AVGIFTSPLIKTIGPERIODER_EMPTY
        }

        @Test
        fun shouldThrowFunksjonellExceptionWhenInntektsperioderIsPensjon() {
            val behandlingsresultatMock = lagGyldigBehandlingsresultat()
            val medlemskapsperiode = behandlingsresultatMock.medlemskapsperioder.first()

            val skatteforholdsPerioder = listOf(
                skatteforholdForTest {
                    fomDato = medlemskapsperiode.fom
                    tomDato = medlemskapsperiode.tom
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }
            )

            val inntektsperioder = listOf(
                inntektForTest {
                    fomDato = medlemskapsperiode.fom
                    tomDato = medlemskapsperiode.tom!!.plusDays(2)
                    type = Inntektskildetype.PENSJON_UFØRETRYGD
                }
            )

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
        fun kastFeilmeldingNårMedlemskapsperioderHarOpphold() {
            val behandlingsresultat = lagGyldigBehandlingsresultat()

            val skatteforholdsPerioder = listOf(
                skatteforholdForTest {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now()
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }
            )

            val inntektsperioder = listOf(
                inntektForTest {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now().plusDays(2)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }
            )

            behandlingsresultat.medlemskapsperioder = mutableSetOf(
                medlemskapsperiodeForTest {
                    fom = LocalDate.now()
                    tom = LocalDate.now().plusDays(5)
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                },
                medlemskapsperiodeForTest {
                    fom = LocalDate.now().plusDays(7)
                    tom = LocalDate.now().plusDays(10)
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                }
            )

            shouldThrow<FunksjonellException> {
                TrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
                    behandlingsresultat,
                    skatteforholdsPerioder,
                    inntektsperioder,
                    unleash
                )
            }.message shouldBe TrygdeavgiftsberegningValidator.MEDLEMSKAPSPERIODER_HAR_OPPHOLD
        }

        @Test
        fun shouldThrowFunksjonellExceptionWhenUtledAvgiftspliktigperiodeFomIsNull() {
            val behandlingsresultatMock = mockk<Behandlingsresultat>()
            val behandling = Behandling.forTest {
                status = Behandlingsstatus.OPPRETTET
                fagsak {
                    type = Sakstyper.FTRL
                }
            }
            val medlemskap = medlemskapsperiodeForTest {
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
            }
            every { behandlingsresultatMock.behandling } returns behandling
            every { behandlingsresultatMock.hentBehandling() } returns behandling
            every { behandlingsresultatMock.medlemskapsperioder } returns mutableSetOf(medlemskap)
            every { behandlingsresultatMock.finnAvgiftspliktigPerioder() } returns (behandlingsresultatMock.medlemskapsperioder as MutableSet<AvgiftspliktigPeriode>).toList()
            every { behandlingsresultatMock.utledAvgiftspliktigperioderFom() } returns null
            every { behandlingsresultatMock.årsavregning } returns Årsavregning.forTest()

            val skatteforholdsPerioder = listOf(
                skatteforholdForTest {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now()
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }
            )

            shouldThrow<FunksjonellException> {
                TrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(behandlingsresultatMock, skatteforholdsPerioder, listOf(), unleash)
            }.message shouldBe TrygdeavgiftsberegningValidator.UTLED_AVGIFTSPLIKTIGPERIODE_FOM_MANGLER
        }

        @Test
        fun shouldThrowFunksjonellExceptionWhenUtledAvgiftspliktigperiodeTomIsNull() {
            val behandlingsresultatMock = mockk<Behandlingsresultat>()
            val behandling = Behandling.forTest {
                status = Behandlingsstatus.OPPRETTET
                fagsak {
                    type = Sakstyper.FTRL
                }
            }
            val medlemskap = medlemskapsperiodeForTest {
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
            }
            every { behandlingsresultatMock.behandling } returns behandling
            every { behandlingsresultatMock.hentBehandling() } returns behandling
            every { behandlingsresultatMock.medlemskapsperioder } returns mutableSetOf(medlemskap)
            every { behandlingsresultatMock.finnAvgiftspliktigPerioder() } returns (behandlingsresultatMock.medlemskapsperioder as MutableSet<AvgiftspliktigPeriode>).toList()
            every { behandlingsresultatMock.utledAvgiftspliktigperioderFom() } returns LocalDate.now()
            every { behandlingsresultatMock.utledAvgiftspliktigperioderTom() } returns null
            every { behandlingsresultatMock.årsavregning } returns Årsavregning.forTest()

            val skatteforholdsPerioder = listOf(
                skatteforholdForTest {
                    fomDato = LocalDate.now()
                    tomDato = LocalDate.now()
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }
            )

            shouldThrow<FunksjonellException> {
                TrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(behandlingsresultatMock, skatteforholdsPerioder, listOf(), unleash)
            }.message shouldBe TrygdeavgiftsberegningValidator.UTLED_AVGIFTPLIKTIGPERIODE_TOM_MANGLER
        }

        @ParameterizedTest
        @MethodSource("valideringsDataperiodermedFeilScenarios")
        fun shouldThrowExceptionWhenPerioderHarFeil(valideringsInput: ValideringInput) {
            val behandlingsresultat = Behandlingsresultat.forTest {
                behandling {
                    tema = Behandlingstema.PENSJONIST
                    status = Behandlingsstatus.OPPRETTET
                    type = Behandlingstyper.FØRSTEGANG
                    fagsak {
                        type = Sakstyper.FTRL
                    }
                }
                valideringsInput.medlemskapsperioder.forEach { mp ->
                    medlemskapsperiode {
                        fom = mp.fom!!
                        tom = mp.tom!!
                        innvilgelsesresultat = mp.innvilgelsesresultat!!
                        bestemmelse = mp.bestemmelse!!
                    }
                }
                årsavregning { }
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
        @MethodSource("valideringsDataperiodermedFeilScenariosNyVurdering")
        fun shouldThrowExceptionWhenPerioderHarFeilNyVurdering(valideringsInput: ValideringInput) {
            val behandlingsresultat = Behandlingsresultat.forTest {
                behandling {
                    tema = Behandlingstema.PENSJONIST
                    status = Behandlingsstatus.OPPRETTET
                    type = Behandlingstyper.NY_VURDERING
                    fagsak {
                        type = Sakstyper.FTRL
                    }
                }
                valideringsInput.medlemskapsperioder.forEach { mp ->
                    medlemskapsperiode {
                        fom = mp.fom!!
                        tom = mp.tom!!
                        innvilgelsesresultat = mp.innvilgelsesresultat!!
                        bestemmelse = mp.bestemmelse!!
                    }
                }
                årsavregning { }
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

        @Test
        fun `ny vurdering - inntekt og skatteperioder skal kun dekke inneværende og fremtidige perioder`() {
            val valideringInput =
                ValideringInput(
                    listOf(
                        medlemskapsperiodeForTest {
                            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                            fom = LocalDate.now().minusYears(1)
                            tom = LocalDate.now().plusDays(5)
                            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                        }
                    ),
                    listOf(
                        skatteforholdForTest {
                            fomDato = LocalDate.now().withMonth(1).withDayOfMonth(1)
                            tomDato = LocalDate.now().plusDays(5)
                            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                        }
                    ),
                    listOf(
                        inntektForTest {
                            fomDato = LocalDate.now().withMonth(1).withDayOfMonth(1)
                            tomDato = LocalDate.now().plusDays(3)
                            type = Inntektskildetype.ARBEIDSINNTEKT
                        },
                        inntektForTest {
                            fomDato = LocalDate.now().plusDays(3)
                            tomDato = LocalDate.now().plusDays(5)
                            type = Inntektskildetype.ARBEIDSINNTEKT
                        }
                    ),
                    ""
                )

            val behandlingsresultat = Behandlingsresultat.forTest {
                behandling {
                    tema = Behandlingstema.PENSJONIST
                    status = Behandlingsstatus.OPPRETTET
                    type = Behandlingstyper.NY_VURDERING
                    fagsak {
                        type = Sakstyper.FTRL
                    }
                }
                valideringInput.medlemskapsperioder.forEach { mp ->
                    medlemskapsperiode {
                        fom = mp.fom!!
                        tom = mp.tom!!
                        innvilgelsesresultat = mp.innvilgelsesresultat!!
                        bestemmelse = mp.bestemmelse!!
                    }
                }
                årsavregning { }
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

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("valideringsDataPerioderDekkesScenarios")
        fun shouldBeValidPeriodeWhenInntektsperioderDekkerHelePerioden(valideringInput: ValideringInput) {
            val behandlingsresultat = Behandlingsresultat.forTest {
                behandling {
                    tema = Behandlingstema.ARBEID_KUN_NORGE
                    status = Behandlingsstatus.OPPRETTET
                    fagsak {
                        type = Sakstyper.FTRL
                    }
                }
                valideringInput.medlemskapsperioder.forEach { mp ->
                    medlemskapsperiode {
                        fom = mp.fom!!
                        tom = mp.tom!!
                        innvilgelsesresultat = mp.innvilgelsesresultat!!
                        bestemmelse = mp.bestemmelse!!
                    }
                }
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

        @Test
        fun shouldThrowExceptionWhenSkattepliktigAndPensjonUføreMedKildeskatt() {
            val behandlingsresultatMock = lagGyldigBehandlingsresultat()
            val medlemskapsperiode = behandlingsresultatMock.medlemskapsperioder.first()
            val skatteforholdsperioder = listOf(
                skatteforholdForTest {
                    fomDato = medlemskapsperiode.fom
                    tomDato = medlemskapsperiode.tom
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }
            )

            val inntektsperioder = listOf(
                inntektForTest {
                    fomDato = medlemskapsperiode.fom
                    tomDato = medlemskapsperiode.tom
                    type = Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
                },
                inntektForTest {
                    fomDato = medlemskapsperiode.fom
                    tomDato = medlemskapsperiode.tom
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }
            )

            shouldThrow<FunksjonellException> {
                TrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
                    behandlingsresultatMock,
                    skatteforholdsperioder,
                    inntektsperioder,
                    unleash
                )
            }.message shouldBe TrygdeavgiftsberegningValidator.SKATTEPLIKTIG_OG_PENSJON_UFORETRYGD_MED_KILDESKATT
        }

        @Test
        fun shouldNotThrowExceptionWhenSkattepliktigAndPensjonUføreWithoutKildeskatt() {
            val behandlingsresultatMock = lagGyldigBehandlingsresultat()
            val medlemskapsperiode = behandlingsresultatMock.medlemskapsperioder.first()
            val skatteforholdsperioder = listOf(
                skatteforholdForTest {
                    fomDato = medlemskapsperiode.fom
                    tomDato = medlemskapsperiode.tom
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }
            )

            val inntektsperioder = listOf(
                inntektForTest {
                    fomDato = medlemskapsperiode.fom
                    tomDato = medlemskapsperiode.tom
                    type = Inntektskildetype.PENSJON_UFØRETRYGD
                },
                inntektForTest {
                    fomDato = medlemskapsperiode.fom
                    tomDato = medlemskapsperiode.tom
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }
            )

            shouldNotThrow<FunksjonellException> {
                TrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
                    behandlingsresultatMock,
                    skatteforholdsperioder,
                    inntektsperioder,
                    unleash
                )
            }
        }

        @Test
        fun shouldNotThrowExceptionWhenNotSkattepliktigAndPensjonUføreMedKildeskatt() {
            val behandlingsresultatMock = lagGyldigBehandlingsresultat()
            val medlemskapsperiode = behandlingsresultatMock.medlemskapsperioder.first()
            val skatteforholdsperioder = listOf(
                skatteforholdForTest {
                    fomDato = medlemskapsperiode.fom
                    tomDato = medlemskapsperiode.tom
                    skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                }
            )

            val inntektsperioder = listOf(
                inntektForTest {
                    fomDato = medlemskapsperiode.fom
                    tomDato = medlemskapsperiode.tom
                    type = Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
                },
                inntektForTest {
                    fomDato = medlemskapsperiode.fom
                    tomDato = medlemskapsperiode.tom
                    type = Inntektskildetype.ARBEIDSINNTEKT
                }
            )

            shouldNotThrow<FunksjonellException> {
                TrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
                    behandlingsresultatMock,
                    skatteforholdsperioder,
                    inntektsperioder,
                    unleash
                )
            }
        }

        @Test
        fun shouldThrowExceptionWhenBehandlingInaktiv() {
            val behandlingsresultatMock = mockk<Behandlingsresultat>()
            val behandling = Behandling.forTest {
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    type = Sakstyper.FTRL
                }
            }
            every { behandlingsresultatMock.behandling } returns behandling
            every { behandlingsresultatMock.hentBehandling() } returns behandling
            val skatteforholdsperioder = emptyList<SkatteforholdTilNorge>()
            val inntektsperioder = emptyList<Inntektsperiode>()

            shouldThrow<FunksjonellException> {
                TrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
                    behandlingsresultatMock,
                    skatteforholdsperioder,
                    inntektsperioder,
                    unleash
                )
            }.message shouldBe BEHANDLING_IKKE_AKTIV
        }

        fun valideringsDataperiodermedFeilScenarios(): List<ValideringInput> = listOf(
            ValideringInput(                                                       // Inntektskilder dekker ikke hele perioden kaster exception
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now()
                        tom = LocalDate.now().plusDays(5)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(2)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    },
                    inntektForTest {
                        fomDato = LocalDate.now().plusDays(4)
                        tomDato = LocalDate.now().plusDays(5)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ),
                TrygdeavgiftsberegningValidator.INNTEKT_OG_SKATT_MÅ_DEKKE_MEDLEMSKAPSPERIODE_FOR_INNVÆRENDE_OG_FREMTIDIG
            ),
            ValideringInput(
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.of(2019, 1, 1)
                        tom = LocalDate.of(2029, 1, 1)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.of(2019, 1, 1)
                        tomDato = LocalDate.of(2024, 3, 31)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    skatteforholdForTest {
                        fomDato = LocalDate.of(2024, 4, 1)
                        tomDato = LocalDate.of(2029, 1, 1)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.of(2019, 1, 1)
                        tomDato = LocalDate.of(2026, 1, 30)
                        type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                        avgiftspliktigMndInntekt = Penger(10000.0)
                    },
                    inntektForTest {
                        fomDato = LocalDate.of(2026, 2, 1)
                        tomDato = LocalDate.of(2029, 1, 1)
                        type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                        avgiftspliktigMndInntekt = Penger(10000.0)
                    },
                    inntektForTest {
                        fomDato = LocalDate.of(2019, 1, 1)
                        tomDato = LocalDate.of(2019, 12, 31)
                        type = Inntektskildetype.NÆRINGSINNTEKT_FRA_NORGE
                        avgiftspliktigMndInntekt = Penger(3000.0)
                    },
                    inntektForTest {
                        fomDato = LocalDate.of(2021, 1, 1)
                        tomDato = LocalDate.of(2021, 1, 31)
                        type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(3000.0)
                    }
                ),
                TrygdeavgiftsberegningValidator.INNTEKT_OG_SKATT_IKKE_TIDLIGERE_ÅR,
                "Fire perioder med gap på 2026-01-31 (ikke dekket)"
            ),
            ValideringInput(                                                       // Skatteforhold dekker ikke hele perioden kaster exception
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now()
                        tom = LocalDate.now().plusDays(5)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(2)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    skatteforholdForTest {
                        fomDato = LocalDate.now().plusDays(4)
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(5)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ),
                TrygdeavgiftsberegningValidator.INNTEKT_OG_SKATT_MÅ_DEKKE_MEDLEMSKAPSPERIODE_FOR_INNVÆRENDE_OG_FREMTIDIG
            ),
            ValideringInput(                                                               // skatteforhold overlapper samme dag
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now()
                        tom = LocalDate.now().plusDays(10)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    skatteforholdForTest {
                        fomDato = LocalDate.now().plusDays(5)
                        tomDato = LocalDate.now().plusDays(10)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(10)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ),
                TrygdeavgiftsberegningValidator.SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE
            ),

            ValideringInput(                                                               // Skatteforholdsperiodene kan ikke overlappe
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now()
                        tom = LocalDate.now().plusDays(1)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(1)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    skatteforholdForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(1)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ),
                TrygdeavgiftsberegningValidator.SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE
            ),

            ValideringInput(                                                               // Inntektsperiode kan ikke være utenfor periode
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now()
                        tom = LocalDate.now().plusDays(1)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(1)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(3)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(15000.0)
                    }
                ),
                TrygdeavgiftsberegningValidator.INNTEKT_OG_SKATT_MÅ_DEKKE_MEDLEMSKAPSPERIODE_FOR_INNVÆRENDE_OG_FREMTIDIG
            ),

            ValideringInput(                                                               // Bestemmelser kan ikke være ulike for medlemskapsperioder
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now()
                        tom = LocalDate.now().plusDays(1)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    },
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now()
                        tom = LocalDate.now().plusDays(1)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(1)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(1)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(15000.0)
                    }
                ),
                TrygdeavgiftsberegningValidator.MEDLEMSKAPSPERIODER_HAR_FORSKJELLIGE_BESTEMMELSER
            ),


            ValideringInput(                                                               // Skatteforhold dekker ikke hele perioden
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now()
                        tom = LocalDate.now().plusDays(3)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now()
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    skatteforholdForTest {
                        fomDato = LocalDate.now().plusDays(1)
                        tomDato = LocalDate.now().plusDays(1)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        type = Inntektskildetype.ARBEIDSINNTEKT
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(3)
                    }
                ),
                TrygdeavgiftsberegningValidator.INNTEKT_OG_SKATT_MÅ_DEKKE_MEDLEMSKAPSPERIODE_FOR_INNVÆRENDE_OG_FREMTIDIG
            ),

            ValideringInput(                                                               // Inntektsperioder dekker ikke hele perioden
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now()
                        tom = LocalDate.now().plusDays(3)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.now().plusDays(0)
                        tomDato = LocalDate.now().plusDays(3)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now()
                        type = Inntektskildetype.ARBEIDSINNTEKT
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(15000.0)
                    }
                ),
                TrygdeavgiftsberegningValidator.INNTEKT_OG_SKATT_MÅ_DEKKE_MEDLEMSKAPSPERIODE_FOR_INNVÆRENDE_OG_FREMTIDIG
            ),
        )

        fun valideringsDataperiodermedFeilScenariosNyVurdering(): List<ValideringInput> = listOf(
            ValideringInput(                                                       // Inntektskilder dekker ikke hele perioden i inneværende og fremtidige år kaster exception
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now().minusYears(1)
                        tom = LocalDate.now().plusDays(5)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.now().withMonth(1).withDayOfMonth(1)
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.now().withMonth(1).withDayOfMonth(1)
                        tomDato = LocalDate.now().plusDays(2)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    },
                    inntektForTest {
                        fomDato = LocalDate.now().plusDays(4)
                        tomDato = LocalDate.now().plusDays(5)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ),
                TrygdeavgiftsberegningValidator.INNTEKT_OG_SKATT_MÅ_DEKKE_MEDLEMSKAPSPERIODE_FOR_INNVÆRENDE_OG_FREMTIDIG
            ),
            ValideringInput(                                                       // Skatteforhold dekker ikke hele perioden i inneværende og fremtidige år kaster exception
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now().minusYears(1)
                        tom = LocalDate.now().plusDays(5)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.now().withMonth(1).withDayOfMonth(1)
                        tomDato = LocalDate.now().plusDays(2)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    skatteforholdForTest {
                        fomDato = LocalDate.now().plusDays(4)
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.now().withMonth(1).withDayOfMonth(1)
                        tomDato = LocalDate.now().plusDays(5)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ),
                TrygdeavgiftsberegningValidator.INNTEKT_OG_SKATT_MÅ_DEKKE_MEDLEMSKAPSPERIODE_FOR_INNVÆRENDE_OG_FREMTIDIG
            ),
            ValideringInput(                                                               // skatteforhold overlapper samme dag
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now()
                        tom = LocalDate.now().plusDays(10)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    skatteforholdForTest {
                        fomDato = LocalDate.now().plusDays(5)
                        tomDato = LocalDate.now().plusDays(10)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(10)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ),
                TrygdeavgiftsberegningValidator.SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE
            ),

            ValideringInput(                                                               // Skatteforholdsperiodene kan ikke overlappe
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now()
                        tom = LocalDate.now().plusDays(1)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(1)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    skatteforholdForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(1)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ),
                TrygdeavgiftsberegningValidator.SKATTEFORHOLDSPERIODENE_KAN_IKKE_OVERLAPPE
            ),

            ValideringInput(                                                               // Bestemmelser kan ikke være ulike for medlemskapsperioder
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now()
                        tom = LocalDate.now().plusDays(1)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    },
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now()
                        tom = LocalDate.now().plusDays(1)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(1)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(1)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(15000.0)
                    }
                ),
                TrygdeavgiftsberegningValidator.MEDLEMSKAPSPERIODER_HAR_FORSKJELLIGE_BESTEMMELSER
            ),
            ValideringInput(                                                       // Skatteforhold kan ikke være i tidligere år
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now().minusYears(1)
                        tom = LocalDate.now().plusDays(5)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.now().minusYears(1)
                        tomDato = LocalDate.now().plusDays(2)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    skatteforholdForTest {
                        fomDato = LocalDate.now().plusDays(3)
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.now().withMonth(1).withDayOfMonth(1)
                        tomDato = LocalDate.now().plusDays(5)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ),
                TrygdeavgiftsberegningValidator.INNTEKT_OG_SKATT_IKKE_TIDLIGERE_ÅR
            ),
            ValideringInput(                                                       // Inntektsperioder kan ikke være i tidligere år
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now().minusYears(1)
                        tom = LocalDate.now().plusDays(5)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.now().minusYears(1)
                        tomDato = LocalDate.now().plusDays(5)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ),
                TrygdeavgiftsberegningValidator.INNTEKT_OG_SKATT_IKKE_TIDLIGERE_ÅR
            ),
        )

        private fun valideringsDataPerioderDekkesScenarios(): List<ValideringInput> = listOf(
            ValideringInput(                                                       // Inntektsperioder skal kunne overlappe
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now()
                        tom = LocalDate.now().plusDays(30)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(30)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(30)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    },
                    inntektForTest {
                        fomDato = LocalDate.now().plusDays(2)
                        tomDato = LocalDate.now().plusDays(5)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ),
                ""
            ),


            ValideringInput(                                                       // Inntektsperioder skal kunne overlappe
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.now()
                        tom = LocalDate.now().plusDays(5)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(5)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now().plusDays(3)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    },
                    inntektForTest {
                        fomDato = LocalDate.now().plusDays(2)
                        tomDato = LocalDate.now().plusDays(5)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ),
                ""
            ),

            ValideringInput(
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.of(inneværendeÅr, 3, 3)
                        tom = LocalDate.of(inneværendeÅr, 3, 5)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 3, 3)
                        tomDato = LocalDate.of(inneværendeÅr, 3, 3)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    skatteforholdForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 3, 4)
                        tomDato = LocalDate.of(inneværendeÅr, 3, 5)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 3, 3)
                        tomDato = LocalDate.of(inneværendeÅr, 3, 4)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(15000.0)
                    },
                    inntektForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 3, 5)
                        tomDato = LocalDate.of(inneværendeÅr, 3, 5)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(15000.0)
                    }
                ),
                ""
            ),

            ValideringInput(
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.of(inneværendeÅr, 1, 1)
                        tom = LocalDate.of(inneværendeÅr, 1, 8)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 1)
                        tomDato = LocalDate.of(inneværendeÅr, 1, 8)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 1)
                        tomDato = LocalDate.of(inneværendeÅr, 1, 4)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(15000.0)
                    },
                    inntektForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 5)
                        tomDato = LocalDate.of(inneværendeÅr, 1, 8)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(15000.0)
                    }
                ),
                ""
            ),

            ValideringInput(                                                               // SammePeriodeForAllePerioder
                listOf(
                    medlemskapsperiodeForTest {
                        fom = LocalDate.of(inneværendeÅr, 1, 1)
                        tom = LocalDate.of(inneværendeÅr, 1, 8)
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 1)
                        tomDato = LocalDate.of(inneværendeÅr, 1, 8)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 1)
                        tomDato = LocalDate.of(inneværendeÅr, 1, 8)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ),
                ""
            ),

            ValideringInput(                                                               // EnMedlemskapOgInntektPeriodeToSkatteforholdPerioder
                listOf(
                    medlemskapsperiodeForTest {
                        fom = LocalDate.of(inneværendeÅr, 1, 1)
                        tom = LocalDate.of(inneværendeÅr, 1, 8)
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 1)
                        tomDato = LocalDate.of(inneværendeÅr, 1, 4)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    skatteforholdForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 5)
                        tomDato = LocalDate.of(inneværendeÅr, 1, 8)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 1)
                        tomDato = LocalDate.of(inneværendeÅr, 1, 8)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ),
                ""
            ),

            ValideringInput(                                                               // EnMedlemskapOgSkatteforholdPeriodeToInntektsperioder
                listOf(
                    medlemskapsperiodeForTest {
                        fom = LocalDate.of(inneværendeÅr, 1, 1)
                        tom = LocalDate.of(inneværendeÅr, 1, 8)
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 1)
                        tomDato = LocalDate.of(inneværendeÅr, 1, 8)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 1)
                        tomDato = LocalDate.of(inneværendeÅr, 1, 4)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    },
                    inntektForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 5)
                        tomDato = LocalDate.of(inneværendeÅr, 1, 8)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ),
                ""
            ),

            ValideringInput(                                                               // ToMedlemskapOgToSkatteforholdPeriodeOgToInntektsperioder
                listOf(
                    medlemskapsperiodeForTest {
                        fom = LocalDate.of(inneværendeÅr, 1, 1)
                        tom = LocalDate.of(inneværendeÅr, 1, 4)
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    },
                    medlemskapsperiodeForTest {
                        fom = LocalDate.of(inneværendeÅr, 1, 5)
                        tom = LocalDate.of(inneværendeÅr, 1, 8)
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 1)
                        tomDato = LocalDate.of(inneværendeÅr, 1, 4)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    skatteforholdForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 5)
                        tomDato = LocalDate.of(inneværendeÅr, 1, 8)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 1)
                        tomDato = LocalDate.of(inneværendeÅr, 1, 4)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    },
                    inntektForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 5)
                        tomDato = LocalDate.of(inneværendeÅr, 1, 8)
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ),
                ""
            ),

            ValideringInput(
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.of(inneværendeÅr, 1, 1)
                        tom = LocalDate.of(inneværendeÅr + 10, 1, 1)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 1)
                        tomDato = LocalDate.of(inneværendeÅr + 5, 3, 31)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    skatteforholdForTest {
                        fomDato = LocalDate.of(inneværendeÅr + 5, 4, 1)
                        tomDato = LocalDate.of(inneværendeÅr + 10, 1, 1)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 1)
                        tomDato = LocalDate.of(inneværendeÅr + 10, 1, 1)
                        type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                        avgiftspliktigMndInntekt = Penger(10000.0)
                    },
                    inntektForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 1)
                        tomDato = LocalDate.of(inneværendeÅr, 12, 31)
                        type = Inntektskildetype.NÆRINGSINNTEKT_FRA_NORGE
                        avgiftspliktigMndInntekt = Penger(3000.0)
                    },
                    inntektForTest {
                        fomDato = LocalDate.of(inneværendeÅr + 2, 1, 1)
                        tomDato = LocalDate.of(inneværendeÅr + 2, 1, 31)
                        type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(3000.0)
                    }
                ),
                "",
                "En periode dekker alt, gap mellom andre perioder OK"
            ),

            ValideringInput(
                listOf(
                    medlemskapsperiodeForTest {
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        fom = LocalDate.of(inneværendeÅr, 1, 1)
                        tom = LocalDate.of(inneværendeÅr + 10, 1, 1)
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD
                    }
                ),
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 1)
                        tomDato = LocalDate.of(inneværendeÅr + 5, 3, 31)
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    },
                    skatteforholdForTest {
                        fomDato = LocalDate.of(inneværendeÅr + 5, 4, 1)
                        tomDato = LocalDate.of(inneværendeÅr + 10, 1, 1)
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 1)
                        tomDato = LocalDate.of(inneværendeÅr + 7, 2, 1)
                        type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                        avgiftspliktigMndInntekt = Penger(10000.0)
                    },
                    inntektForTest {
                        fomDato = LocalDate.of(inneværendeÅr + 7, 2, 1)
                        tomDato = LocalDate.of(inneværendeÅr + 10, 1, 1)
                        type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                        avgiftspliktigMndInntekt = Penger(10000.0)
                    },
                    inntektForTest {
                        fomDato = LocalDate.of(inneværendeÅr, 1, 1)
                        tomDato = LocalDate.of(inneværendeÅr, 12, 31)
                        type = Inntektskildetype.NÆRINGSINNTEKT_FRA_NORGE
                        avgiftspliktigMndInntekt = Penger(3000.0)
                    },
                    inntektForTest {
                        fomDato = LocalDate.of(inneværendeÅr + 2, 1, 1)
                        tomDato = LocalDate.of(inneværendeÅr + 2, 1, 31)
                        type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                        arbeidsgiversavgiftBetalesTilSkatt = true
                        avgiftspliktigMndInntekt = Penger(3000.0)
                    }
                ),
                "",
                "Fire perioder med overlapp på 2026-02-01 (begge inkluderer denne dagen)"
            )
        )

        fun valideringsData(): List<ValideringInput> = listOf(
            ValideringInput(
                listOf(
                    skatteforholdForTest {
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
                listOf(
                    inntektForTest {
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ),
                SKATTEFORHOLDSPERIODER_EMPTY
            ),
            ValideringInput(
                listOf(
                    skatteforholdForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now()
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    },
                    skatteforholdForTest {
                        fomDato = LocalDate.now()
                        tomDato = LocalDate.now()
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                ),
                listOf(
                    inntektForTest {
                        type = Inntektskildetype.ARBEIDSINNTEKT
                    }
                ),
                SKATTEPLIKTTYPE_LIK_FOR_ALLE_PERIODER
            )
        )

        private fun lagGyldigBehandlingsresultat() = Behandlingsresultat.forTest {
            behandling {
                status = Behandlingsstatus.OPPRETTET
                fagsak {
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                fom = LocalDate.now()
                tom = LocalDate.now().plusDays(5)
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            }
            årsavregning { }
        }


        private fun lagGyldigBehandlingsresultatForLovvalg() = Behandlingsresultat.forTest {
            behandling {
                status = Behandlingsstatus.OPPRETTET
                fagsak {
                    type = Sakstyper.EU_EOS
                    tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                }
            }
            årsavregning { }
        }
    }

    @Nested
    inner class ErAllePerioderSkattepliktige {
        @Test
        fun shouldReturnTrueWhenAllePerioderIsSkattepliktige() {
            val skatteforholdsperioder = listOf(
                skatteforholdForTest {
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                },
                skatteforholdForTest {
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }
            )
            TrygdeavgiftsberegningValidator.erAllePerioderSkattepliktige(skatteforholdsperioder) shouldBe true
        }

        @Test
        fun shouldReturnFalseWhenNotAllePerioderIsSkattepliktige() {
            val skatteforholdsperioder = listOf(
                skatteforholdForTest {
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                },
                skatteforholdForTest {
                    skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                }
            )
            TrygdeavgiftsberegningValidator.erAllePerioderSkattepliktige(skatteforholdsperioder) shouldBe false
        }
    }
}
