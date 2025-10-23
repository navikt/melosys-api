package no.nav.melosys.service.avgift.aarsavregning

import io.kotest.assertions.throwables.shouldThrow
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
        val behandlingsresultat = Behandlingsresultat().apply {
            behandling = Behandling.forTest {
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
        val tidligereBehandlingsresultat = lagTidligereBehandlingsresultat()
        val fagsak = Fagsak.forTest()
        val behandlingsresultat = Behandlingsresultat().apply resultat@{
            behandling = Behandling.forTest {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
                this.fagsak = fagsak
            }
            medlemskapsperioder = mutableSetOf(
                lagMedlemskapsperiode("2023-01-01", "2023-05-31"),
                lagMedlemskapsperiode("2023-06-01", "2023-12-31")
            )
            årsavregning = Årsavregning.forTest {
                id = 112
                aar = 2023
                this.behandlingsresultat = this@resultat
                this.tidligereBehandlingsresultat = tidligereBehandlingsresultat
                this.tilFaktureringBeloep = BigDecimal.TEN
                this.tidligereFakturertBeloep = BigDecimal.ONE
                this.trygdeavgiftFraAvgiftssystemet = BigDecimal.ONE
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { behandlingsresultatService.lagreOgFlush(any()) } returns behandlingsresultat

        årsavregningService.oppdaterHarTrygdeavgiftFraAvgiftssystemet(1L, true)

        verify(exactly = 1) { behandlingsresultatService.lagreOgFlush(behandlingsresultat) }
        behandlingsresultat.årsavregning?.harTrygdeavgiftFraAvgiftssystemet shouldBe true
        behandlingsresultat.årsavregning?.tilFaktureringBeloep shouldBe null
        behandlingsresultat.årsavregning?.tidligereFakturertBeloep shouldBe BigDecimal.ONE
        behandlingsresultat.årsavregning?.trygdeavgiftFraAvgiftssystemet shouldBe null
    }

    @Test
    fun `tilbakestiller medlemskapsperioder når harTrygdeavgiftFraAvgiftssystemet settes til false`() {

        // Lag tidligereBehandlingsresultat med medlemskapsperioder for 2023
        val tidligereBehandlingsresultat = Behandlingsresultat().apply {
            id = 2L
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            vedtakMetadata = VedtakMetadata()
            vedtakMetadata!!.vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
            medlemskapsperioder = mutableSetOf(
                lagMedlemskapsperiode("2022-01-01", "2022-12-31"),  // Overlapper ikke med 2023
                lagMedlemskapsperiode("2023-01-01", "2023-05-31"),  // Overlapper med 2023
                lagMedlemskapsperiode("2023-06-01", "2024-05-31")   // Overlapper med 2023
            )
        }
        val fagsak = Fagsak.forTest()
        val behandlingsresultat = Behandlingsresultat().apply {
            behandling = Behandling.forTest {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
                this.fagsak = fagsak
            }
            medlemskapsperioder = mutableSetOf(
                lagMedlemskapsperiode("2023-03-01", "2023-07-31")
            )
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

        medlemskapsperioderCaptured.size shouldBe 2

        // Sjekk at riktige perioder ble replikert
        medlemskapsperioderCaptured[0].fom shouldBe LocalDate.of(2023, 1, 1)
        medlemskapsperioderCaptured[0].tom shouldBe LocalDate.of(2023, 5, 31)
        medlemskapsperioderCaptured[1].fom shouldBe LocalDate.of(2023, 6, 1)
        medlemskapsperioderCaptured[1].tom shouldBe LocalDate.of(2023, 12, 31)

        // Sjekk at feltene ble nullstilt
        behandlingsresultatCaptor.captured.årsavregning?.harTrygdeavgiftFraAvgiftssystemet shouldBe false
        behandlingsresultatCaptor.captured.årsavregning?.tilFaktureringBeloep shouldBe null
        behandlingsresultatCaptor.captured.årsavregning?.tidligereFakturertBeloep shouldBe BigDecimal(50)
        behandlingsresultatCaptor.captured.årsavregning?.trygdeavgiftFraAvgiftssystemet shouldBe null
    }

    @Test
    fun `beholder eksisterende medlemskapsperioder når harTrygdeavgiftFraAvgiftssystemet settes til true`() {
        val tidligereBehandlingsresultat = lagTidligereBehandlingsresultat()

        val eksisterendeMedlemskapsperioder = mutableSetOf(
            lagMedlemskapsperiode("2023-01-01", "2023-05-31"),
            lagMedlemskapsperiode("2023-06-01", "2023-08-31")
        )
        val fagsak = Fagsak.forTest()
        val behandlingsresultat = Behandlingsresultat().apply {
            val behandlingsresultatOutercontext = this
            behandling = Behandling.forTest {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
                this.fagsak = fagsak
            }
            medlemskapsperioder = eksisterendeMedlemskapsperioder
            årsavregning = Årsavregning.forTest {
                id = 112
                aar = 2023
                this.behandlingsresultat = behandlingsresultatOutercontext
                this.tidligereBehandlingsresultat = tidligereBehandlingsresultat
                this.harTrygdeavgiftFraAvgiftssystemet = false
                this.tilFaktureringBeloep = BigDecimal.valueOf(100)
                this.tidligereFakturertBeloep = BigDecimal.valueOf(50)
                this.trygdeavgiftFraAvgiftssystemet = BigDecimal.valueOf(50)
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { behandlingsresultatService.lagreOgFlush(any()) } returns behandlingsresultat


        årsavregningService.oppdaterHarTrygdeavgiftFraAvgiftssystemet(1L, true)


        verify(exactly = 1) { behandlingsresultatService.lagreOgFlush(behandlingsresultat) }

        behandlingsresultat.årsavregning?.harTrygdeavgiftFraAvgiftssystemet shouldBe true
        behandlingsresultat.årsavregning?.tilFaktureringBeloep shouldBe null
        behandlingsresultat.årsavregning?.tidligereFakturertBeloep shouldBe BigDecimal(50)
        behandlingsresultat.årsavregning?.trygdeavgiftFraAvgiftssystemet shouldBe null

        behandlingsresultat.medlemskapsperioder shouldBe eksisterendeMedlemskapsperioder
        behandlingsresultat.medlemskapsperioder.size shouldBe 2
    }

    @Test
    fun `returnerer oppdatert ÅrsavregningModel`() {
        val tidligereBehandlingsresultat = lagTidligereBehandlingsresultat()
        val fagsak = Fagsak.forTest()
        val behandlingsresultat = Behandlingsresultat().apply {
            val behandlingsresultatOutercontext = this
            behandling = Behandling.forTest {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
                this.fagsak = fagsak
            }
            medlemskapsperioder = mutableSetOf(
                lagMedlemskapsperiode("2023-01-01", "2023-05-31")
            )
            årsavregning = Årsavregning.forTest {
                id = 112
                aar = 2023
                this.behandlingsresultat = behandlingsresultatOutercontext
                this.tidligereBehandlingsresultat = tidligereBehandlingsresultat
                this.tilFaktureringBeloep = BigDecimal.valueOf(100)
                this.tidligereFakturertBeloep = BigDecimal.valueOf(50)
                this.trygdeavgiftFraAvgiftssystemet = BigDecimal.valueOf(50)
                this.harTrygdeavgiftFraAvgiftssystemet = false
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { behandlingsresultatService.lagreOgFlush(any()) } returns behandlingsresultat


        val resultat = årsavregningService.oppdaterHarTrygdeavgiftFraAvgiftssystemet(1L, true)


        resultat.årsavregningID shouldBe 112
        resultat.år shouldBe 2023
        resultat.harTrygdeavgiftFraAvgiftssystemet shouldBe true
        resultat.tilFaktureringBeloep shouldBe null
        resultat.tidligereFakturertBeloep shouldBe BigDecimal(50)
        resultat.trygdeavgiftFraAvgiftssystemet shouldBe null
    }
}
