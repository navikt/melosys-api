package no.nav.melosys.service.avgift.satsendring

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
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

    @Test
    fun `AvgiftSatsendringInfo når det finnes både satsendring og en aktiv ny vurdering i en sak`() {
        val år = 2023
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandlingMedSatsendring = Behandling().apply {
            id = 1L
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.AVSLUTTET
            this.fagsak = fagsak
        }
        val behandlingNyVurdering = Behandling().apply {
            id = 2L
            type = Behandlingstyper.NY_VURDERING
            status = Behandlingsstatus.UNDER_BEHANDLING
            this.fagsak = fagsak
        }
        fagsak.behandlinger.addAll(listOf(behandlingMedSatsendring, behandlingNyVurdering))

        val opprinneligSats = 5.9
        val nySats = 6.3
        val behandlingsresultat = Behandlingsresultat().apply {
            id = 1L
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(lagTrygdeavgiftsperiode(opprinneligSats))
            })
        }
        val behandlingsresultatNyVurdering = Behandlingsresultat().apply {
            id = 2L
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(lagTrygdeavgiftsperiode(opprinneligSats))
            })
        }

        mockHentBehandling(listOf(behandlingMedSatsendring, behandlingNyVurdering))

        every { behandlingsresultatService.finnResultaterMedMedlemskapseriodeOverlappendeMed(år) } returns listOf(
            behandlingsresultat,
            behandlingsresultatNyVurdering
        )
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } returns true
        every { trygdeavgiftsberegningService.beregnTrygdeavgift(any(), any(), any()) } returns listOf(lagTrygdeavgiftsperiode(nySats))
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingMedSatsendring.id) } returns behandlingsresultat


        val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)


        satsendringInfo shouldBe SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = emptyList(),
            behandlingerMedSatsendringOgNyVurdering = listOf(
                SatsendringFinner.BehandlingForSatstendring(
                    behandlingID = 1L,
                    saksnummer = FagsakTestFactory.SAKSNUMMER,
                    behandlingstype = Behandlingstyper.FØRSTEGANG,
                    harSatsendring = true,
                    harAktivNyVurdering = true
                )
            ),
            behandlingerUtenSatsendring = emptyList(),
            behandlingerSomFeilet = emptyList()
        )
    }

    @Test
    fun `AvgiftSatsendringInfo behandlingUtenSatsendring når trygdeavgift for året som sjekkes er likt, men et annet år er forskjellig`() {
        val år = 2024
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandlingMedSatsendring = Behandling().apply {
            id = 1L
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.AVSLUTTET
            this.fagsak = fagsak
        }
        val behandlingNyVurdering = Behandling().apply {
            id = 2L
            type = Behandlingstyper.NY_VURDERING
            status = Behandlingsstatus.UNDER_BEHANDLING
            this.fagsak = fagsak
        }
        fagsak.behandlinger.addAll(listOf(behandlingMedSatsendring, behandlingNyVurdering))

        val opprinneligSats = 5.9
        val nySats = 6.3
        val behandlingsresultat = Behandlingsresultat().apply {
            id = 1L
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(lagTrygdeavgiftsperiode(opprinneligSats), lagTrygdeavgiftsperiode(opprinneligSats, 2024))
            })
        }
        val behandlingsresultatNyVurdering = Behandlingsresultat().apply {
            id = 2L
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(lagTrygdeavgiftsperiode(opprinneligSats))
            })
        }

        mockHentBehandling(listOf(behandlingMedSatsendring, behandlingNyVurdering))

        every { behandlingsresultatService.finnResultaterMedMedlemskapseriodeOverlappendeMed(år) } returns listOf(
            behandlingsresultat,
            behandlingsresultatNyVurdering
        )
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } returns true
        every { trygdeavgiftsberegningService.beregnTrygdeavgift(any(), any(), any()) } returns listOf(
            lagTrygdeavgiftsperiode(nySats),
            lagTrygdeavgiftsperiode(opprinneligSats, 2024)
        )
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingMedSatsendring.id) } returns behandlingsresultat


        val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)


        satsendringInfo shouldBe SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = emptyList(),
            behandlingerMedSatsendringOgNyVurdering = emptyList(),
            behandlingerUtenSatsendring = listOf(
                SatsendringFinner.BehandlingForSatstendring(
                    behandlingID = 1L,
                    saksnummer = FagsakTestFactory.SAKSNUMMER,
                    behandlingstype = Behandlingstyper.FØRSTEGANG,
                    harSatsendring = false,
                    harAktivNyVurdering = true
                )
            ),
            behandlingerSomFeilet = emptyList()
        )
    }

    @Test
    fun `AvgiftSatsendringInfo når det finnes 2 avsluttede behandlinger på samme sak - sist registrert blir valg`() {
        val år = 2023
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandlingMedSatsendring = Behandling().apply {
            id = 1L
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.AVSLUTTET
            registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            this.fagsak = fagsak
        }
        val behandlingNyVurdering = Behandling().apply {
            id = 2L
            type = Behandlingstyper.NY_VURDERING
            status = Behandlingsstatus.AVSLUTTET
            registrertDato = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
            this.fagsak = fagsak
        }
        fagsak.behandlinger.addAll(listOf(behandlingMedSatsendring, behandlingNyVurdering))

        val opprinneligSats = 5.9
        val nySats = 6.3
        val behandlingsresultat = Behandlingsresultat().apply {
            id = 1L
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(lagTrygdeavgiftsperiode(opprinneligSats))
            })
        }
        val behandlingsresultatNyVurdering = Behandlingsresultat().apply {
            id = 2L
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(lagTrygdeavgiftsperiode(opprinneligSats))
            })
        }

        mockHentBehandling(listOf(behandlingMedSatsendring, behandlingNyVurdering))

        every { behandlingsresultatService.finnResultaterMedMedlemskapseriodeOverlappendeMed(år) } returns listOf(
            behandlingsresultat,
            behandlingsresultatNyVurdering
        )
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } returns true
        every { trygdeavgiftsberegningService.beregnTrygdeavgift(any(), any(), any()) } returns listOf(lagTrygdeavgiftsperiode(nySats))
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingNyVurdering.id) } returns behandlingsresultatNyVurdering


        val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)


        satsendringInfo shouldBe SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = listOf(
                SatsendringFinner.BehandlingForSatstendring(
                    behandlingID = 2L,
                    saksnummer = FagsakTestFactory.SAKSNUMMER,
                    behandlingstype = Behandlingstyper.NY_VURDERING,
                    harSatsendring = true,
                    harAktivNyVurdering = false
                )
            ),
            behandlingerMedSatsendringOgNyVurdering = emptyList(),
            behandlingerUtenSatsendring = emptyList(),
            behandlingerSomFeilet = emptyList()
        )
    }

    @Test
    fun `AvgiftSatsendringInfo når det finnes 2 saker med en førstegang og en ny vurdering - 2 behandlinger kommer i resultat`() {
        val år = 2023
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandlingMedSatsendring = Behandling().apply {
            id = 1L
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.AVSLUTTET
            registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            this.fagsak = fagsak
        }
        val behandlingNyVurdering = Behandling().apply {
            id = 2L
            type = Behandlingstyper.NY_VURDERING
            status = Behandlingsstatus.AVSLUTTET
            registrertDato = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
            this.fagsak = fagsak
        }
        fagsak.behandlinger.addAll(listOf(behandlingMedSatsendring, behandlingNyVurdering))

        val fagsak2 = FagsakTestFactory.builder().saksnummer("test2").build()
        val behandlingMedSatsendring2 = Behandling().apply {
            id = 3L
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.AVSLUTTET
            registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            this.fagsak = fagsak2
        }
        val behandlingNyVurdering2 = Behandling().apply {
            id = 4L
            type = Behandlingstyper.NY_VURDERING
            status = Behandlingsstatus.UNDER_BEHANDLING
            registrertDato = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
            this.fagsak = fagsak2
        }
        fagsak2.behandlinger.addAll(listOf(behandlingMedSatsendring2, behandlingNyVurdering2))

        val opprinneligSats = 5.9
        val nySats = 6.3
        val behandlingsresultat = Behandlingsresultat().apply {
            id = 1L
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(lagTrygdeavgiftsperiode(opprinneligSats))
            })
        }
        val behandlingsresultatNyVurdering = Behandlingsresultat().apply {
            id = 2L
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(lagTrygdeavgiftsperiode(opprinneligSats))
            })
        }
        val behandlingsresultat2 = Behandlingsresultat().apply {
            id = 3L
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(lagTrygdeavgiftsperiode(opprinneligSats))
            })
        }
        val behandlingsresultatNyVurdering2 = Behandlingsresultat().apply {
            id = 4L
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(lagTrygdeavgiftsperiode(opprinneligSats))
            })
        }

        mockHentBehandling(listOf(behandlingMedSatsendring, behandlingNyVurdering, behandlingMedSatsendring2, behandlingNyVurdering2))

        every { behandlingsresultatService.finnResultaterMedMedlemskapseriodeOverlappendeMed(år) } returns listOf(
            behandlingsresultat,
            behandlingsresultatNyVurdering,
            behandlingsresultat2,
            behandlingsresultatNyVurdering2
        )
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } returns true
        every { trygdeavgiftsberegningService.beregnTrygdeavgift(any(), any(), any()) } returns listOf(lagTrygdeavgiftsperiode(nySats))
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingNyVurdering.id) } returns behandlingsresultatNyVurdering
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingMedSatsendring2.id) } returns behandlingsresultat2


        val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)


        satsendringInfo shouldBe SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = listOf(
                SatsendringFinner.BehandlingForSatstendring(
                    behandlingID = 2L,
                    saksnummer = FagsakTestFactory.SAKSNUMMER,
                    behandlingstype = Behandlingstyper.NY_VURDERING,
                    harSatsendring = true,
                    harAktivNyVurdering = false
                )
            ),
            behandlingerMedSatsendringOgNyVurdering = listOf(
                SatsendringFinner.BehandlingForSatstendring(
                    behandlingID = 3L,
                    saksnummer = fagsak2.saksnummer,
                    behandlingstype = Behandlingstyper.FØRSTEGANG,
                    harSatsendring = true,
                    harAktivNyVurdering = true
                )
            ),
            behandlingerUtenSatsendring = emptyList(),
            behandlingerSomFeilet = emptyList()
        )
    }

    @Test
    fun `AvgiftSatsendringInfo når det finnes 2 avsluttede behandlinger på samme sak og en åpen`() {
        val år = 2023
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandlingMedSatsendring = Behandling().apply {
            id = 1L
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.AVSLUTTET
            registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            this.fagsak = fagsak
        }
        val behandlingNyVurdering = Behandling().apply {
            id = 2L
            type = Behandlingstyper.NY_VURDERING
            status = Behandlingsstatus.AVSLUTTET
            registrertDato = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
            this.fagsak = fagsak
        }
        val behandlingNyVurderingÅpen = Behandling().apply {
            id = 3L
            type = Behandlingstyper.NY_VURDERING
            status = Behandlingsstatus.UNDER_BEHANDLING
            registrertDato = LocalDate.now().plusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC)
            this.fagsak = fagsak
        }
        fagsak.behandlinger.addAll(listOf(behandlingMedSatsendring, behandlingNyVurdering, behandlingNyVurderingÅpen))

        val opprinneligSats = 5.9
        val nySats = 6.3
        val behandlingsresultat = Behandlingsresultat().apply {
            id = 1L
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(lagTrygdeavgiftsperiode(opprinneligSats))
            })
        }
        val behandlingsresultatNyVurdering = Behandlingsresultat().apply {
            id = 2L
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(lagTrygdeavgiftsperiode(opprinneligSats))
            })
        }

        mockHentBehandling(listOf(behandlingMedSatsendring, behandlingNyVurdering))

        every { behandlingsresultatService.finnResultaterMedMedlemskapseriodeOverlappendeMed(år) } returns listOf(
            behandlingsresultat,
            behandlingsresultatNyVurdering
        )
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(any()) } returns true
        every { trygdeavgiftsberegningService.beregnTrygdeavgift(any(), any(), any()) } returns listOf(lagTrygdeavgiftsperiode(nySats))
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingNyVurdering.id) } returns behandlingsresultatNyVurdering


        val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)


        satsendringInfo shouldBe SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = emptyList(),
            behandlingerMedSatsendringOgNyVurdering = listOf(
                SatsendringFinner.BehandlingForSatstendring(
                    behandlingID = 2L,
                    saksnummer = FagsakTestFactory.SAKSNUMMER,
                    behandlingstype = Behandlingstyper.NY_VURDERING,
                    harSatsendring = true,
                    harAktivNyVurdering = true
                )
            ),
            behandlingerUtenSatsendring = emptyList(),
            behandlingerSomFeilet = emptyList()
        )
    }

    @Test
    fun `AvgiftSatsendringInfo kun åpen førstegangsbehandling - ingen resultat`() {
        val år = 2023
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandlingMedSatsendring = Behandling().apply {
            id = 1L
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.UNDER_BEHANDLING
            registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
            this.fagsak = fagsak
        }
        fagsak.behandlinger.addAll(listOf(behandlingMedSatsendring))

        val opprinneligSats = 5.9
        val behandlingsresultat = Behandlingsresultat().apply {
            id = 1L
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(lagTrygdeavgiftsperiode(opprinneligSats))
            })
        }

        mockHentBehandling(listOf(behandlingMedSatsendring))
        every { behandlingsresultatService.finnResultaterMedMedlemskapseriodeOverlappendeMed(år) } returns listOf(behandlingsresultat)
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true


        val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)


        satsendringInfo shouldBe SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = emptyList(),
            behandlingerMedSatsendringOgNyVurdering = emptyList(),
            behandlingerUtenSatsendring = emptyList(),
            behandlingerSomFeilet = emptyList()
        )
    }

    @Test
    fun `AvgiftSatsendringInfo når det feiler mot beregn trygdeavgift`() {
        val år = 2023
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandlingMedSatsendring = Behandling().apply {
            id = 1L
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.AVSLUTTET
            this.fagsak = fagsak
        }
        val behandlingNyVurdering = Behandling().apply {
            id = 2L
            type = Behandlingstyper.NY_VURDERING
            status = Behandlingsstatus.UNDER_BEHANDLING
            this.fagsak = fagsak
        }
        fagsak.behandlinger.addAll(listOf(behandlingMedSatsendring, behandlingNyVurdering))

        val opprinneligSats = 5.9
        val behandlingsresultat = Behandlingsresultat().apply {
            id = 1L
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                trygdeavgiftsperioder = setOf(lagTrygdeavgiftsperiode(opprinneligSats))
            })
        }

        mockHentBehandling(listOf(behandlingMedSatsendring))
        every { behandlingsresultatService.finnResultaterMedMedlemskapseriodeOverlappendeMed(år) } returns listOf(behandlingsresultat)
        every { trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat) } returns true
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingMedSatsendring.id) } returns behandlingsresultat
        every { trygdeavgiftsberegningService.beregnTrygdeavgift(behandlingsresultat, any(), any()) } throws RuntimeException("Feiler mot beregning")


        val satsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(år)


        satsendringInfo shouldBe SatsendringFinner.AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = emptyList(),
            behandlingerMedSatsendringOgNyVurdering = emptyList(),
            behandlingerUtenSatsendring = emptyList(),
            behandlingerSomFeilet = listOf(
                SatsendringFinner.BehandlingForSatstendring(
                    behandlingID = 1L,
                    saksnummer = FagsakTestFactory.SAKSNUMMER,
                    behandlingstype = Behandlingstyper.FØRSTEGANG,
                    harSatsendring = false,
                    harAktivNyVurdering = false,
                    feilAarsak = "Feiler mot beregning"
                )
            )
        )
    }

    fun mockHentBehandling(behandlinger: List<Behandling>) {
        every { behandlingService.hentBehandling(any()) } answers { call ->
            val id = call.invocation.args[0] as Long
            behandlinger.find { it.id == id }
                ?: throw RuntimeException("Unexpected id: $id")
        }
    }

    private fun lagTrygdeavgiftsperiode(sats: Double, år: Int = 2023): Trygdeavgiftsperiode = Trygdeavgiftsperiode(
        id = 1L,
        periodeFra = LocalDate.of(år, 1, 1),
        periodeTil = LocalDate.of(år, 12, 31),
        trygdeavgiftsbeløpMd = Penger(sats * 1000),
        trygdesats = BigDecimal.valueOf(sats)
    )
}
