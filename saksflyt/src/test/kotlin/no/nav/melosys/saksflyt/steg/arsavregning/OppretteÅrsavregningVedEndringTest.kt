package no.nav.melosys.saksflyt.steg.arsavregning

import io.getunleash.FakeUnleash
import io.mockk.Called
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.util.stream.Stream

private const val SAKSNUMMER = "123456789"

@ExtendWith(MockKExtension::class)
class OppretteÅrsavregningVedEndringTest {

    @RelaxedMockK
    private lateinit var årsavregningService: ÅrsavregningService

    @RelaxedMockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    private lateinit var prosessInstansService: ProsessinstansService

    private lateinit var oppretteÅrsavregningVedEndring: OppretteÅrsavregningVedEndring


    @BeforeEach
    fun setUp() {
        val fakeUnleash = FakeUnleash().apply { enableAll() }
        oppretteÅrsavregningVedEndring = OppretteÅrsavregningVedEndring(
            årsavregningService,
            behandlingsresultatService,
            prosessInstansService,
            fakeUnleash
        )
    }

    @Test
    fun `oppretter årsavregninger for førstegangsbehandling tilbake i tid`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                fagsak {
                    saksnummer = SAKSNUMMER
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                fom = LocalDate.now().minusYears(2)
                tom = LocalDate.now().plusYears(1)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET

            }
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling = behandlingsresultat.behandling
        }

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { årsavregningService.finnÅrsavregningerPåFagsak(any(), any(), any()) } returns emptyList()


        oppretteÅrsavregningVedEndring.utfør(prosessinstans)


        verify { prosessInstansService.opprettArsavregningsBehandlingProsessflyt(SAKSNUMMER, "2024", Behandlingsaarsaktyper.AUTOMATISK_OPPRETTELSE) }
        verify { prosessInstansService.opprettArsavregningsBehandlingProsessflyt(SAKSNUMMER, "2025", Behandlingsaarsaktyper.AUTOMATISK_OPPRETTELSE) }
        confirmVerified(prosessInstansService)
    }

    @Test
    fun `oppretter 1 årsavregning for innvilget periode førstegangsbehandling tilbake i tid`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                fagsak {
                    saksnummer = SAKSNUMMER
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                fom = LocalDate.now().minusYears(2)
                tom = LocalDate.now().minusYears(1)
                innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
            }
            medlemskapsperiode {
                fom = LocalDate.now().minusYears(1)
                tom = LocalDate.now().plusYears(1)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling = behandlingsresultat.behandling
        }

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { årsavregningService.finnÅrsavregningerPåFagsak(any(), any(), any()) } returns emptyList()


        oppretteÅrsavregningVedEndring.utfør(prosessinstans)


        verify { prosessInstansService.opprettArsavregningsBehandlingProsessflyt(SAKSNUMMER, "2025", Behandlingsaarsaktyper.AUTOMATISK_OPPRETTELSE) }
        confirmVerified(prosessInstansService)
    }

    @Test
    fun `oppretter ikke årsavregninger for førstegangsbehandling kun frem i tid`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                fagsak {
                    saksnummer = SAKSNUMMER
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                fom = LocalDate.now()
                tom = LocalDate.now().plusYears(1)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET

            }
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling = behandlingsresultat.behandling
        }

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { årsavregningService.finnÅrsavregningerPåFagsak(any(), any(), any()) } returns emptyList()


        oppretteÅrsavregningVedEndring.utfør(prosessinstans)


        verify { prosessInstansService wasNot Called }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endringITidligerePeriodeScenarios")
    fun `ny vurdering scenario`(scenario: PeriodeEndringScenario) {
        val opprinneligBehandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                fagsak {
                    saksnummer = SAKSNUMMER
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                fom = LocalDate.now().minusYears(2)
                tom = LocalDate.now().plusYears(1)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
            }
        }

        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 2L
            behandling {
                id = 2L
                type = Behandlingstyper.NY_VURDERING
                fagsak {
                    saksnummer = SAKSNUMMER
                    type = Sakstyper.FTRL
                }
                opprinneligBehandling = opprinneligBehandlingsresultat.behandling
            }
            scenario.ny.forEach { periode ->
                medlemskapsperiode {
                    fom = periode.fom
                    tom = periode.tom
                    innvilgelsesresultat = periode.innvilgelsesresultat
                    medlemskapstype = periode.medlemskapstype
                }
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling = behandlingsresultat.behandling
        }

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns opprinneligBehandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(2L) } returns behandlingsresultat
        every { årsavregningService.finnÅrsavregningerPåFagsak(any(), any(), any()) } returns emptyList()

        oppretteÅrsavregningVedEndring.utfør(prosessinstans)

        scenario.forventedeÅr.forEach { år ->
            verify {
                prosessInstansService.opprettArsavregningsBehandlingProsessflyt(
                    SAKSNUMMER,
                    år,
                    Behandlingsaarsaktyper.AUTOMATISK_OPPRETTELSE
                )
            }
        }

        confirmVerified(prosessInstansService)
    }

    companion object {
        @JvmStatic
        fun endringITidligerePeriodeScenarios() = Stream.of(
            PeriodeEndringScenario(
                ny = listOf(
                    Periode(
                        fom = LocalDate.now().minusYears(2).justerMånederISammeÅr(2),
                        tom = LocalDate.now().plusYears(1),
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET,
                        medlemskapstype = Medlemskapstyper.FRIVILLIG
                    )
                ),
                forventedeÅr = listOf("2024", "2025"),
                beskrivelse = "Endring i fom dato innenfor samme år tidligere - 2 årsavregninger"
            ),
            PeriodeEndringScenario(
                ny = listOf(
                    Periode(
                        fom = LocalDate.now().minusYears(2),
                        tom = LocalDate.now().plusYears(1),
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET,
                        medlemskapstype = Medlemskapstyper.PLIKTIG
                    )
                ),
                forventedeÅr = listOf("2024", "2025"),
                beskrivelse = "Endring fra frivillig til pliktig medlemskap - 2 årsavregninger"
            ),
            PeriodeEndringScenario(
                ny = listOf(
                    Periode(
                        fom = LocalDate.now().minusYears(1),
                        tom = LocalDate.now().plusYears(1),
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET,
                        medlemskapstype = Medlemskapstyper.FRIVILLIG
                    )
                ),
                forventedeÅr = listOf("2024", "2025"),
                beskrivelse = "Endrer fom til kun et år tidligere - 2 årsavregning"
            ),
            PeriodeEndringScenario(
                ny = listOf(
                    Periode(
                        fom = LocalDate.now().minusYears(2),
                        tom = LocalDate.now().plusYears(1),
                        innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT,
                        medlemskapstype = Medlemskapstyper.FRIVILLIG
                    )
                ),
                forventedeÅr = listOf("2024", "2025"),
                beskrivelse = "Avslått periode - 2 årsavregninger"
            ),
            PeriodeEndringScenario(
                ny = listOf(
                    Periode(
                        fom = LocalDate.now().minusYears(2),
                        tom = LocalDate.now().minusYears(1),
                        innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT,
                        medlemskapstype = Medlemskapstyper.FRIVILLIG
                    ),
                    Periode(
                        fom = LocalDate.now().minusYears(1),
                        tom = LocalDate.now().plusYears(1),
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET,
                        medlemskapstype = Medlemskapstyper.FRIVILLIG
                    )
                ),
                forventedeÅr = listOf("2024", "2025"),
                beskrivelse = "En avslått periode, en innvilget - 2 årsavregninger"
            )
        )
    }
}

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
    val innvilgelsesresultat: InnvilgelsesResultat,
    val medlemskapstype: Medlemskapstyper
)

data class PeriodeEndringScenario(
    val ny: List<Periode>,
    val forventedeÅr: List<String>,
    val beskrivelse: String = ""
) {
    override fun toString() = beskrivelse
}

private fun LocalDate.justerMånederISammeÅr(måneder: Long): LocalDate {
    val justertDato = this.plusMonths(måneder)
    return if (justertDato.year != this.year) {
        justertDato.minusMonths(måneder)
    } else {
        justertDato
    }
}
