package no.nav.melosys.service.avgift.aarsavregning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

internal class ÅrsavregningServiceResetTest : ÅrsavregningServiceTestBase() {

    @Test
    fun `kaster feil når ingen eksisterende årsavregning finnes på behandlingen`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                id = 1L
            }
            // årsavregning is null by default
        }
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat

        shouldThrow<FunksjonellException> {
            årsavregningService.resetEksisterendeÅrsavregning(1L)
        }.message shouldBe "Ingen eksisterende årsavregning funnet på behandlingsresultat=1"
    }

    @Test
    fun `kaster feil når resultattype ikke er IKKE_FASTSATT`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            behandling {
                id = 1L
                fagsak { }
            }
            årsavregning {
                id = 112
                aar = 2023
            }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat

        shouldThrow<FunksjonellException> {
            årsavregningService.resetEksisterendeÅrsavregning(1L)
        }.message shouldBe "Kan ikke oppdatere årsavregning for behandlingsresultat=1 med type FASTSATT_TRYGDEAVGIFT"
    }

    @Test
    fun `når ny vurdering har blitt vedtatt før årsavregning, resettes åpne årsavregninger med info fra ny vurdering - refaktorert`() {
        val fagsak = Fagsak.forTest { }

        val behandlingsresultatFørstegangsbehandling = lagTidligereBehandlingsresultat {
            id = 1L
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling {
                id = 1L
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                this.fagsak = fagsak
            }
            medlemskapsperiode("2023-01-01", "2023-05-31") {
                trygdeavgiftsperiode("2023-01-01", "2023-05-01")
            }
            vedtakMetadata {
                vedtaksdato = LocalDate.now().minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
            }
            registrertDato = LocalDate.now().minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
        }

        val behandlingsresultatÅrsavregning = Behandlingsresultat.forTest {
            id = 2L
            type = Behandlingsresultattyper.IKKE_FASTSATT
            behandling {
                id = 2L
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.UNDER_BEHANDLING
                this.fagsak = fagsak
            }
            medlemskapsperiode("2023-01-01", "2023-05-31") {
                trygdeavgiftsperiode("2023-01-01", "2023-05-01")
            }
            årsavregning {
                id = 112
                aar = 2023
                this.tidligereBehandlingsresultat = behandlingsresultatFørstegangsbehandling
            }
            vedtakMetadata {
                vedtaksdato = LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            registrertDato = LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
        }

        val behandlingsresultatNyVurdering = Behandlingsresultat.forTest {
            id = 3L
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling {
                id = 3L
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.AVSLUTTET
                this.fagsak = fagsak
            }
            medlemskapsperiode("2023-01-01", "2023-09-30") {
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD
                trygdeavgiftsperiode("2023-01-01", "2023-09-30")
            }
            vedtakMetadata {
                vedtaksdato = LocalDate.now().minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            registrertDato = LocalDate.now().minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultatFørstegangsbehandling
        every { behandlingsresultatService.hentBehandlingsresultat(2L) } returns behandlingsresultatÅrsavregning
        every { behandlingsresultatService.hentBehandlingsresultat(3L) } returns behandlingsresultatNyVurdering
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { behandlingsresultatService.lagreOgFlush(behandlingsresultatFørstegangsbehandling) } returns behandlingsresultatFørstegangsbehandling
        every { behandlingsresultatService.lagreOgFlush(behandlingsresultatÅrsavregning) } returns behandlingsresultatÅrsavregning
        every { behandlingsresultatService.lagre(any()) } answers {
            firstArg<Behandlingsresultat>().apply {
                årsavregning?.id = 113L
            }
        }

        val result: ÅrsavregningModel = årsavregningService.resetEksisterendeÅrsavregning(2L).shouldNotBeNull()

        val expected = ÅrsavregningModel(
            årsavregningID = 113L,
            år = 2023,
            tidligereTrygdeavgiftsGrunnlag = Trygdeavgiftsgrunnlag(
                listOf(
                    MedlemskapsperiodeForAvgift(
                        periodeFra = LocalDate.of(2023, 1, 1),
                        periodeTil = LocalDate.of(2023, 9, 30),
                        dekning = Trygdedekninger.FULL_DEKNING_FTRL,
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD,
                        medlemskapstyper = Medlemskapstyper.FRIVILLIG,
                        InnvilgelsesResultat.INNVILGET
                    )
                ),
                listOf(
                    SkatteforholdTilNorgeForAvgift(lagSkatteforholdTilNorge("2023-01-01", "2023-09-30"))
                ),
                listOf(
                    InntektsperioderForAvgift(Inntektsperiode().apply {
                        fomDato = LocalDate.parse("2023-01-01")
                        tomDato = LocalDate.parse("2023-09-30")
                        avgiftspliktigMndInntekt = Penger(5000.0)
                        type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                    })
                )
            ),
            sisteGjeldendeMedlemskapsperioder = listOf(
                MedlemskapsperiodeForAvgift(
                    periodeFra = LocalDate.of(2023, 1, 1),
                    periodeTil = LocalDate.of(2023, 9, 30),
                    dekning = Trygdedekninger.FULL_DEKNING_FTRL,
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD,
                    medlemskapstyper = Medlemskapstyper.FRIVILLIG,
                    InnvilgelsesResultat.INNVILGET
                )
            ),
            tidligereAvgift = result.tidligereAvgift, // Use actual value (avoids bidirectional relationship comparison)
            nyttTrygdeavgiftsGrunnlag = null,
            endeligAvgift = emptyList(),
            tidligereFakturertBeloep = BigDecimal.valueOf(4500000, 2),
            beregnetAvgiftBelop = null,
            tilFaktureringBeloep = null,
            harTrygdeavgiftFraAvgiftssystemet = null,
            trygdeavgiftFraAvgiftssystemet = null,
            endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET,
            manueltAvgiftBeloep = null,
            tidligereTrygdeavgiftFraAvgiftssystemet = null,
            tidligereÅrsavregningmanueltAvgiftBeloep = null,
            harSkjoennsfastsattInntektsgrunnlag = false
        )

        result shouldBe expected

        // Verify tidligereAvgift separately (since we used actual value above)
        result.tidligereAvgift.shouldHaveSize(1).single().run {
            periodeFra shouldBe LocalDate.of(2023, 1, 1)
            periodeTil shouldBe LocalDate.of(2023, 9, 30)
            trygdesats shouldBe BigDecimal(3.5)
        }

        verify(exactly = 1) { behandlingsresultatService.lagreOgFlush(any()) }
        verify(exactly = 1) { behandlingsresultatService.lagre(any()) }
    }
}
