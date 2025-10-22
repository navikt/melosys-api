package no.nav.melosys.service.avgift.aarsavregning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import no.nav.melosys.domain.*
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
import java.util.*

internal class ÅrsavregningServiceOpprettTest : ÅrsavregningServiceTestBase() {

    @Test
    fun `Ny årsavregning kaster feil når flere årsavregninger eksisterer for samme år på samme Fagsak`() {
        val årsavregningEntity1 = Årsavregning.forTest {
            aar = 2023
            behandlingsresultat = Behandlingsresultat()
        }
        val eksisterendeBehandling = Behandling.forTest { id = 1L }
        every { aarsavregningRepository.findById(1L) }.returns(Optional.of(årsavregningEntity1))
        every { aarsavregningRepository.finnAntallÅrsavregningerPåFagsakForÅr(1, 2023) }.returns(1)
        every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(Behandlingsresultat().apply {
            behandling = eksisterendeBehandling
        })

        shouldThrow<FunksjonellException> {
            årsavregningService.opprettÅrsavregning(1, 2023)
        }
    }

    @Test
    fun `opprettÅrsavregning - første gang uten tidligere årsavregning eller behandlinger`() {
        // Scenario 1: Ingen tidligere årsavregning
        val fagsak = Fagsak.forTest {
            saksnummer = "123456"
            behandling {
                id = 1L
                type = Behandlingstyper.NY_VURDERING
                status = Behandlingsstatus.AVSLUTTET
            }
            behandling {
                id = 2L
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.OPPRETTET
            }
        }

        // Første behandling med medlemskap og avgift (NY_VURDERING)
        val nyVurderingBehandlingsresultat = Behandlingsresultat().apply {
            id = 1L
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling = fagsak.behandlinger[0]
            medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-12-31"))
            registrertDato = LocalDate.now().minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC)
        }

        // Ny årsavregningsbehandling som skal opprettes
        val årsavregningBehandlingsresultat = Behandlingsresultat().apply {
            id = 2L
            behandling = fagsak.behandlinger[1]
            registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } answers {
            val id = firstArg<Long>()
            when (id) {
                1L -> nyVurderingBehandlingsresultat
                2L -> årsavregningBehandlingsresultat
                else -> null
            }!!
        }

        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { aarsavregningRepository.finnAntallÅrsavregningerPåFagsakForÅr(2, 2023) } returns 0
        every { behandlingsresultatService.lagreOgFlush(any()) } answers { firstArg() }
        every { behandlingsresultatService.lagre(any()) } answers {
            firstArg<Behandlingsresultat>().apply {
                årsavregning?.id = 50L
            }
        }

        val resultat = årsavregningService.opprettÅrsavregning(2, 2023)

        resultat shouldNotBe null
        resultat.årsavregningID shouldBe 50L
        resultat.år shouldBe 2023

        // Verifiser at tidligere grunnlag er hentet fra NY_VURDERING
        resultat.tidligereTrygdeavgiftsGrunnlag shouldNotBe null
        resultat.tidligereTrygdeavgiftsGrunnlag?.medlemskapsperioder?.size shouldBe 1
        resultat.tidligereTrygdeavgiftsGrunnlag?.medlemskapsperioder?.get(0)?.fom shouldBe LocalDate.of(2023, 1, 1)
        resultat.tidligereTrygdeavgiftsGrunnlag?.medlemskapsperioder?.get(0)?.tom shouldBe LocalDate.of(2023, 12, 31)

        // Verifiser at gjeldende medlemskapsperioder også er satt
        resultat.sisteGjeldendeMedlemskapsperioder.size shouldBe 1
        resultat.sisteGjeldendeMedlemskapsperioder[0].fom shouldBe LocalDate.of(2023, 1, 1)
        resultat.sisteGjeldendeMedlemskapsperioder[0].tom shouldBe LocalDate.of(2023, 12, 31)

        // Verifiser at tidligereAvgift er hentet
        resultat.tidligereAvgift.isNotEmpty() shouldBe true

        // Siden det ikke finnes tidligere årsavregning, skal tidligereFakturertBeloep være beregnet
        resultat.tidligereFakturertBeloep shouldNotBe null
    }

    @Test
    fun `Ny årsavregning med tidligere årsavregning og påfølgende ny vurdering - skal hente noe data fra tidligere årsavregning`() {
        val fagsak = Fagsak.forTest {
            behandling {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
                registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
                status = Behandlingsstatus.AVSLUTTET
            }
            behandling {
                id = 2L
                type = Behandlingstyper.NY_VURDERING
                registrertDato = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                status = Behandlingsstatus.AVSLUTTET

            }
            behandling {
                id = 3L
                type = Behandlingstyper.ÅRSAVREGNING
                registrertDato = LocalDate.now().plusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
                status = Behandlingsstatus.OPPRETTET
            }
        }
        val behandlingsresultatÅrsavregningEksisterende = Behandlingsresultat().apply behandlingsresultat@{
            id = 1L
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            behandling = fagsak.behandlinger[0]
            medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-05-31"))
            årsavregning = Årsavregning.forTest {
                id = 112
                aar = 2023
                trygdeavgiftFraAvgiftssystemet = BigDecimal("5000")
                behandlingsresultat = this@behandlingsresultat
            }
            registrertDato = LocalDate.now().minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata = VedtakMetadata().apply {
                vedtaksdato = LocalDate.now().minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }

        val behandlingsresultatNyVurdering = Behandlingsresultat().apply {
            id = 2L
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandling = fagsak.behandlinger[1]
            medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-05-31"))
            registrertDato = LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
            vedtakMetadata = VedtakMetadata().apply {
                vedtaksdato = LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
        }

        val behandlingsresultatÅrsavregningNy = Behandlingsresultat().apply {
            id = 3L
            behandling = fagsak.behandlinger[2]
            registrertDato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } answers {
            val id = firstArg<Long>()
            when (id) {
                1L -> behandlingsresultatÅrsavregningEksisterende
                2L -> behandlingsresultatNyVurdering
                3L -> behandlingsresultatÅrsavregningNy
                else -> null
            }!!
        }

        every { fagsakService.hentFagsak(any()) }.returns(fagsak)

        every { behandlingsresultatService.lagre(any()) } answers {
            firstArg<Behandlingsresultat>().apply {
                årsavregning!!.id = 50L
            }
        }

        årsavregningService.opprettÅrsavregning(3, 2023) shouldBe ÅrsavregningModel(
            årsavregningID = 50L,
            år = 2023,
            tidligereTrygdeavgiftsGrunnlag = Trygdeavgiftsgrunnlag(
                listOf(
                    MedlemskapsperiodeForAvgift(
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 5, 31),
                        dekning = Trygdedekninger.FULL_DEKNING_FTRL,
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8,
                        medlemskapstyper = Medlemskapstyper.FRIVILLIG,
                        InnvilgelsesResultat.INNVILGET
                    )
                ),
                listOf(SkatteforholdTilNorgeForAvgift(lagSkatteforholdTilNorge("2023-01-01", "2023-05-31"))),
                listOf(InntektsperioderForAvgift(lagInntektsperiode("2023-01-01", "2023-05-31")))
            ),
            sisteGjeldendeMedlemskapsperioder = listOf(
                MedlemskapsperiodeForAvgift(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 5, 31),
                    dekning = Trygdedekninger.FULL_DEKNING_FTRL,
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8,
                    medlemskapstyper = Medlemskapstyper.FRIVILLIG,
                    InnvilgelsesResultat.INNVILGET
                )
            ),
            tidligereAvgift = behandlingsresultatNyVurdering.trygdeavgiftsperioder.filter { it.overlapperMedÅr(2023) },
            nyttTrygdeavgiftsGrunnlag = null,
            endeligAvgift = emptyList(),
            tidligereFakturertBeloep = BigDecimal("25000.00"),
            beregnetAvgiftBelop = null,
            tilFaktureringBeloep = null,
            harTrygdeavgiftFraAvgiftssystemet = true,
            trygdeavgiftFraAvgiftssystemet = BigDecimal("5000"),
            endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET,
            manueltAvgiftBeloep = null,
            tidligereTrygdeavgiftFraAvgiftssystemet = BigDecimal("5000"),
            tidligereÅrsavregningmanueltAvgiftBeloep = null,
            harSkjoennsfastsattInntektsgrunnlag = false
        )
    }
}
