package no.nav.melosys.service.avgift.aarsavregning

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

internal class ÅrsavregningServiceFinnTest : ÅrsavregningServiceTestBase() {

    @Test
    fun `finnÅrsavregning for ny årsavregning uten info i Melosys`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
                fagsak { }
            }
            årsavregning {
                id = 112
                aar = 2023
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat


        val result = årsavregningService.finnÅrsavregningForBehandling(1)


        result.shouldNotBeNull().run {
            årsavregningID shouldBe 112
            år shouldBe 2023
            tidligereTrygdeavgiftsGrunnlag shouldBe Trygdeavgiftsgrunnlag(emptyList(), emptyList(), emptyList())
            sisteGjeldendeAvgiftspliktigPerioder.shouldBeEmpty()
            tidligereAvgift.shouldBeEmpty()
            nyttTrygdeavgiftsGrunnlag shouldBe null
            endeligAvgift.shouldBeEmpty()
            tidligereFakturertBeloep shouldBe null
            beregnetAvgiftBelop shouldBe null
            tilFaktureringBeloep shouldBe null
            harInnbetaltTrygdeavgift shouldBe null
            innbetaltTrygdeavgift shouldBe null
            manueltAvgiftBeloep shouldBe null
            tidligereInnbetaltTrygdeavgift shouldBe null
            tidligereÅrsavregningmanueltAvgiftBeloep shouldBe null
            harSkjoennsfastsattInntektsgrunnlag shouldBe false
        }
    }

    @Test
    fun `finnÅrsavregning for ny årsavregning, grunnlag finnes i Melosys`() {
        val tidligereBehandlingsresultat = lagTidligereBehandlingsresultat {
            id = 99L
            // Setup tidligere behandling for henting av avgiftsgrunnlag
            behandling {
                id = 99L
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = "123456"
                    type = Sakstyper.FTRL
                }
            }
            registrertDato = LocalDate.now().minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
            medlemskapsperiode("2022-01-01", "2022-08-31")
            medlemskapsperiode("2022-09-01", "2023-05-31")
            medlemskapsperiode("2023-07-01", "2023-08-31", InnvilgelsesResultat.AVSLAATT)
        }
        val fagsak = tidligereBehandlingsresultat.hentBehandling().fagsak

        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
                this.fagsak = fagsak
            }
            registrertDato = LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
            årsavregning {
                id = 112
                aar = 2023
                this.tidligereBehandlingsresultat = tidligereBehandlingsresultat
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(99L) } returns tidligereBehandlingsresultat
        every { fagsakService.hentFagsak("123456") } returns fagsak

        // Test expectations should match what lagTidligereBehandlingsresultat() creates
        val resultat = årsavregningService.finnÅrsavregningForBehandling(1)


        resultat.shouldNotBeNull().run {
            årsavregningID shouldBe 112
            år shouldBe 2023

            tidligereTrygdeavgiftsGrunnlag.shouldNotBeNull().run {
                avgiftspliktigperioder.shouldHaveSize(1)
                avgiftspliktigperioder.elementAt(0).fom shouldBe LocalDate.of(2023, 1, 1)
                avgiftspliktigperioder.elementAt(0).tom shouldBe LocalDate.of(2023, 5, 31)
            }

            tidligereAvgift.shouldNotBeEmpty()

            sisteGjeldendeAvgiftspliktigPerioder.shouldHaveSize(1)
                .single() shouldBe MedlemskapsperiodeForAvgift(
                fom = LocalDate.of(2023, 1, 1),
                tom = LocalDate.of(2023, 5, 31),
                dekning = Trygdedekninger.FULL_DEKNING_FTRL,
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8,
                medlemskapstyper = Medlemskapstyper.FRIVILLIG,
                InnvilgelsesResultat.INNVILGET
            )
            nyttTrygdeavgiftsGrunnlag shouldBe null
            endeligAvgift.shouldBeEmpty()
        }

    }

    @Test
    fun `finnÅrsavregning nr 2 av 3 årsavregninger på samme år - skal hente data fra nr 1 basert på vedtaksdato`() {
        // Årsavregning nr 1 - vedtatt først (10 dager siden)
        val behandlingsresultatÅrsavregning1 = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = "12345678"
                }
                registrertDato = LocalDate.now().minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            vedtakMetadata {
                vedtaksdato = LocalDate.now().minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
                vedtakstype = Vedtakstyper.ENDRINGSVEDTAK
            }
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            registrertDato = LocalDate.now().minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
            årsavregning {
                id = 101
                aar = 2023
                innbetaltTrygdeavgift = BigDecimal("5000.00")
                manueltAvgiftBeloep = BigDecimal("5500.00")
            }
        }


        val fagsak = behandlingsresultatÅrsavregning1.hentBehandling().fagsak


        // Årsavregning nr 2 - vedtatt 5 dager siden (denne henter vi)
        val behandlingsresultatÅrsavregning2 = Behandlingsresultat.forTest {
            id = 2L
            behandling {
                id = 2L
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
                this.fagsak = fagsak
                registrertDato = LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            vedtakMetadata {
                vedtaksdato = LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
                vedtakstype = Vedtakstyper.ENDRINGSVEDTAK
            }
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            registrertDato = LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
            årsavregning {
                id = 102
                aar = 2023
                tidligereBehandlingsresultat = null
                tidligereFakturertBeloep = BigDecimal("6000.00")
                beregnetAvgiftBelop = BigDecimal("6500.00")
                tilFaktureringBeloep = BigDecimal("500.00")
                harInnbetaltTrygdeavgift = true
                innbetaltTrygdeavgift = BigDecimal("6200.00")
                endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET
                manueltAvgiftBeloep = null
                harSkjoennsfastsattInntektsgrunnlag = false
            }
        }

        // Årsavregning nr 3 - vedtatt 2 dager siden (nyeste, skal ikke påvirke nr 2)
        val behandlingsresultatÅrsavregning3 = Behandlingsresultat.forTest {
            id = 3L
            behandling {
                id = 3L
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
                this.fagsak = fagsak
                registrertDato = LocalDate.now().minusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            vedtakMetadata {
                vedtaksdato = LocalDate.now().minusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC)
                vedtakstype = Vedtakstyper.ENDRINGSVEDTAK
            }
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            registrertDato = LocalDate.now().minusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC)
            årsavregning {
                id = 103
                aar = 2023
                innbetaltTrygdeavgift = BigDecimal("7000.00")
                manueltAvgiftBeloep = BigDecimal("7500.00")
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultatÅrsavregning1
        every { behandlingsresultatService.hentBehandlingsresultat(2L) } returns behandlingsresultatÅrsavregning2
        every { behandlingsresultatService.hentBehandlingsresultat(3L) } returns behandlingsresultatÅrsavregning3
        every { fagsakService.hentFagsak("12345678") } returns fagsak

        // Hent årsavregning nr 2
        val result = årsavregningService.finnÅrsavregningForBehandling(2L)


        result.shouldNotBeNull().run {
            årsavregningID shouldBe 102
            år shouldBe 2023

            // Verifiser at data kommer fra årsavregning nr 2
            tidligereFakturertBeloep shouldBe BigDecimal("6000.00")
            beregnetAvgiftBelop shouldBe BigDecimal("6500.00")
            tilFaktureringBeloep shouldBe BigDecimal("500.00")
            harInnbetaltTrygdeavgift shouldBe true
            innbetaltTrygdeavgift shouldBe BigDecimal("6200.00")
            endeligAvgiftValg shouldBe EndeligAvgiftValg.OPPLYSNINGER_ENDRET
            manueltAvgiftBeloep shouldBe null
            harSkjoennsfastsattInntektsgrunnlag shouldBe false

            // VIKTIGST: Verifiser at tidligeredata kommer fra årsavregning nr 1 (ikke nr 3!)
            tidligereInnbetaltTrygdeavgift shouldBe BigDecimal("5000.00")
            tidligereÅrsavregningmanueltAvgiftBeloep shouldBe BigDecimal("5500.00")

            // Verifiser at data IKKE kommer fra årsavregning nr 3
            tidligereInnbetaltTrygdeavgift shouldNotBe BigDecimal("7000.00")
            tidligereÅrsavregningmanueltAvgiftBeloep shouldNotBe BigDecimal("7500.00")
        }
    }

    @Test
    fun `finnÅrsavregning uten tidligere årsavregning - skal ikke hente data fra siste årsavregning`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
                fagsak {
                    saksnummer = "12345678"
                }
            }
            årsavregning {
                id = 112
                aar = 2023
                tidligereBehandlingsresultat = null
                tidligereFakturertBeloep = BigDecimal("1000.00")
                beregnetAvgiftBelop = BigDecimal("1500.00")
                tilFaktureringBeloep = BigDecimal("500.00")
                harInnbetaltTrygdeavgift = true
                innbetaltTrygdeavgift = BigDecimal("1200.00")
                endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET
                manueltAvgiftBeloep = null
                harSkjoennsfastsattInntektsgrunnlag = false
            }
        }

        val fagsak = behandlingsresultat.hentBehandling().fagsak

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { fagsakService.hentFagsak("12345678") } returns fagsak


        val result = årsavregningService.finnÅrsavregningForBehandling(1)


        result.shouldNotBeNull().run {
            årsavregningID shouldBe 112
            år shouldBe 2023
            tidligereTrygdeavgiftsGrunnlag shouldBe Trygdeavgiftsgrunnlag(emptyList(), emptyList(), emptyList())
            sisteGjeldendeAvgiftspliktigPerioder.shouldBeEmpty()
            tidligereAvgift.shouldBeEmpty()
            nyttTrygdeavgiftsGrunnlag shouldBe null
            endeligAvgift.shouldBeEmpty()
            tidligereFakturertBeloep shouldBe BigDecimal("1000.00")
            beregnetAvgiftBelop shouldBe BigDecimal("1500.00")
            tilFaktureringBeloep shouldBe BigDecimal("500.00")
            harInnbetaltTrygdeavgift shouldBe true
            innbetaltTrygdeavgift shouldBe BigDecimal("1200.00")
            endeligAvgiftValg shouldBe EndeligAvgiftValg.OPPLYSNINGER_ENDRET
            manueltAvgiftBeloep shouldBe null
            tidligereInnbetaltTrygdeavgift shouldBe null
            tidligereÅrsavregningmanueltAvgiftBeloep shouldBe null
            harSkjoennsfastsattInntektsgrunnlag shouldBe false
        }
    }

    @Test
    fun `finnÅrsavregning med tidligere årsavregning - skal hente data fra siste årsavregning`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
                registrertDato = LocalDate.now().minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
                fagsak {
                    saksnummer = "12345678"
                }
            }
            vedtakMetadata {
                vedtaksdato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
                vedtakstype = Vedtakstyper.ENDRINGSVEDTAK
            }
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            registrertDato = LocalDate.now().minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
            årsavregning {
                id = 112
                aar = 2023
                tidligereBehandlingsresultat = null
                tidligereFakturertBeloep = BigDecimal("1000.00")
                beregnetAvgiftBelop = BigDecimal("1500.00")
                tilFaktureringBeloep = BigDecimal("500.00")
                harInnbetaltTrygdeavgift = true
                innbetaltTrygdeavgift = BigDecimal("1200.00")
                endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET
                manueltAvgiftBeloep = null
                harSkjoennsfastsattInntektsgrunnlag = false
            }
        }
        val fagsak = behandlingsresultat.hentBehandling().fagsak

        // Lag tidligere årsavregning som skal finnes (vedtatt tidligere)
        val behandlingsresultatTidligereÅrsavregning = Behandlingsresultat.forTest {
            id = 50L
            behandling {
                id = 50L
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVSLUTTET
                this.fagsak = fagsak
                registrertDato = LocalDate.now().minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            vedtakMetadata {
                vedtaksdato = LocalDate.now().minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
                vedtakstype = Vedtakstyper.ENDRINGSVEDTAK
            }
            årsavregning {
                id = 50
                aar = 2023
                innbetaltTrygdeavgift = BigDecimal("8000.00")
                manueltAvgiftBeloep = BigDecimal("9000.00")
            }
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            registrertDato = LocalDate.now().minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(50L) } returns behandlingsresultatTidligereÅrsavregning
        every { fagsakService.hentFagsak("12345678") } returns fagsak

        val result = årsavregningService.finnÅrsavregningForBehandling(1)

        result.shouldNotBeNull().run {
            årsavregningID shouldBe 112
            år shouldBe 2023
            tidligereTrygdeavgiftsGrunnlag shouldBe Trygdeavgiftsgrunnlag(emptyList(), emptyList(), emptyList())
            sisteGjeldendeAvgiftspliktigPerioder.shouldBeEmpty()
            tidligereAvgift.shouldBeEmpty()
            nyttTrygdeavgiftsGrunnlag shouldBe null
            endeligAvgift.shouldBeEmpty()
            tidligereFakturertBeloep shouldBe BigDecimal("1000.00")
            beregnetAvgiftBelop shouldBe BigDecimal("1500.00")
            tilFaktureringBeloep shouldBe BigDecimal("500.00")
            harInnbetaltTrygdeavgift shouldBe true
            innbetaltTrygdeavgift shouldBe BigDecimal("1200.00")
            endeligAvgiftValg shouldBe EndeligAvgiftValg.OPPLYSNINGER_ENDRET
            manueltAvgiftBeloep shouldBe null
            tidligereInnbetaltTrygdeavgift shouldBe BigDecimal("8000.00")
            tidligereÅrsavregningmanueltAvgiftBeloep shouldBe BigDecimal("9000.00")
            harSkjoennsfastsattInntektsgrunnlag shouldBe false
        }
    }

    @Test
    fun `finnÅrsavregning filtrerer sisteGjeldendeAvgiftspliktigPerioder basert på vedtaksDato`() {
        // Tidligere behandlingsresultat 1 - vedtatt 2023-03-01 (FØR årsavregningens vedtaksDato)
        val tidligBehandlingsresultat = lagTidligereBehandlingsresultat {
            id = 10L
            behandling {
                id = 10L
                status = Behandlingsstatus.AVSLUTTET
                fagsak {
                    saksnummer = "123456"
                    type = Sakstyper.FTRL
                }
            }
            registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2023, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            medlemskapsperiode("2023-01-01", "2023-06-30")
        }
        val fagsak = tidligBehandlingsresultat.hentBehandling().fagsak

        // Tidligere behandlingsresultat 2 - vedtatt 2023-09-01 (ETTER årsavregningens vedtaksDato)
        val senBehandlingsresultat = lagTidligereBehandlingsresultat {
            id = 20L
            behandling {
                id = 20L
                status = Behandlingsstatus.AVSLUTTET
                this.fagsak = fagsak
            }
            registrertDato = LocalDate.of(2023, 8, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2023, 9, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            medlemskapsperiode("2023-01-01", "2023-12-31")
        }

        // Årsavregning med vedtaksDato 2023-07-01 - mellom de to behandlingsresultatene
        val årsavregningBehandlingsresultat = Behandlingsresultat.forTest {
            id = 30L
            behandling {
                id = 30L
                type = Behandlingstyper.ÅRSAVREGNING
                this.fagsak = fagsak
            }
            vedtakMetadata {
                vedtaksdato = LocalDate.of(2023, 7, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                vedtakstype = Vedtakstyper.ENDRINGSVEDTAK
            }
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            registrertDato = LocalDate.of(2023, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
            årsavregning {
                id = 200
                aar = 2023
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(10L) } returns tidligBehandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(20L) } returns senBehandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(30L) } returns årsavregningBehandlingsresultat
        every { fagsakService.hentFagsak("123456") } returns fagsak

        val result = årsavregningService.finnÅrsavregningForBehandling(30L)

        result.shouldNotBeNull().run {
            årsavregningID shouldBe 200
            år shouldBe 2023

            // Skal kun inneholde perioder fra tidligBehandlingsresultat (vedtaksdato 2023-03-01 < 2023-07-01)
            // senBehandlingsresultat (vedtaksdato 2023-09-01) skal filtreres bort
            sisteGjeldendeAvgiftspliktigPerioder.shouldHaveSize(1)
                .single() shouldBe MedlemskapsperiodeForAvgift(
                fom = LocalDate.of(2023, 1, 1),
                tom = LocalDate.of(2023, 6, 30),
                dekning = Trygdedekninger.FULL_DEKNING_FTRL,
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8,
                medlemskapstyper = Medlemskapstyper.FRIVILLIG,
                InnvilgelsesResultat.INNVILGET
            )
        }
    }

    @Test
    fun `returnerer null når ingen årsavregning finnes på behandling`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
                fagsak {}
            }
            // årsavregning is null by default
        }

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat

        val result = årsavregningService.finnÅrsavregningForBehandling(1)

        result.shouldBeNull()
    }
}
