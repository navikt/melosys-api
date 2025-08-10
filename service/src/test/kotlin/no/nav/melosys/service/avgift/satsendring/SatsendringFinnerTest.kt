package no.nav.melosys.service.avgift.satsendring

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test

@ExtendWith(MockKExtension::class)
class SatsendringFinnerTest {
    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var trygdeavgiftService: TrygdeavgiftService

    @MockK
    private lateinit var trygdeavgiftsberegningService: TrygdeavgiftsberegningService

    private lateinit var satsendringFinner: SatsendringFinner

    @BeforeEach
    fun setUp() {
        satsendringFinner = SatsendringFinner(
            behandlingService,
            behandlingsresultatService,
            trygdeavgiftService,
            trygdeavgiftsberegningService
        )
    }

    @ParameterizedTest
    @EnumSource(Behandlingstyper::class, names = ["NY_VURDERING", "MANGLENDE_INNBETALING_TRYGDEAVGIFT"])
    fun `AvgiftSatsendringInfo når det finnes både satsendring og en aktiv ny vurdering i en sak`(behandlingstype: Behandlingstyper) {
        val år = 2023
        val fagsak = Fagsak.forTest {
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling {
                id = 2L
                type = behandlingstype
                status = Behandlingsstatus.UNDER_BEHANDLING
                registrertDato = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }
        val behandlingMedSatsendring = fagsak.behandlinger[0]
        val behandlingNyVurdering = fagsak.behandlinger[1]

        val behandlingsresultat = lagBehandlingsresultat(1, setOf(lagTrygdeavgiftsperiode(OPPRINNELIG_SATS)))
        val behandlingsresultatNyVurdering = lagBehandlingsresultat(2, setOf(lagTrygdeavgiftsperiode(OPPRINNELIG_SATS)))

        mockHentBehandling(listOf(behandlingMedSatsendring, behandlingNyVurdering))

        every { behandlingsresultatService.finnResultaterMedVedtakOgMedlemskapsperiodeOverlappendeMed(år) } returns listOf(
            behandlingsresultat,
            behandlingsresultatNyVurdering
        )
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } returns true
        every { trygdeavgiftsberegningService.beregnTrygdeavgift(any(), any(), any()) } returns listOf(lagTrygdeavgiftsperiode(NY_SATS))
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingMedSatsendring.id) } returns behandlingsresultat


        val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)


        satsendringInfo shouldBe SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = emptyList(),
            behandlingerMedSatsendringOgBerørtAktivBehandling = listOf(
                SatsendringFinner.BehandlingInfo(
                    behandlingID = 1L,
                    saksnummer = FagsakTestFactory.SAKSNUMMER,
                    behandlingstype = Behandlingstyper.FØRSTEGANG,
                    påvirketAvSatsendring = true,
                    harAnnenAktivBehandling = true
                )
            ),
            behandlingerUtenSatsendring = emptyList(),
            behandlingerSomFeilet = emptyList()
        )
    }

    @Test
    fun `AvgiftSatsendringInfo ingen resultat når fagsak er annulert`() {
        val år = 2023
        val fagsak = Fagsak.forTest {
            status = Saksstatuser.ANNULLERT
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
            }
            behandling {
                id = 2L
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.UNDER_BEHANDLING
            }
        }
        val behandlingMedSatsendring = fagsak.behandlinger[0]

        val behandlingsresultat = lagBehandlingsresultat(1, setOf(lagTrygdeavgiftsperiode(OPPRINNELIG_SATS)))
        val behandlingsresultatNyVurdering = lagBehandlingsresultat(2, setOf(lagTrygdeavgiftsperiode(OPPRINNELIG_SATS)))

        mockHentBehandling(fagsak)

        every { behandlingsresultatService.finnResultaterMedVedtakOgMedlemskapsperiodeOverlappendeMed(år) } returns listOf(
            behandlingsresultat,
            behandlingsresultatNyVurdering
        )
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } returns true
        every { trygdeavgiftsberegningService.beregnTrygdeavgift(any(), any(), any()) } returns listOf(lagTrygdeavgiftsperiode(NY_SATS))
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingMedSatsendring.id) } returns behandlingsresultat


        val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)


        satsendringInfo shouldBe SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = emptyList(),
            behandlingerMedSatsendringOgBerørtAktivBehandling = emptyList(),
            behandlingerUtenSatsendring = emptyList(),
            behandlingerSomFeilet = emptyList()
        )
    }

    @Test
    fun `AvgiftSatsendringInfo førstegang og ny vurdering er avsluttet, men ny vurdering har ikke fakturerbar trygdeavgift - ingen resultat`() {
        val år = 2023
        val fagsak = Fagsak.forTest {
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling {
                id = 2L
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.AVSLUTTET
                registrertDato = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }
        val behandlingMedSatsendring = fagsak.behandlinger[0]

        val behandlingsresultat = lagBehandlingsresultat(1, setOf(lagTrygdeavgiftsperiode(OPPRINNELIG_SATS)))
        val behandlingsresultatNyVurdering = lagBehandlingsresultat(2, setOf(lagTrygdeavgiftsperiode(OPPRINNELIG_SATS)))

        mockHentBehandling(fagsak)

        every { behandlingsresultatService.finnResultaterMedVedtakOgMedlemskapsperiodeOverlappendeMed(år) } returns listOf(
            behandlingsresultat,
            behandlingsresultatNyVurdering
        )
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } answers {
            firstArg<Behandlingsresultat>().id == 1L
        }
        every { trygdeavgiftsberegningService.beregnTrygdeavgift(any(), any(), any()) } returns listOf(lagTrygdeavgiftsperiode(NY_SATS))
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingMedSatsendring.id) } returns behandlingsresultat


        val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)


        satsendringInfo shouldBe SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = emptyList(),
            behandlingerMedSatsendringOgBerørtAktivBehandling = emptyList(),
            behandlingerUtenSatsendring = emptyList(),
            behandlingerSomFeilet = emptyList()
        )
    }

    @Test
    fun `AvgiftSatsendringInfo behandlingUtenSatsendring når trygdeavgift for året som sjekkes er likt, men et annet år er forskjellig`() {
        val år = 2024
        val fagsak = Fagsak.forTest {
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling {
                id = 2L
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.UNDER_BEHANDLING
                registrertDato = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }
        val behandlingMedSatsendring = fagsak.behandlinger[0]

        val behandlingsresultat = Behandlingsresultat().apply {
            id = 1L
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(lagTrygdeavgiftsperiode(OPPRINNELIG_SATS), lagTrygdeavgiftsperiode(OPPRINNELIG_SATS, 2024))
            })
        }
        val behandlingsresultatNyVurdering = lagBehandlingsresultat(2, setOf(lagTrygdeavgiftsperiode(OPPRINNELIG_SATS)))

        mockHentBehandling(fagsak)

        every { behandlingsresultatService.finnResultaterMedVedtakOgMedlemskapsperiodeOverlappendeMed(år) } returns listOf(
            behandlingsresultat,
            behandlingsresultatNyVurdering
        )
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } returns true
        every { trygdeavgiftsberegningService.beregnTrygdeavgift(any(), any(), any()) } returns listOf(
            lagTrygdeavgiftsperiode(NY_SATS),
            lagTrygdeavgiftsperiode(OPPRINNELIG_SATS, 2024)
        )
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingMedSatsendring.id) } returns behandlingsresultat


        val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)


        satsendringInfo shouldBe SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = emptyList(),
            behandlingerMedSatsendringOgBerørtAktivBehandling = emptyList(),
            behandlingerUtenSatsendring = listOf(
                SatsendringFinner.BehandlingInfo(
                    behandlingID = 1L,
                    saksnummer = FagsakTestFactory.SAKSNUMMER,
                    behandlingstype = Behandlingstyper.FØRSTEGANG,
                    påvirketAvSatsendring = false,
                    harAnnenAktivBehandling = true
                )
            ),
            behandlingerSomFeilet = emptyList()
        )
    }

    @Test
    fun `AvgiftSatsendringInfo når det finnes 2 avsluttede behandlinger på samme sak - sist registrert blir valg`() {
        val år = 2023
        val fagsak = Fagsak.forTest {
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling {
                id = 2L
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.AVSLUTTET
                registrertDato = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }
        val behandlingNyVurdering = fagsak.behandlinger[1]

        val behandlingsresultat = lagBehandlingsresultat(1, setOf(lagTrygdeavgiftsperiode(OPPRINNELIG_SATS)))
        val behandlingsresultatNyVurdering = lagBehandlingsresultat(2, setOf(lagTrygdeavgiftsperiode(OPPRINNELIG_SATS)))

        mockHentBehandling(fagsak)

        every { behandlingsresultatService.finnResultaterMedVedtakOgMedlemskapsperiodeOverlappendeMed(år) } returns listOf(
            behandlingsresultat,
            behandlingsresultatNyVurdering
        )
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } returns true
        every { trygdeavgiftsberegningService.beregnTrygdeavgift(any(), any(), any()) } returns listOf(lagTrygdeavgiftsperiode(NY_SATS))
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingNyVurdering.id) } returns behandlingsresultatNyVurdering


        val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)


        satsendringInfo shouldBe SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = listOf(
                SatsendringFinner.BehandlingInfo(
                    behandlingID = 2L,
                    saksnummer = FagsakTestFactory.SAKSNUMMER,
                    behandlingstype = Behandlingstyper.NY_VURDERING,
                    påvirketAvSatsendring = true,
                    harAnnenAktivBehandling = false
                )
            ),
            behandlingerMedSatsendringOgBerørtAktivBehandling = emptyList(),
            behandlingerUtenSatsendring = emptyList(),
            behandlingerSomFeilet = emptyList()
        )
    }

    @Test
    fun `AvgiftSatsendringInfo når det finnes 2 saker med en førstegang og en ny vurdering - 2 behandlinger kommer i resultat`() {
        val år = 2023
        val fagsak = Fagsak.forTest {
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling {
                id = 2L
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.AVSLUTTET
                registrertDato = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }
        val behandlingMedSatsendring = fagsak.behandlinger[0]
        val behandlingNyVurdering = fagsak.behandlinger[1]

        val fagsak2 = Fagsak.forTest {
            saksnummer = "test2"
            behandling {
                id = 3L
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling {
                id = 4L
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.UNDER_BEHANDLING
                registrertDato = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }
        val behandlingMedSatsendring2 = fagsak2.behandlinger[0]
        val behandlingNyVurdering2 = fagsak2.behandlinger[1]

        val behandlingsresultat = lagBehandlingsresultat(1, setOf(lagTrygdeavgiftsperiode(OPPRINNELIG_SATS)))
        val behandlingsresultatNyVurdering = lagBehandlingsresultat(2, setOf(lagTrygdeavgiftsperiode(OPPRINNELIG_SATS)))
        val behandlingsresultat2 = lagBehandlingsresultat(3, setOf(lagTrygdeavgiftsperiode(OPPRINNELIG_SATS)))
        val behandlingsresultatNyVurdering2 = lagBehandlingsresultat(4, setOf(lagTrygdeavgiftsperiode(OPPRINNELIG_SATS)))

        mockHentBehandling(listOf(behandlingMedSatsendring, behandlingNyVurdering, behandlingMedSatsendring2, behandlingNyVurdering2))

        every { behandlingsresultatService.finnResultaterMedVedtakOgMedlemskapsperiodeOverlappendeMed(år) } returns listOf(
            behandlingsresultat,
            behandlingsresultatNyVurdering,
            behandlingsresultat2,
            behandlingsresultatNyVurdering2
        )
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } returns true
        every { trygdeavgiftsberegningService.beregnTrygdeavgift(any(), any(), any()) } returns listOf(lagTrygdeavgiftsperiode(NY_SATS))
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingNyVurdering.id) } returns behandlingsresultatNyVurdering
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingMedSatsendring2.id) } returns behandlingsresultat2


        val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)


        satsendringInfo shouldBe SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = listOf(
                SatsendringFinner.BehandlingInfo(
                    behandlingID = 2L,
                    saksnummer = FagsakTestFactory.SAKSNUMMER,
                    behandlingstype = Behandlingstyper.NY_VURDERING,
                    påvirketAvSatsendring = true,
                    harAnnenAktivBehandling = false
                )
            ),
            behandlingerMedSatsendringOgBerørtAktivBehandling = listOf(
                SatsendringFinner.BehandlingInfo(
                    behandlingID = 3L,
                    saksnummer = fagsak2.saksnummer,
                    behandlingstype = Behandlingstyper.FØRSTEGANG,
                    påvirketAvSatsendring = true,
                    harAnnenAktivBehandling = true
                )
            ),
            behandlingerUtenSatsendring = emptyList(),
            behandlingerSomFeilet = emptyList()
        )
    }

    @Test
    fun `AvgiftSatsendringInfo når det finnes 2 avsluttede behandlinger på samme sak og en åpen`() {
        val år = 2023
        val fagsak = Fagsak.forTest {
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling {
                id = 2L
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.AVSLUTTET
                registrertDato = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            behandling {
                id = 3L
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.UNDER_BEHANDLING
                registrertDato = LocalDate.now().plusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }
        val behandlingNyVurdering = fagsak.behandlinger[1]

        val behandlingsresultat = lagBehandlingsresultat(1, setOf(lagTrygdeavgiftsperiode(OPPRINNELIG_SATS)))
        val behandlingsresultatNyVurdering = lagBehandlingsresultat(2, setOf(lagTrygdeavgiftsperiode(OPPRINNELIG_SATS)))

        mockHentBehandling(fagsak)

        every { behandlingsresultatService.finnResultaterMedVedtakOgMedlemskapsperiodeOverlappendeMed(år) } returns listOf(
            behandlingsresultat,
            behandlingsresultatNyVurdering
        )
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } returns true
        every { trygdeavgiftsberegningService.beregnTrygdeavgift(any(), any(), any()) } returns listOf(lagTrygdeavgiftsperiode(NY_SATS))
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingNyVurdering.id) } returns behandlingsresultatNyVurdering


        val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)


        satsendringInfo shouldBe SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = emptyList(),
            behandlingerMedSatsendringOgBerørtAktivBehandling = listOf(
                SatsendringFinner.BehandlingInfo(
                    behandlingID = 2L,
                    saksnummer = FagsakTestFactory.SAKSNUMMER,
                    behandlingstype = Behandlingstyper.NY_VURDERING,
                    påvirketAvSatsendring = true,
                    harAnnenAktivBehandling = true
                )
            ),
            behandlingerUtenSatsendring = emptyList(),
            behandlingerSomFeilet = emptyList()
        )
    }

    @Test
    fun `AvgiftSatsendringInfo kun åpen førstegangsbehandling - ingen resultat`() {
        val år = 2023
        val fagsak = Fagsak.forTest {
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.UNDER_BEHANDLING
                registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }

        val behandlingsresultat = lagBehandlingsresultat(1, setOf(lagTrygdeavgiftsperiode(OPPRINNELIG_SATS)))

        mockHentBehandling(fagsak)
        every { behandlingsresultatService.finnResultaterMedVedtakOgMedlemskapsperiodeOverlappendeMed(år) } returns listOf(behandlingsresultat)
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true


        val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)


        satsendringInfo shouldBe SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = emptyList(),
            behandlingerMedSatsendringOgBerørtAktivBehandling = emptyList(),
            behandlingerUtenSatsendring = emptyList(),
            behandlingerSomFeilet = emptyList()
        )
    }

    @Test
    fun `AvgiftSatsendringInfo førstegangsbehandling med 2 trygdeavgiftsperioder i ulik rekkefølge som er like, ingen satsendring`() {
        val år = 2023
        val fagsak = Fagsak.forTest {
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }

        val elements1 = lagTrygdeavgiftsperiode(OPPRINNELIG_SATS)
        val elements2 = lagTrygdeavgiftsperiode(NY_SATS, id = 2L)
        val behandlingsresultat = lagBehandlingsresultat(1, setOf(elements1, elements2))

        mockHentBehandling(fagsak)
        every { behandlingsresultatService.finnResultaterMedVedtakOgMedlemskapsperiodeOverlappendeMed(år) } returns listOf(behandlingsresultat)
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        val elements3 = lagTrygdeavgiftsperiode(OPPRINNELIG_SATS, id = null)
        val elements4 = lagTrygdeavgiftsperiode(NY_SATS, id = null)
        every { trygdeavgiftsberegningService.beregnTrygdeavgift(any(), any(), any()) } returns listOf(elements4, elements3)
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingsresultat.id) } returns behandlingsresultat


        val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)


        satsendringInfo shouldBe SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = emptyList(),
            behandlingerMedSatsendringOgBerørtAktivBehandling = emptyList(),
            behandlingerUtenSatsendring = listOf(
                SatsendringFinner.BehandlingInfo(
                    behandlingID = 1L,
                    saksnummer = FagsakTestFactory.SAKSNUMMER,
                    behandlingstype = Behandlingstyper.FØRSTEGANG,
                    påvirketAvSatsendring = false,
                    harAnnenAktivBehandling = false
                )
            ),
            behandlingerSomFeilet = emptyList()
        )
    }

    @Test
    fun `AvgiftSatsendringInfo når det feiler mot beregn trygdeavgift`() {
        val år = 2023
        val fagsak = Fagsak.forTest {
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
            }
            behandling {
                id = 2L
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.UNDER_BEHANDLING
            }
        }
        val behandlingMedSatsendring = fagsak.behandlinger[0]

        val behandlingsresultat = lagBehandlingsresultat(1, setOf(lagTrygdeavgiftsperiode(OPPRINNELIG_SATS)))

        mockHentBehandling(fagsak)
        every { behandlingsresultatService.finnResultaterMedVedtakOgMedlemskapsperiodeOverlappendeMed(år) } returns listOf(behandlingsresultat)
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingMedSatsendring.id) } returns behandlingsresultat
        every { trygdeavgiftsberegningService.beregnTrygdeavgift(behandlingsresultat, any(), any()) } throws RuntimeException("Feiler mot beregning")


        val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)


        satsendringInfo shouldBe SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = emptyList(),
            behandlingerMedSatsendringOgBerørtAktivBehandling = emptyList(),
            behandlingerUtenSatsendring = emptyList(),
            behandlingerSomFeilet = listOf(
                SatsendringFinner.BehandlingInfo(
                    behandlingID = 1L,
                    saksnummer = FagsakTestFactory.SAKSNUMMER,
                    behandlingstype = Behandlingstyper.FØRSTEGANG,
                    påvirketAvSatsendring = false,
                    harAnnenAktivBehandling = false,
                    feilÅrsak = "Feiler mot beregning"
                )
            )
        )
    }

    private fun lagBehandlingsresultat(id: Long, trygdeavgiftsperioder: Set<Trygdeavgiftsperiode>) =
        Behandlingsresultat().apply {
            this.id = id
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                this.trygdeavgiftsperioder = trygdeavgiftsperioder
            })
        }


    private fun mockHentBehandling(behandlinger: List<Behandling>) {
        every { behandlingService.hentBehandling(any()) } answers { call ->
            val id = call.invocation.args[0] as Long
            behandlinger.find { it.id == id }
                ?: throw RuntimeException("Unexpected id: $id")
        }
    }

    private fun mockHentBehandling(fagsak: Fagsak) {
        mockHentBehandling(fagsak.behandlinger)
    }

    private fun lagTrygdeavgiftsperiode(sats: Double, år: Int = 2023, id: Long? = 1L): Trygdeavgiftsperiode = Trygdeavgiftsperiode(
        id = id,
        periodeFra = LocalDate.of(år, 1, 1),
        periodeTil = LocalDate.of(år, 12, 31),
        trygdeavgiftsbeløpMd = Penger(sats * 1000),
        trygdesats = BigDecimal.valueOf(sats)
    )

    companion object {
        const val OPPRINNELIG_SATS = 5.9
        const val NY_SATS = 6.3
    }

}
