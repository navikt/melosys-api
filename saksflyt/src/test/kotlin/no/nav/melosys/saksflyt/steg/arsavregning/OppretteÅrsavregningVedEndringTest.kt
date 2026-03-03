package no.nav.melosys.saksflyt.steg.arsavregning

import io.getunleash.FakeUnleash
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

private const val SAKSNUMMER = "123456789"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
        clearAllMocks()
    }

    @Test
    fun `oppretter årsavregninger for førstegangsbehandling tilbake i tid`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.YRKESAKTIV
                fagsak {
                    saksnummer = SAKSNUMMER
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                fom = LocalDate.now().minusYears(2)
                tom = LocalDate.now().plusYears(1)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                trygdedekning = Trygdedekninger.FULL_DEKNING
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling = behandlingsresultat.behandling
        }

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { årsavregningService.finnÅrsavregningerPåFagsak(any(), any(), any()) } returns emptyList()


        oppretteÅrsavregningVedEndring.utfør(prosessinstans)


        verify {
            prosessInstansService.opprettArsavregningsBehandlingProsessflyt(
                SAKSNUMMER,
                "2024",
                Behandlingsaarsaktyper.AUTOMATISK_OPPRETTELSE
            )
        }
        verify {
            prosessInstansService.opprettArsavregningsBehandlingProsessflyt(
                SAKSNUMMER,
                "2025",
                Behandlingsaarsaktyper.AUTOMATISK_OPPRETTELSE
            )
        }
        confirmVerified(prosessInstansService)
    }

    @Test
    fun `oppretter 1 årsavregning for innvilget periode førstegangsbehandling tilbake i tid`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.YRKESAKTIV
                fagsak {
                    saksnummer = SAKSNUMMER
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                fom = LocalDate.now().minusYears(2)
                tom = LocalDate.now().minusYears(1)
                innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
                trygdedekning = Trygdedekninger.FULL_DEKNING
            }
            medlemskapsperiode {
                fom = LocalDate.now().minusYears(1)
                tom = LocalDate.now().plusYears(1)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                trygdedekning = Trygdedekninger.FULL_DEKNING
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling = behandlingsresultat.behandling
        }

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { årsavregningService.finnÅrsavregningerPåFagsak(any(), any(), any()) } returns emptyList()


        oppretteÅrsavregningVedEndring.utfør(prosessinstans)


        verify {
            prosessInstansService.opprettArsavregningsBehandlingProsessflyt(
                SAKSNUMMER,
                "2025",
                Behandlingsaarsaktyper.AUTOMATISK_OPPRETTELSE
            )
        }
        confirmVerified(prosessInstansService)
    }

    @Test
    fun `oppretter ikke årsavregninger for førstegangsbehandling kun frem i tid`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.YRKESAKTIV
                fagsak {
                    saksnummer = SAKSNUMMER
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                fom = LocalDate.now()
                tom = LocalDate.now().plusYears(1)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                trygdedekning = Trygdedekninger.FULL_DEKNING
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
    @MethodSource("endringITidligereMedlemskapsperiodeScenarios")
    fun `ny vurdering medlemskapsperiode scenario`(scenario: PeriodeEndringScenario) {
        val opprinneligBehandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.YRKESAKTIV
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
                trygdedekning = Trygdedekninger.FULL_DEKNING
            }
        }

        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 2L
            behandling {
                id = 2L
                type = Behandlingstyper.NY_VURDERING
                tema = Behandlingstema.YRKESAKTIV
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
                    innvilgelsesresultat = periode.innvilgelsesresultat!!
                    medlemskapstype = periode.medlemskapstype!!
                    trygdedekning = periode.dekning!!
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

    @Test
    fun `ny vurdering - endrer tom dato - kun årsavregning for året med tom endring`() {
        val opprinneligBehandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.YRKESAKTIV
                fagsak {
                    saksnummer = SAKSNUMMER
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                fom = LocalDate.of(2024, 1, 1)
                tom = LocalDate.of(2025, 1, 1)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                trygdedekning = Trygdedekninger.FULL_DEKNING
            }
        }

        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 2L
            behandling {
                id = 2L
                type = Behandlingstyper.NY_VURDERING
                tema = Behandlingstema.YRKESAKTIV
                fagsak {
                    saksnummer = SAKSNUMMER
                    type = Sakstyper.FTRL
                }
                opprinneligBehandling = opprinneligBehandlingsresultat.behandling
            }
            medlemskapsperiode {
                fom = LocalDate.of(2024, 1, 1)
                tom = LocalDate.of(2025, 1, 15)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                trygdedekning = Trygdedekninger.FULL_DEKNING
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling = behandlingsresultat.behandling
        }

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns opprinneligBehandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(2L) } returns behandlingsresultat
        every { årsavregningService.finnÅrsavregningerPåFagsak(any(), any(), any()) } returns emptyList()

        oppretteÅrsavregningVedEndring.utfør(prosessinstans)

        verify {
            prosessInstansService.opprettArsavregningsBehandlingProsessflyt(SAKSNUMMER, "2025", Behandlingsaarsaktyper.AUTOMATISK_OPPRETTELSE)
        }

        confirmVerified(prosessInstansService)
    }


    @Disabled("Egen jira sak for eøs saker")
    @ParameterizedTest(name = "{0}")
    @MethodSource("endringITidligereLovvalgsperiodeScenarios")
    fun `ny vurdering lovvalgsperiode scenario`(scenario: PeriodeEndringScenario) {
        val opprinneligBehandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                fagsak {
                    saksnummer = SAKSNUMMER
                    type = Sakstyper.EU_EOS
                }
            }
            lovvalgsperiode {
                fom = LocalDate.now().minusYears(2)
                tom = LocalDate.now().plusYears(1)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                dekning = Trygdedekninger.FULL_DEKNING
            }
        }

        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 2L
            behandling {
                id = 2L
                type = Behandlingstyper.NY_VURDERING
                fagsak {
                    saksnummer = SAKSNUMMER
                    type = Sakstyper.EU_EOS
                }
                opprinneligBehandling = opprinneligBehandlingsresultat.behandling
            }
            scenario.ny.forEach { periode ->
                lovvalgsperiode {
                    fom = periode.fom
                    tom = periode.tom
                    innvilgelsesresultat = periode.innvilgelsesresultat!!
                    medlemskapstype = periode.medlemskapstype
                    dekning = periode.dekning
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

    @Disabled("Egen jira sak for eøs saker")
    @ParameterizedTest(name = "{0}")
    @MethodSource("endringITidligereHelseutgiftsperiodeScenarios")
    fun `ny vurdering helseutgiftdekkes scenario`(scenario: PeriodeEndringScenario) {
        val opprinneligBehandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.PENSJONIST
                fagsak {
                    saksnummer = SAKSNUMMER
                    tema = Sakstemaer.TRYGDEAVGIFT
                    type = Sakstyper.EU_EOS
                }
            }
            helseutgiftDekkesPeriode {
                fomDato = LocalDate.now().minusYears(2)
                tomDato = LocalDate.now().plusYears(1)
            }
        }

        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 2L
            behandling {
                id = 2L
                type = Behandlingstyper.NY_VURDERING
                tema = Behandlingstema.PENSJONIST
                fagsak {
                    saksnummer = SAKSNUMMER
                    tema = Sakstemaer.TRYGDEAVGIFT
                    type = Sakstyper.EU_EOS
                }
                opprinneligBehandling = opprinneligBehandlingsresultat.behandling
            }
            scenario.ny.forEach { periode ->
                helseutgiftDekkesPeriode {
                    fomDato = periode.fom
                    tomDato = periode.tom
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

    fun endringITidligereMedlemskapsperiodeScenarios() = listOf(
        PeriodeEndringScenario(
            ny = listOf(
                Periode(
                    fom = LocalDate.now().minusYears(2).justerMånederISammeÅr(2),
                    tom = LocalDate.now().plusYears(1),
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET,
                    medlemskapstype = Medlemskapstyper.FRIVILLIG,
                    dekning = Trygdedekninger.FULL_DEKNING
                )
            ),
            forventedeÅr = listOf("2024"),
            beskrivelse = "Endring i fom dato innenfor samme år tidligere - 1 årsavregninger"
        ),
        PeriodeEndringScenario(
            ny = listOf(
                Periode(
                    fom = LocalDate.now().minusYears(2),
                    tom = LocalDate.now().plusYears(1),
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET,
                    medlemskapstype = Medlemskapstyper.PLIKTIG,
                    dekning = Trygdedekninger.FULL_DEKNING
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
                    medlemskapstype = Medlemskapstyper.FRIVILLIG,
                    dekning = Trygdedekninger.FULL_DEKNING
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
                    medlemskapstype = Medlemskapstyper.FRIVILLIG,
                    dekning = Trygdedekninger.FULL_DEKNING
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
                    medlemskapstype = Medlemskapstyper.FRIVILLIG,
                    dekning = Trygdedekninger.FULL_DEKNING
                ),
                Periode(
                    fom = LocalDate.now().minusYears(1),
                    tom = LocalDate.now().plusYears(1),
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET,
                    medlemskapstype = Medlemskapstyper.FRIVILLIG,
                    dekning = Trygdedekninger.FULL_DEKNING
                )
            ),
            forventedeÅr = listOf("2024", "2025"),
            beskrivelse = "En avslått periode, en innvilget - 2 årsavregninger"
        ),
        PeriodeEndringScenario(
            ny = listOf(
                Periode(
                    fom = LocalDate.now().minusYears(2),
                    tom = LocalDate.now().plusYears(1),
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET,
                    medlemskapstype = Medlemskapstyper.FRIVILLIG,
                    dekning = Trygdedekninger.FULL_DEKNING
                )
            ),
            forventedeÅr = emptyList(),
            beskrivelse = "Ingen endring - ingen årsavregning"
        ),
        PeriodeEndringScenario(
            ny = listOf(
                Periode(
                    fom = LocalDate.now().minusYears(2),
                    tom = LocalDate.now().plusYears(1),
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET,
                    medlemskapstype = Medlemskapstyper.FRIVILLIG,
                    dekning = Trygdedekninger.FULL_DEKNING_FTRL
                )
            ),
            forventedeÅr = listOf("2024", "2025"),
            beskrivelse = "Endret dekning - 2 årsavregninger"
        )
    )

    fun endringITidligereLovvalgsperiodeScenarios() = listOf(
        PeriodeEndringScenario(
            ny = listOf(
                Periode(
                    fom = LocalDate.now().minusYears(2).justerMånederISammeÅr(2),
                    tom = LocalDate.now().plusYears(1),
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET,
                    medlemskapstype = Medlemskapstyper.FRIVILLIG,
                    dekning = Trygdedekninger.FULL_DEKNING
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
                    medlemskapstype = Medlemskapstyper.PLIKTIG,
                    dekning = Trygdedekninger.FULL_DEKNING
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
                    medlemskapstype = Medlemskapstyper.FRIVILLIG,
                    dekning = Trygdedekninger.FULL_DEKNING
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
                    medlemskapstype = Medlemskapstyper.FRIVILLIG,
                    dekning = Trygdedekninger.FULL_DEKNING
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
                    medlemskapstype = Medlemskapstyper.FRIVILLIG,
                    dekning = Trygdedekninger.FULL_DEKNING
                ),
                Periode(
                    fom = LocalDate.now().minusYears(1),
                    tom = LocalDate.now().plusYears(1),
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET,
                    medlemskapstype = Medlemskapstyper.FRIVILLIG,
                    dekning = Trygdedekninger.FULL_DEKNING
                )
            ),
            forventedeÅr = listOf("2024", "2025"),
            beskrivelse = "En avslått periode, en innvilget - 2 årsavregninger"
        ),
        PeriodeEndringScenario(
            ny = listOf(
                Periode(
                    fom = LocalDate.now().minusYears(2),
                    tom = LocalDate.now().plusYears(1),
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET,
                    medlemskapstype = Medlemskapstyper.FRIVILLIG,
                    dekning = Trygdedekninger.FULL_DEKNING
                )
            ),
            forventedeÅr = emptyList(),
            beskrivelse = "Ingen endring - ingen årsavregning"
        ),
        PeriodeEndringScenario(
            ny = listOf(
                Periode(
                    fom = LocalDate.now().minusYears(2),
                    tom = LocalDate.now().plusYears(1),
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET,
                    medlemskapstype = Medlemskapstyper.FRIVILLIG,
                    dekning = Trygdedekninger.FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER
                )
            ),
            forventedeÅr = listOf("2024", "2025"),
            beskrivelse = "Endret dekning - 2 årsavregninger"
        )
    )

    fun endringITidligereHelseutgiftsperiodeScenarios() = listOf(
        PeriodeEndringScenario(
            ny = listOf(
                Periode(
                    fom = LocalDate.now().minusYears(2).justerMånederISammeÅr(2),
                    tom = LocalDate.now().plusYears(1)
                )
            ),
            forventedeÅr = listOf("2024", "2025"),
            beskrivelse = "Endring i fom dato innenfor samme år tidligere - 2 årsavregninger"
        ),
        PeriodeEndringScenario(
            ny = listOf(
                Periode(
                    fom = LocalDate.now().minusYears(1),
                    tom = LocalDate.now().plusYears(1),
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
                )
            ),
            forventedeÅr = emptyList(),
            beskrivelse = "Ingen endring - ingen årsavregning"
        ),
    )
}

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
    val innvilgelsesresultat: InnvilgelsesResultat? = null,
    val medlemskapstype: Medlemskapstyper? = null,
    val dekning: Trygdedekninger? = null
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
