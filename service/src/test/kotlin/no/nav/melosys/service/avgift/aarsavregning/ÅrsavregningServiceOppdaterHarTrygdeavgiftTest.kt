package no.nav.melosys.service.avgift.aarsavregning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

internal class ÅrsavregningServiceOppdaterHarTrygdeavgiftTest : ÅrsavregningServiceTestBase() {

    @Test
    fun `kaster feil når årsavregning ikke finnes`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                id = 1L
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat

        shouldThrow<FunksjonellException> {
            årsavregningService.oppdaterHarTrygdeavgiftFraAvgiftssystemet(1L, true)
        }
    }

    @Test
    fun `setter harTrygdeavgiftFraAvgiftssystemet og nullstiller felt`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
                fagsak { }
            }
            årsavregning {
                id = 112
                aar = 2023
                tidligereBehandlingsresultat = Behandlingsresultat.forTest()
                tilFaktureringBeloep = BigDecimal.TEN
                tidligereFakturertBeloep = BigDecimal.ONE
                trygdeavgiftFraAvgiftssystemet = BigDecimal.ONE
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { behandlingsresultatService.lagreOgFlush(any()) } returns behandlingsresultat

        årsavregningService.oppdaterHarTrygdeavgiftFraAvgiftssystemet(1L, true)

        verify(exactly = 1) { behandlingsresultatService.lagreOgFlush(behandlingsresultat) }


        with(behandlingsresultat) {
            årsavregning.shouldNotBeNull().run {
                harTrygdeavgiftFraAvgiftssystemet shouldBe true
                tilFaktureringBeloep shouldBe null
                tidligereFakturertBeloep shouldBe BigDecimal.ONE
                trygdeavgiftFraAvgiftssystemet shouldBe null
            }

        }
    }

    @Test
    fun `tilbakestiller medlemskapsperioder når harTrygdeavgiftFraAvgiftssystemet settes til false`() {

        // Lag tidligereBehandlingsresultat med medlemskapsperioder for 2023
        val tidligereBehandlingsresultat = Behandlingsresultat.forTest {
            id = 2L
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
            }
            medlemskapsperiode("2022-01-01", "2022-12-31")  // Overlapper ikke med 2023
            medlemskapsperiode("2023-01-01", "2023-05-31")  // Overlapper med 2023
            medlemskapsperiode("2023-06-01", "2024-05-31")   // Overlapper med 2023
        }

        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
                fagsak { }
            }

            medlemskapsperiode("2023-03-01", "2023-07-31")
        }

        val årsavregning = Årsavregning.forTest {
            id = 112
            aar = 2023
            this.behandlingsresultat = behandlingsresultat
            this.tidligereBehandlingsresultat = tidligereBehandlingsresultat
            this.harTrygdeavgiftFraAvgiftssystemet = true
            this.tilFaktureringBeloep = BigDecimal.valueOf(100)
            this.tidligereFakturertBeloep = BigDecimal.valueOf(50)
            this.trygdeavgiftFraAvgiftssystemet = BigDecimal.valueOf(50)
        }

        behandlingsresultat.årsavregning = årsavregning

        val årsavregningService = ÅrsavregningService(
            aarsavregningRepository,
            behandlingsresultatService,
            fagsakService,
            trygdeavgiftService,
        )

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat

        val behandlingsresultatCaptor = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagreOgFlush(capture(behandlingsresultatCaptor)) } answers {
            behandlingsresultatCaptor.captured
        }


        årsavregningService.oppdaterHarTrygdeavgiftFraAvgiftssystemet(1L, false)


        verify(exactly = 1) { behandlingsresultatService.lagreOgFlush(any()) }

        val medlemskapsperioderCaptured = behandlingsresultatCaptor.captured.medlemskapsperioder
            .toList()
            .sortedBy { it.fom }
            .shouldBeInstanceOf<List<Medlemskapsperiode>>()

        medlemskapsperioderCaptured.shouldHaveSize(2).run {
            // Sjekk at riktige perioder ble replikert
            elementAt(0).fom shouldBe LocalDate.of(2023, 1, 1)
            elementAt(0).tom shouldBe LocalDate.of(2023, 5, 31)
            elementAt(1).fom shouldBe LocalDate.of(2023, 6, 1)
            elementAt(1).tom shouldBe LocalDate.of(2023, 12, 31)
        }

        behandlingsresultatCaptor.captured.årsavregning.shouldNotBeNull().run {
            // Sjekk at feltene ble nullstilt
            harTrygdeavgiftFraAvgiftssystemet shouldBe false
            tilFaktureringBeloep shouldBe null
            tidligereFakturertBeloep shouldBe BigDecimal(50)
            trygdeavgiftFraAvgiftssystemet shouldBe null
        }
    }

    @Test
    fun `beholder eksisterende medlemskapsperioder når harTrygdeavgiftFraAvgiftssystemet settes til true`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
                fagsak { }
            }
            årsavregning {
                id = 112
                aar = 2023
                tidligereBehandlingsresultat = lagTidligereBehandlingsresultat()
                harTrygdeavgiftFraAvgiftssystemet = false
                tilFaktureringBeloep = BigDecimal.valueOf(100)
                tidligereFakturertBeloep = BigDecimal.valueOf(50)
                trygdeavgiftFraAvgiftssystemet = BigDecimal.valueOf(50)
            }

            medlemskapsperiode("2023-01-01", "2023-05-31")
            medlemskapsperiode("2023-06-01", "2023-08-31")
        }

        val eksisterendeMedlemskapsperioder = behandlingsresultat.medlemskapsperioder

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { behandlingsresultatService.lagreOgFlush(any()) } returns behandlingsresultat


        årsavregningService.oppdaterHarTrygdeavgiftFraAvgiftssystemet(1L, true)


        verify(exactly = 1) { behandlingsresultatService.lagreOgFlush(behandlingsresultat) }

        with(behandlingsresultat) {
            årsavregning.shouldNotBeNull().run {
                harTrygdeavgiftFraAvgiftssystemet shouldBe true
                tilFaktureringBeloep shouldBe null
                tidligereFakturertBeloep shouldBe BigDecimal(50)
                trygdeavgiftFraAvgiftssystemet shouldBe null
            }
            medlemskapsperioder.shouldHaveSize(2) shouldBe eksisterendeMedlemskapsperioder
        }
    }

    @Test
    fun `returnerer oppdatert ÅrsavregningModel`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
                fagsak { }
            }
            årsavregning {
                id = 112
                aar = 2023
                this.tidligereBehandlingsresultat = lagTidligereBehandlingsresultat()
                this.tilFaktureringBeloep = BigDecimal.valueOf(100)
                this.tidligereFakturertBeloep = BigDecimal.valueOf(50)
                this.trygdeavgiftFraAvgiftssystemet = BigDecimal.valueOf(50)
                this.harTrygdeavgiftFraAvgiftssystemet = false
            }
            medlemskapsperiode("2023-01-01", "2023-05-31")
        }

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { behandlingsresultatService.lagreOgFlush(any()) } returns behandlingsresultat


        val resultat = årsavregningService.oppdaterHarTrygdeavgiftFraAvgiftssystemet(1L, true)


        with(resultat) {
            årsavregningID shouldBe 112
            år shouldBe 2023
            harTrygdeavgiftFraAvgiftssystemet shouldBe true
            tilFaktureringBeloep shouldBe null
            tidligereFakturertBeloep shouldBe BigDecimal(50)
            trygdeavgiftFraAvgiftssystemet shouldBe null
        }
    }
}
