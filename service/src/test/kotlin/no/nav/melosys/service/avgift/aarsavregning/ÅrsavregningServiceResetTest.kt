package no.nav.melosys.service.avgift.aarsavregning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.forTest
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
        val behandlingsresultat = Behandlingsresultat().apply {
            behandling = Behandling.forTest {
                id = 1L
            }
            årsavregning = null
        }
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat

        shouldThrow<FunksjonellException> {
            årsavregningService.resetEksisterendeÅrsavregning(1L)
        }.message shouldBe "Ingen eksisterende årsavregning funnet på behandlingsresultat=1"
    }

    @Test
    fun `kaster feil når resultattype ikke er IKKE_FASTSATT`() {
        val fagsak = Fagsak.forTest { }
        val behandlingsresultat = Behandlingsresultat().apply resultat@{
            behandling = Behandling.forTest {
                id = 1L
                this.fagsak = fagsak
            }
            årsavregning = Årsavregning.forTest {
                id = 112
                aar = 2023
                this.behandlingsresultat = this@resultat
            }
        }
        behandlingsresultat.type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat

        shouldThrow<FunksjonellException> {
            årsavregningService.resetEksisterendeÅrsavregning(1L)
        }.message shouldBe "Kan ikke oppdatere årsavregning for behandlingsresultat=1 med type FASTSATT_TRYGDEAVGIFT"
    }

    @Test
    fun `når ny vurdering har blitt vedtatt før årsavregning, resettes åpne årsavregninger med info fra ny vurdering`() {
        val fagsak = Fagsak.forTest()

        val førstegangsbehandling = Behandling.forTest {
            id = 1L
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.AVSLUTTET
            this.fagsak = fagsak
        }
        val årsavregningsbehandling = Behandling.forTest {
            id = 2L
            type = Behandlingstyper.ÅRSAVREGNING
            status = Behandlingsstatus.UNDER_BEHANDLING
            this.fagsak = fagsak
        }
        val nyVurderingsbehandling = Behandling.forTest {
            id = 3L
            type = Behandlingstyper.NY_VURDERING
            status = Behandlingsstatus.AVSLUTTET
            this.fagsak = fagsak
        }
        val behandlingsresultatFørstegangsbehandling = lagTidligereBehandlingsresultat().apply {
            id = 1L
            behandling = førstegangsbehandling
            medlemskapsperioder = mutableSetOf(
                lagMedlemskapsperiode("2023-01-01", "2023-05-31").apply {
                    trygdeavgiftsperioder = setOf(lagTrygdeavgift("2023-01-01", "2023-05-01"))
                }
            )
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            registrertDato = LocalDate.now().minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
        }

        val behandlingsresultatÅrsavregning = lagTidligereBehandlingsresultat().apply resultat@{
            id = 2L
            behandling = årsavregningsbehandling
            type = Behandlingsresultattyper.IKKE_FASTSATT
            medlemskapsperioder = mutableSetOf(
                lagMedlemskapsperiode("2023-01-01", "2023-05-31").apply {
                    trygdeavgiftsperioder = setOf(lagTrygdeavgift("2023-01-01", "2023-05-01"))
                }
            )
            årsavregning = Årsavregning.forTest {
                id = 112
                aar = 2023
                this.behandlingsresultat = this@resultat
                this.tidligereBehandlingsresultat = behandlingsresultatFørstegangsbehandling
            }
            registrertDato = LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
        }

        val behandlingsresultatNyVurdering = lagTidligereBehandlingsresultat().apply {
            id = 3L
            behandling = nyVurderingsbehandling
            medlemskapsperioder = mutableSetOf(
                lagMedlemskapsperiode("2023-01-01", "2023-09-30").apply {
                    trygdeavgiftsperioder = setOf(lagTrygdeavgift("2023-01-01", "2023-09-30"))
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD
                }
            )
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            registrertDato = LocalDate.now().minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        }

        fagsak.behandlinger.clear()
        fagsak.behandlinger.addAll(
            listOf(
                førstegangsbehandling,
                årsavregningsbehandling,
                nyVurderingsbehandling
            )
        )
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

        val result = årsavregningService.resetEksisterendeÅrsavregning(2L)

        result shouldBe ÅrsavregningModel(
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
                    InntektsperioderForAvgift(lagInntektsperiode("2023-01-01", "2023-09-30"))
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
            tidligereAvgift = listOf(
                lagTrygdeavgift("2023-01-01", "2023-09-30")
            ),
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

        verify(exactly = 1) { behandlingsresultatService.lagreOgFlush(any()) }
        verify(exactly = 1) { behandlingsresultatService.lagre(any()) }
    }
}
