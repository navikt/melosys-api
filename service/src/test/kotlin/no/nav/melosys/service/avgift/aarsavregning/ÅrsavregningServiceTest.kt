package no.nav.melosys.service.avgift.aarsavregning

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.AarsavregningRepository
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

@ExtendWith(MockKExtension::class)
internal class ÅrsavregningServiceTest {

    @RelaxedMockK
    private lateinit var aarsavregningRepository: AarsavregningRepository

    @RelaxedMockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    private lateinit var fagsakService: FagsakService

    @RelaxedMockK
    private lateinit var trygdeavgiftService: TrygdeavgiftService

    private lateinit var årsavregningService: ÅrsavregningService
    private val unleash = FakeUnleash().apply { enableAll() }

    @BeforeEach
    fun setup() {
        årsavregningService = ÅrsavregningService(
            aarsavregningRepository,
            behandlingsresultatService,
            fagsakService,
            trygdeavgiftService,
            unleash,
        )
        SpringSubjectHandler.set(TestSubjectHandler())
    }

    @Nested
    inner class OpprettÅrsavregning {
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
            resultat.tidligereTrygdeavgiftsGrunnlag?.fastsettingsperioder?.size shouldBe 1
            resultat.tidligereTrygdeavgiftsGrunnlag?.fastsettingsperioder?.get(0)?.fom shouldBe LocalDate.of(2023, 1, 1)
            resultat.tidligereTrygdeavgiftsGrunnlag?.fastsettingsperioder?.get(0)?.tom shouldBe LocalDate.of(2023, 12, 31)

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
                type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
                behandling = fagsak.behandlinger[0]
                medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-05-31"))
                årsavregning = Årsavregning.forTest {
                    id = 112
                    aar = 2023
                    trygdeavgiftFraAvgiftssystemet = BigDecimal("5000")
                    behandlingsresultat = this@behandlingsresultat
                }
                registrertDato = LocalDate.now().minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
            }

            val behandlingsresultatNyVurdering = Behandlingsresultat().apply {
                id = 2L
                type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
                behandling = fagsak.behandlinger[1]
                medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-05-31"))
                registrertDato = LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
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
                            periodeFra = LocalDate.of(2023, 1, 1),
                            periodeTil = LocalDate.of(2023, 5, 31),
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
                        periodeFra = LocalDate.of(2023, 1, 1),
                        periodeTil = LocalDate.of(2023, 5, 31),
                        dekning = Trygdedekninger.FULL_DEKNING_FTRL,
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8,
                        medlemskapstyper = Medlemskapstyper.FRIVILLIG,
                        InnvilgelsesResultat.INNVILGET
                    )
                ),
                tidligereAvgift = behandlingsresultatNyVurdering.trygdeavgiftsperioder?.filter { it.overlapperMedÅr(2023) }.orEmpty(),
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

    @Nested
    inner class FinnÅrsavregningForBehandling {
        @Test
        fun `finnÅrsavregning for ny årsavregning uten info i Melosys`() {
            val fagsak = Fagsak.forTest { }
            val behandlingsresultat = Behandlingsresultat().apply {
                behandling = Behandling.forTest {
                    id = 1L
                    type = Behandlingstyper.ÅRSAVREGNING
                    this.fagsak = fagsak
                }
            }
            val årsavregningEntity = Årsavregning.forTest {
                id = 112
                aar = 2023
                this.behandlingsresultat = behandlingsresultat
            }
            behandlingsresultat.årsavregning = årsavregningEntity
            every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat

            årsavregningService.finnÅrsavregningForBehandling(1) shouldBe ÅrsavregningModel(
                årsavregningID = 112,
                år = 2023,
                tidligereTrygdeavgiftsGrunnlag =  Trygdeavgiftsgrunnlag(emptyList(), emptyList(), emptyList()),
                sisteGjeldendeMedlemskapsperioder = emptyList(),
                tidligereAvgift = emptyList(),
                nyttTrygdeavgiftsGrunnlag = null,
                endeligAvgift = emptyList(),
                tidligereFakturertBeloep = null,
                beregnetAvgiftBelop = null,
                tilFaktureringBeloep = null,
                harTrygdeavgiftFraAvgiftssystemet = null,
                trygdeavgiftFraAvgiftssystemet = null,
                manueltAvgiftBeloep = null,
                tidligereTrygdeavgiftFraAvgiftssystemet = null,
                tidligereÅrsavregningmanueltAvgiftBeloep = null,
                harSkjoennsfastsattInntektsgrunnlag = false
            )
        }

        @Test
        fun `finnÅrsavregning for ny årsavregning, grunnlag finnes i Melosys`() {
            val fagsak = Fagsak.forTest {
                saksnummer = "123456"
            }
            val behandlingsresultat = Behandlingsresultat().apply {
                behandling = Behandling.forTest {
                    id = 1L
                    type = Behandlingstyper.ÅRSAVREGNING
                    this.fagsak = fagsak
                }
                registrertDato = LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            val tidligereBehandlingsresultat = lagTidligereBehandlingsresultat().apply {
                // Setup tidligere behandling for henting av avgiftsgrunnlag
                behandling = Behandling.forTest {
                    id = 99L
                    status = Behandlingsstatus.AVSLUTTET
                    this.fagsak = fagsak
                }
                registrertDato = LocalDate.now().minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
            }
            fagsak.behandlinger.add(tidligereBehandlingsresultat.hentBehandling())

            val årsavregningEntity = Årsavregning.forTest {
                id = 112
                aar = 2023
                this.behandlingsresultat = behandlingsresultat
                this.tidligereBehandlingsresultat = tidligereBehandlingsresultat
            }

            behandlingsresultat.årsavregning = årsavregningEntity
            every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
            every { behandlingsresultatService.hentBehandlingsresultat(99L) } returns tidligereBehandlingsresultat
            every { fagsakService.hentFagsak("123456") } returns fagsak

            // Test expectations should match what lagTidligereBehandlingsresultat() creates
            val resultat = årsavregningService.finnÅrsavregningForBehandling(1)

            resultat shouldNotBe null
            resultat!!.årsavregningID shouldBe 112
            resultat.år shouldBe 2023

            // Verify tidligereTrygdeavgiftsGrunnlag is populated
            resultat.tidligereTrygdeavgiftsGrunnlag shouldNotBe null
            resultat.tidligereTrygdeavgiftsGrunnlag?.fastsettingsperioder?.size shouldBe 1
            resultat.tidligereTrygdeavgiftsGrunnlag?.fastsettingsperioder?.get(0)?.fom shouldBe LocalDate.of(2023, 1, 1)
            resultat.tidligereTrygdeavgiftsGrunnlag?.fastsettingsperioder?.get(0)?.tom shouldBe LocalDate.of(2023, 5, 31)

            // Verify tidligere avgift is populated
            resultat.tidligereAvgift.isNotEmpty() shouldBe true

            resultat.sisteGjeldendeMedlemskapsperioder shouldBe listOf(
                MedlemskapsperiodeForAvgift(
                    periodeFra = LocalDate.of(2023, 1, 1),
                    periodeTil = LocalDate.of(2023, 5, 31),
                    dekning = Trygdedekninger.FULL_DEKNING_FTRL,
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8,
                    medlemskapstyper = Medlemskapstyper.FRIVILLIG,
                    InnvilgelsesResultat.INNVILGET
                )
            )
            resultat.nyttTrygdeavgiftsGrunnlag shouldBe null
            resultat.endeligAvgift shouldBe emptyList()
        }

        @Test
        fun `finnÅrsavregning nr 2 av 3 årsavregninger på samme år - skal hente data fra nr 1 basert på vedtaksdato`() {
            val fagsak = Fagsak.forTest {
                saksnummer = "12345678"
            }

            // Årsavregning nr 1 - vedtatt først (10 dager siden)
            val behandlingsresultatÅrsavregning1 = Behandlingsresultat().apply {
                id = 1L
                behandling = Behandling.forTest {
                    id = 1L
                    type = Behandlingstyper.ÅRSAVREGNING
                    status = Behandlingsstatus.AVSLUTTET
                    this.fagsak = fagsak
                    registrertDato = LocalDate.now().minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
                }
                vedtakMetadata = VedtakMetadata().apply {
                    vedtaksdato = LocalDate.now().minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
                    vedtakstype = Vedtakstyper.ENDRINGSVEDTAK
                }
                årsavregning = Årsavregning.forTest {
                    id = 101
                    aar = 2023
                    trygdeavgiftFraAvgiftssystemet = BigDecimal("5000.00")
                    manueltAvgiftBeloep = BigDecimal("5500.00")
                }
            }

            // Årsavregning nr 2 - vedtatt 5 dager siden (denne henter vi)
            val behandlingsresultatÅrsavregning2 = Behandlingsresultat().apply resultat@{
                id = 2L
                behandling = Behandling.forTest {
                    id = 2L
                    type = Behandlingstyper.ÅRSAVREGNING
                    status = Behandlingsstatus.AVSLUTTET
                    this.fagsak = fagsak
                    registrertDato = LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
                }
                vedtakMetadata = VedtakMetadata().apply {
                    vedtaksdato = LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
                    vedtakstype = Vedtakstyper.ENDRINGSVEDTAK
                }
                årsavregning = Årsavregning.forTest {
                    id = 102
                    aar = 2023
                    this.behandlingsresultat = this@resultat
                    tidligereBehandlingsresultat = null
                    tidligereFakturertBeloep = BigDecimal("6000.00")
                    beregnetAvgiftBelop = BigDecimal("6500.00")
                    tilFaktureringBeloep = BigDecimal("500.00")
                    harTrygdeavgiftFraAvgiftssystemet = true
                    trygdeavgiftFraAvgiftssystemet = BigDecimal("6200.00")
                    endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET
                    manueltAvgiftBeloep = null
                    harSkjoennsfastsattInntektsgrunnlag = false
                }
            }

            // Årsavregning nr 3 - vedtatt 2 dager siden (nyeste, skal ikke påvirke nr 2)
            val behandlingsresultatÅrsavregning3 = Behandlingsresultat().apply {
                id = 3L
                behandling = Behandling.forTest {
                    id = 3L
                    type = Behandlingstyper.ÅRSAVREGNING
                    status = Behandlingsstatus.AVSLUTTET
                    this.fagsak = fagsak
                    registrertDato = LocalDate.now().minusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC)
                }
                vedtakMetadata = VedtakMetadata().apply {
                    vedtaksdato = LocalDate.now().minusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC)
                    vedtakstype = Vedtakstyper.ENDRINGSVEDTAK
                }
                årsavregning = Årsavregning.forTest {
                    id = 103
                    aar = 2023
                    trygdeavgiftFraAvgiftssystemet = BigDecimal("7000.00")
                    manueltAvgiftBeloep = BigDecimal("7500.00")
                }
            }

            // Legg til alle behandlinger på fagsaken
            fagsak.leggTilBehandling(behandlingsresultatÅrsavregning1.hentBehandling())
            fagsak.leggTilBehandling(behandlingsresultatÅrsavregning2.hentBehandling())
            fagsak.leggTilBehandling(behandlingsresultatÅrsavregning3.hentBehandling())

            every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultatÅrsavregning1
            every { behandlingsresultatService.hentBehandlingsresultat(2L) } returns behandlingsresultatÅrsavregning2
            every { behandlingsresultatService.hentBehandlingsresultat(3L) } returns behandlingsresultatÅrsavregning3
            every { fagsakService.hentFagsak("12345678") } returns fagsak

            // Hent årsavregning nr 2
            val result = årsavregningService.finnÅrsavregningForBehandling(2L)

            result.shouldNotBeNull()
            result.årsavregningID shouldBe 102
            result.år shouldBe 2023

            // Verifiser at data kommer fra årsavregning nr 2
            result.tidligereFakturertBeloep shouldBe BigDecimal("6000.00")
            result.beregnetAvgiftBelop shouldBe BigDecimal("6500.00")
            result.tilFaktureringBeloep shouldBe BigDecimal("500.00")
            result.harTrygdeavgiftFraAvgiftssystemet shouldBe true
            result.trygdeavgiftFraAvgiftssystemet shouldBe BigDecimal("6200.00")
            result.endeligAvgiftValg shouldBe EndeligAvgiftValg.OPPLYSNINGER_ENDRET
            result.manueltAvgiftBeloep shouldBe null
            result.harSkjoennsfastsattInntektsgrunnlag shouldBe false

            // VIKTIGST: Verifiser at tidligeredata kommer fra årsavregning nr 1 (ikke nr 3!)
            result.tidligereTrygdeavgiftFraAvgiftssystemet shouldBe BigDecimal("5000.00")
            result.tidligereÅrsavregningmanueltAvgiftBeloep shouldBe BigDecimal("5500.00")

            // Verifiser at data IKKE kommer fra årsavregning nr 3
            result.tidligereTrygdeavgiftFraAvgiftssystemet shouldNotBe BigDecimal("7000.00")
            result.tidligereÅrsavregningmanueltAvgiftBeloep shouldNotBe BigDecimal("7500.00")
        }

        @Test
        fun `finnÅrsavregning uten tidligere årsavregning - skal ikke hente data fra siste årsavregning`() {
            val fagsak = Fagsak.forTest {
                saksnummer = "12345678"
            }
            val behandlingsresultat = Behandlingsresultat().apply {
                behandling = Behandling.forTest {
                    id = 1L
                    type = Behandlingstyper.ÅRSAVREGNING
                    this.fagsak = fagsak
                }
            }

            val årsavregningEntity = Årsavregning.forTest {
                id = 112
                aar = 2023
                this.behandlingsresultat = behandlingsresultat
                tidligereBehandlingsresultat = null
                tidligereFakturertBeloep = BigDecimal("1000.00")
                beregnetAvgiftBelop = BigDecimal("1500.00")
                tilFaktureringBeloep = BigDecimal("500.00")
                harTrygdeavgiftFraAvgiftssystemet = true
                trygdeavgiftFraAvgiftssystemet = BigDecimal("1200.00")
                endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET
                manueltAvgiftBeloep = null
                harSkjoennsfastsattInntektsgrunnlag = false
            }

            behandlingsresultat.årsavregning = årsavregningEntity


            fagsak.leggTilBehandling(behandlingsresultat.hentBehandling())

            every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
            every { fagsakService.hentFagsak("12345678") } returns fagsak

            val result = årsavregningService.finnÅrsavregningForBehandling(1)

            result shouldBe ÅrsavregningModel(
                årsavregningID = 112,
                år = 2023,
                tidligereTrygdeavgiftsGrunnlag =  Trygdeavgiftsgrunnlag(emptyList(), emptyList(), emptyList()),
                sisteGjeldendeMedlemskapsperioder = emptyList(),
                tidligereAvgift = emptyList(),
                nyttTrygdeavgiftsGrunnlag = null,
                endeligAvgift = emptyList(),
                tidligereFakturertBeloep = BigDecimal("1000.00"),
                beregnetAvgiftBelop = BigDecimal("1500.00"),
                tilFaktureringBeloep = BigDecimal("500.00"),
                harTrygdeavgiftFraAvgiftssystemet = true,
                trygdeavgiftFraAvgiftssystemet = BigDecimal("1200.00"),
                endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET,
                manueltAvgiftBeloep = null,
                tidligereTrygdeavgiftFraAvgiftssystemet = null,
                tidligereÅrsavregningmanueltAvgiftBeloep = null,
                harSkjoennsfastsattInntektsgrunnlag = false
            )
        }

        @Test
        fun `finnÅrsavregning med tidligere årsavregning - skal hente data fra siste årsavregning`() {
            val fagsak = Fagsak.forTest {
                saksnummer = "12345678"
            }
            val behandlingsresultat = Behandlingsresultat().apply {
                behandling = Behandling.forTest {
                    id = 1L
                    type = Behandlingstyper.ÅRSAVREGNING
                    this.fagsak = fagsak
                }
                vedtakMetadata = VedtakMetadata().apply {
                    vedtaksdato = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
                    vedtakstype = Vedtakstyper.ENDRINGSVEDTAK
                }
            }

            val årsavregningEntity = Årsavregning.forTest {
                id = 112
                aar = 2023
                this.behandlingsresultat = behandlingsresultat
                tidligereBehandlingsresultat = null
                tidligereFakturertBeloep = BigDecimal("1000.00")
                beregnetAvgiftBelop = BigDecimal("1500.00")
                tilFaktureringBeloep = BigDecimal("500.00")
                harTrygdeavgiftFraAvgiftssystemet = true
                trygdeavgiftFraAvgiftssystemet = BigDecimal("1200.00")
                endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET
                manueltAvgiftBeloep = null
                harSkjoennsfastsattInntektsgrunnlag = false
            }

            behandlingsresultat.årsavregning = årsavregningEntity

            // Lag tidligere årsavregning som skal finnes (vedtatt tidligere)
            val behandlingsresultatTidligereÅrsavregning = Behandlingsresultat().apply {
                id = 50L
                behandling = Behandling.forTest {
                    id = 50L
                    type = Behandlingstyper.ÅRSAVREGNING
                    status = Behandlingsstatus.AVSLUTTET
                    this.fagsak = fagsak
                    registrertDato = LocalDate.now().minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC)
                }
                vedtakMetadata = VedtakMetadata().apply {
                    vedtaksdato = LocalDate.now().minusDays(10).atStartOfDay().toInstant(ZoneOffset.UTC)
                    vedtakstype = Vedtakstyper.ENDRINGSVEDTAK
                }
                årsavregning = Årsavregning.forTest {
                    id = 50
                    aar = 2023
                    trygdeavgiftFraAvgiftssystemet = BigDecimal("8000.00")
                    manueltAvgiftBeloep = BigDecimal("9000.00")
                }
            }

            fagsak.leggTilBehandling(behandlingsresultatTidligereÅrsavregning.hentBehandling())
            fagsak.leggTilBehandling(behandlingsresultat.hentBehandling())

            every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
            every { behandlingsresultatService.hentBehandlingsresultat(50L) } returns behandlingsresultatTidligereÅrsavregning
            every { fagsakService.hentFagsak("12345678") } returns fagsak

            val result = årsavregningService.finnÅrsavregningForBehandling(1)

            result shouldBe ÅrsavregningModel(
                årsavregningID = 112,
                år = 2023,
                tidligereTrygdeavgiftsGrunnlag = Trygdeavgiftsgrunnlag(emptyList(), emptyList(), emptyList()),
                sisteGjeldendeMedlemskapsperioder = emptyList(),
                tidligereAvgift = emptyList(),
                nyttTrygdeavgiftsGrunnlag = null,
                endeligAvgift = emptyList(),
                tidligereFakturertBeloep = BigDecimal("1000.00"),
                beregnetAvgiftBelop = BigDecimal("1500.00"),
                tilFaktureringBeloep = BigDecimal("500.00"),
                harTrygdeavgiftFraAvgiftssystemet = true,
                trygdeavgiftFraAvgiftssystemet = BigDecimal("1200.00"),
                endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET,
                manueltAvgiftBeloep = null,
                tidligereTrygdeavgiftFraAvgiftssystemet = BigDecimal("8000.00"),
                tidligereÅrsavregningmanueltAvgiftBeloep = BigDecimal("9000.00"),
                harSkjoennsfastsattInntektsgrunnlag = false
            )
        }

        @Test
        fun `returnerer null når ingen årsavregning finnes på behandling`() {
            val fagsak = Fagsak.forTest { }
            val behandlingsresultat = Behandlingsresultat().apply {
                behandling = Behandling.forTest {
                    id = 1L
                    type = Behandlingstyper.ÅRSAVREGNING
                    this.fagsak = fagsak
                }
                årsavregning = null
            }

            every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat

            val result = årsavregningService.finnÅrsavregningForBehandling(1)

            result.shouldBeNull()
        }
    }

    @Nested
    inner class Oppdater {
        @Test
        fun `tilFaktureringBeloep skal settes til diff mellom nytt totalbeloep og tidligere fakturert beloep`() {
            val fagsak = Fagsak.forTest { }
            val behandlingsresultat = Behandlingsresultat().apply resultat@{
                behandling = Behandling.forTest {
                    this.fagsak = fagsak
                }
                årsavregning = Årsavregning.forTest {
                    id = 1
                    aar = 2023
                    tidligereFakturertBeloep = BigDecimal.valueOf(12.4)
                    behandlingsresultat = this@resultat
                }
            }
            every { aarsavregningRepository.findById(1L) }.returns(Optional.of(behandlingsresultat.hentÅrsavregning()))
            every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)


            årsavregningService.oppdater(1L, 1L, BigDecimal.valueOf(5.2))


            behandlingsresultat.hentÅrsavregning().tilFaktureringBeloep shouldBe BigDecimal.valueOf(-7.2)
        }

        @Test
        fun `tilFaktureringBeloep skal settes til beregnetAvgiftBelop hvis ikke tidligere avgift er satt`() {
            val fagsak = Fagsak.forTest { }
            val behandlingsresultat = Behandlingsresultat().apply resultat@{
                behandling = Behandling.forTest {
                    this.fagsak = fagsak
                }
                årsavregning = Årsavregning.forTest {
                    id = 1L
                    aar = 2023
                    behandlingsresultat = this@resultat
                }
            }
            every { aarsavregningRepository.findById(1L) }.returns(Optional.of(behandlingsresultat.hentÅrsavregning()))
            every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)


            årsavregningService.oppdater(1L, 1L, BigDecimal.ONE, null, null)


            behandlingsresultat.hentÅrsavregning().tilFaktureringBeloep shouldBe BigDecimal.ONE
        }

        @Test
        fun `tilFaktureringBeloep skal settes hvis avgift i avgiftssystemet og ny avgift ikke er null`() {
            val fagsak = Fagsak.forTest { }
            val behandlingsresultat = Behandlingsresultat().apply resultat@{
                behandling = Behandling.forTest {
                    this.fagsak = fagsak
                }
                årsavregning = Årsavregning.forTest {
                    id = 1L
                    aar = 2023
                    behandlingsresultat = this@resultat
                    harTrygdeavgiftFraAvgiftssystemet = true
                }
            }
            every { aarsavregningRepository.findById(1L) }.returns(Optional.of(behandlingsresultat.hentÅrsavregning()))
            every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)


            årsavregningService.oppdater(1L, 1L, BigDecimal.valueOf(42.0), BigDecimal.valueOf(4.4))


            behandlingsresultat.hentÅrsavregning().tilFaktureringBeloep shouldBe BigDecimal.valueOf(37.6)
        }

        @Test
        fun `tilFaktureringBeloep skal settes til diff mellom beregnetAvgiftBelop og avgift i avgiftssystemet og melosys`() {
            val fagsak = Fagsak.forTest { }
            val behandlingsresultat = Behandlingsresultat().apply resultat@{
                behandling = Behandling.forTest {
                    this.fagsak = fagsak
                }
                årsavregning = Årsavregning.forTest {
                    id = 1L
                    aar = 2023
                    tidligereFakturertBeloep = BigDecimal(37.0)
                    behandlingsresultat = this@resultat
                    harTrygdeavgiftFraAvgiftssystemet = true
                }
            }
            every { aarsavregningRepository.findById(1L) }.returns(Optional.of(behandlingsresultat.hentÅrsavregning()))
            every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)


            årsavregningService.oppdater(1L, 1L, BigDecimal.valueOf(42.0), BigDecimal.valueOf(4.4))


            behandlingsresultat.hentÅrsavregning().tilFaktureringBeloep shouldBe BigDecimal.valueOf(0.6)
        }

        @Test
        fun `harTrygdeavgiftFraAvgiftssystemet skal ikke settes hvis null`() {
            val fagsak = Fagsak.forTest { }
            val behandlingsresultat = Behandlingsresultat().apply resultat@{
                behandling = Behandling.forTest {
                    this.fagsak = fagsak
                }
                årsavregning = Årsavregning.forTest {
                    id = 1L
                    aar = 2023
                    behandlingsresultat = this@resultat
                }
            }
            every { aarsavregningRepository.findById(1L) }.returns(Optional.of(behandlingsresultat.hentÅrsavregning()))
            every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)
            behandlingsresultat.hentÅrsavregning().harTrygdeavgiftFraAvgiftssystemet shouldBe null


            årsavregningService.oppdater(1L, 1L, null, BigDecimal.ONE)


            behandlingsresultat.hentÅrsavregning().harTrygdeavgiftFraAvgiftssystemet shouldBe null
        }
    }

    @Nested
    inner class HentSisteBehandlingsresultatMedInnvilgetMedlemskapsperiodeOgAvgiftsgrunnlag {
        @Test
        fun `henter nyeste behandlingsresultat med grunnlag og riktig år for opprettelse av ny årsavregning`() {
            val aktivFagsak = Fagsak.forTest {
                saksnummer = "123456"
            }

            val eldreBehandlingsresultat = lagTidligereBehandlingsresultat().apply {
                id = 1
                type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
                behandling = Behandling.forTest().apply behandling@{
                    id = 1
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false))
            }

            val nyesteBehandlingsresultat = lagTidligereBehandlingsresultat().apply {
                id = 2
                type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
                behandling = Behandling.forTest().apply behandling@{
                    id = 2
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 1, 10).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-08-31", medTrygdeavgift = false))
            }


            every { fagsakService.hentFagsak("123456") } returns aktivFagsak
            every { behandlingsresultatService.hentBehandlingsresultat(1) } returns eldreBehandlingsresultat
            every { behandlingsresultatService.hentBehandlingsresultat(2) } returns nyesteBehandlingsresultat


            // Med ny logikk: nyesteBehandlingsresultat har medlemskapsperioder, men ingen har avgiftsgrunnlag
            årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)
                .shouldBe(
                    GjeldendeBehandlingsresultaterForÅrsavregning(
                        sisteBehandlingsresultatMedAvgiftspliktigPeriode = nyesteBehandlingsresultat,
                        sisteBehandlingsresultatMedAvgift = null
                    )
                )
            verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
        }

        @Test
        fun `henter nyeste behandlingsresultat med manuellAvgift satt og uten medlemskapsperioder ved opprettelse av årsavregning`() {
            val aktivFagsak = Fagsak.forTest {
                saksnummer = "123456"
            }

            val eldreBehandlingsresultat = lagTidligereBehandlingsresultat().apply {
                id = 1
                type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
                behandling = Behandling.forTest().apply behandling@{
                    id = 1
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false))
            }

            val behandlingsresultatMedManuelAvgift = lagTidligereBehandlingsresultat().apply {
                id = 2
                type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
                årsavregning = Årsavregning.forTest {
                    id = 2
                    aar = 2023
                    manueltAvgiftBeloep = BigDecimal.valueOf(1000.0)
                }
                behandling = Behandling.forTest().apply behandling@{
                    id = 2
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 1, 10).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = mutableSetOf()
            }


            every { fagsakService.hentFagsak("123456") } returns aktivFagsak
            every { behandlingsresultatService.hentBehandlingsresultat(1) } returns eldreBehandlingsresultat
            every { behandlingsresultatService.hentBehandlingsresultat(2) } returns behandlingsresultatMedManuelAvgift


            // Med ny logikk: eldreBehandlingsresultat har medlemskapsperioder, men ingen behandling har avgiftsgrunnlag
            årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)
                .shouldBe(
                    GjeldendeBehandlingsresultaterForÅrsavregning(
                        sisteBehandlingsresultatMedAvgiftspliktigPeriode = eldreBehandlingsresultat,
                        sisteBehandlingsresultatMedAvgift = null,
                        sisteÅrsavregning = behandlingsresultatMedManuelAvgift
                    )
                )
            verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
        }

        @ParameterizedTest
        @EnumSource(Behandlingsresultattyper::class, names = ["FERDIGBEHANDLET", "HENLEGGELSE_BORTFALT"])
        fun `ekskluderer årsavregninger uten vedtak`() {
            val aktivFagsak = Fagsak.forTest {
                saksnummer = "123456"
            }

            val eldreForstegangsbehandlingsresultat = lagTidligereBehandlingsresultat().apply {
                id = 1
                type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
                behandling = Behandling.forTest().apply behandling@{
                    id = 1
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false))
            }

            val nyttÅrsavregningsbehandlingsresultat = lagTidligereBehandlingsresultat().apply {
                id = 2
                type = Behandlingsresultattyper.FERDIGBEHANDLET
                behandling = Behandling.forTest().apply behandling@{
                    id = 1
                    type = Behandlingstyper.ÅRSAVREGNING
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false))
            }

            every { fagsakService.hentFagsak("123456") } returns aktivFagsak
            every { behandlingsresultatService.hentBehandlingsresultat(1) } returns eldreForstegangsbehandlingsresultat
            every { behandlingsresultatService.hentBehandlingsresultat(2) } returns nyttÅrsavregningsbehandlingsresultat


            // Med ny logikk: eldreForstegangsbehandlingsresultat har medlemskapsperioder, men ingen avgift
            årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)
                .shouldBe(
                    GjeldendeBehandlingsresultaterForÅrsavregning(
                        sisteBehandlingsresultatMedAvgiftspliktigPeriode = eldreForstegangsbehandlingsresultat,
                        sisteBehandlingsresultatMedAvgift = null
                    )
                )
            verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
        }

        @Test
        fun `henter årsavregning med resulttatype FASTSATT_TRYGDEAVGIFT`() {
            val aktivFagsak = Fagsak.forTest {
                saksnummer = "123456"
            }

            val forstegangsbehandlingsresultat = lagTidligereBehandlingsresultat().apply {
                id = 1
                type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
                behandling = Behandling.forTest().apply behandling@{
                    id = 1
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false))
            }

            val vedtattAarsavregningsresultat = lagTidligereBehandlingsresultat().apply {
                id = 2
                type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
                behandling = Behandling.forTest().apply behandling@{
                    id = 2
                    type = Behandlingstyper.ÅRSAVREGNING
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false))
            }

            val ferdigbehandletAarsavregningsresultat = lagTidligereBehandlingsresultat().apply {
                id = 3
                type = Behandlingsresultattyper.FERDIGBEHANDLET
                behandling = Behandling.forTest().apply behandling@{
                    id = 3
                    type = Behandlingstyper.ÅRSAVREGNING
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31", medTrygdeavgift = false))
            }

            every { fagsakService.hentFagsak("123456") } returns aktivFagsak
            every { behandlingsresultatService.hentBehandlingsresultat(1) } returns forstegangsbehandlingsresultat
            every { behandlingsresultatService.hentBehandlingsresultat(2) } returns vedtattAarsavregningsresultat
            every { behandlingsresultatService.hentBehandlingsresultat(3) } returns ferdigbehandletAarsavregningsresultat


            // Med ny logikk: vedtattAarsavregningsresultat har medlemskapsperioder, men ingen avgift
            årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)
                .shouldBe(
                    GjeldendeBehandlingsresultaterForÅrsavregning(
                        sisteBehandlingsresultatMedAvgiftspliktigPeriode = vedtattAarsavregningsresultat,
                        sisteBehandlingsresultatMedAvgift = null
                    )
                )
            verify(exactly = 3) { behandlingsresultatService.hentBehandlingsresultat(any()) }
        }

        @Test
        fun `henter separate behandlinger for medlemskapsperiode og avgiftsgrunnlag når de er forskjellige`() {
            // Scenario 4: Tidligere årsavregning med senere ny vurdering med ulikt medlemskapsperiode
            val aktivFagsak = Fagsak.forTest {
                saksnummer = "123456"
            }

            // Første behandling med medlemskap og avgift
            val forsteBehandlingsresultat = lagTidligereBehandlingsresultat().apply {
                id = 1
                type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
                behandling = Behandling.forTest().apply behandling@{
                    id = 1
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-12-31"))
            }

            // Årsavregning basert på første behandling
            val aarsavregningsresultat = Behandlingsresultat().apply {
                id = 2
                type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
                behandling = Behandling.forTest().apply behandling@{
                    id = 2
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-12-31"))
                årsavregning = Årsavregning.forTest {
                    aar = 2023
                    manueltAvgiftBeloep = null
                }
            }

            // Ny vurdering med endret medlemskapsperiode (kortere periode)
            val nyVurderingMedEndretMedlemskap = Behandlingsresultat().apply {
                id = 3
                type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
                behandling = Behandling.forTest().apply behandling@{
                    id = 3
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 9, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder =
                    mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-06-30", medTrygdeavgift = false)) // Endret periode, ingen avgift
            }

            every { fagsakService.hentFagsak("123456") } returns aktivFagsak
            every { behandlingsresultatService.hentBehandlingsresultat(1) } returns forsteBehandlingsresultat
            every { behandlingsresultatService.hentBehandlingsresultat(2) } returns aarsavregningsresultat
            every { behandlingsresultatService.hentBehandlingsresultat(3) } returns nyVurderingMedEndretMedlemskap

            val resultat = årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)

            // Verifiserer at medlemskapsperiode kommer fra ny vurdering
            resultat?.sisteBehandlingsresultatMedAvgiftspliktigPeriode shouldBe nyVurderingMedEndretMedlemskap
            // Verifiserer at avgiftsgrunnlag kommer fra årsavregning
            resultat?.sisteBehandlingsresultatMedAvgift shouldBe aarsavregningsresultat

            verify(exactly = 3) { behandlingsresultatService.hentBehandlingsresultat(any()) }
        }

        @Test
        fun `henter samme behandling for medlemskap og avgift når medlemskapsperiode ikke er endret`() {
            // Scenario 3: Tidligere årsavregning med senere ny vurdering med likt medlemskapsperiode
            val aktivFagsak = Fagsak.forTest {
                saksnummer = "123456"
            }

            val aarsavregningsresultat = Behandlingsresultat().apply {
                id = 1
                type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
                behandling = Behandling.forTest().apply behandling@{
                    id = 1
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-12-31"))
                årsavregning = Årsavregning.forTest {
                    aar = 2023
                    manueltAvgiftBeloep = null
                }
            }

            // Ny vurdering med samme medlemskapsperiode - ingen trygdeavgift siden det er ny vurdering
            val nyVurderingSammeMedlemskap = Behandlingsresultat().apply {
                id = 2
                type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
                behandling = Behandling.forTest().apply behandling@{
                    id = 2
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 9, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder =
                    mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-12-31", medTrygdeavgift = false)) // Samme periode, men uten avgift
            }

            every { fagsakService.hentFagsak("123456") } returns aktivFagsak
            every { behandlingsresultatService.hentBehandlingsresultat(1) } returns aarsavregningsresultat
            every { behandlingsresultatService.hentBehandlingsresultat(2) } returns nyVurderingSammeMedlemskap

            val resultat = årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)

            // Medlemskapsperiode kommer fra nyeste, avgift kommer fra den som har avgift
            resultat?.sisteBehandlingsresultatMedAvgiftspliktigPeriode shouldBe nyVurderingSammeMedlemskap
            resultat?.sisteBehandlingsresultatMedAvgift shouldBe aarsavregningsresultat

            verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
        }

        @Test
        fun `håndterer behandling uten trygdeavgiftsperioder korrekt`() {
            val aktivFagsak = Fagsak.forTest {
                saksnummer = "123456"
            }

            // Behandling med medlemskap men uten trygdeavgiftsperioder
            val behandlingUtenAvgift = Behandlingsresultat().apply {
                id = 1
                type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
                behandling = Behandling.forTest().apply behandling@{
                    id = 1
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-12-31", medTrygdeavgift = false))
            }

            // Behandling med både medlemskap og avgift
            val behandlingMedAvgift = Behandlingsresultat().apply {
                id = 2
                type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
                behandling = Behandling.forTest().apply behandling@{
                    id = 2
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = mutableSetOf(lagMedlemskapsperiode("2023-01-01", "2023-12-31"))
            }

            every { fagsakService.hentFagsak("123456") } returns aktivFagsak
            every { behandlingsresultatService.hentBehandlingsresultat(1) } returns behandlingUtenAvgift
            every { behandlingsresultatService.hentBehandlingsresultat(2) } returns behandlingMedAvgift

            val resultat = årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("123456", 2023)

            resultat?.sisteBehandlingsresultatMedAvgiftspliktigPeriode shouldBe behandlingMedAvgift
            resultat?.sisteBehandlingsresultatMedAvgift shouldBe behandlingMedAvgift

            verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
        }

    }

    @Nested
    inner class ResetEksisterendeÅrsavregning {
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
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 9, 30),
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

    @Nested
    inner class OppdaterHarTrygdeavgiftFraAvgiftssystemet {
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
                unleash
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

    fun lagTidligereBehandlingsresultat(): Behandlingsresultat = Behandlingsresultat().apply {
        id = 1L
        type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
        vedtakMetadata = VedtakMetadata()
        vedtakMetadata!!.vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
        medlemskapsperioder = mutableSetOf(
            lagMedlemskapsperiode("2022-01-01", "2022-08-31"),
            lagMedlemskapsperiode("2022-09-01", "2023-05-31"),
            lagMedlemskapsperiode("2023-07-01", "2023-08-31", InnvilgelsesResultat.AVSLAATT)
        )
    }

    private fun lagMedlemskapsperiode(
        start: String,
        slutt: String,
        innvilgelsesResultat: InnvilgelsesResultat = InnvilgelsesResultat.INNVILGET,
        medTrygdeavgift: Boolean = true,
        forskuddsvisFaktura: Boolean = true
    ): Medlemskapsperiode {
        return Medlemskapsperiode().apply {
            trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
            innvilgelsesresultat = innvilgelsesResultat
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            fom = LocalDate.parse(start)
            tom = LocalDate.parse(slutt)
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
            trygdeavgiftsperioder = if (medTrygdeavgift) {
                mutableSetOf(lagTrygdeavgift(start, slutt, forskuddsvisFaktura))
            } else {
                mutableSetOf()
            }
        }
    }

    private fun lagTrygdeavgift(start: String, slutt: String, forskuddsvisFaktura: Boolean = true): Trygdeavgiftsperiode = Trygdeavgiftsperiode(
        periodeFra = LocalDate.parse(start),
        periodeTil = LocalDate.parse(slutt),
        trygdeavgiftsbeløpMd = Penger(5000.0),
        trygdesats = BigDecimal(3.5),
        grunnlagInntekstperiode = lagInntektsperiode(start, slutt),
        grunnlagSkatteforholdTilNorge = lagSkatteforholdTilNorge(start, slutt),
        forskuddsvisFaktura = forskuddsvisFaktura
    )

    private fun lagInntektsperiode(start: String, slutt: String): Inntektsperiode = Inntektsperiode().apply {
        fomDato = LocalDate.parse(start)
        tomDato = LocalDate.parse(slutt)
        avgiftspliktigMndInntekt = Penger(5000.0)
        avgiftspliktigTotalinntekt = Penger(5000.0)
        type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
    }

    private fun lagSkatteforholdTilNorge(start: String, slutt: String): SkatteforholdTilNorge = SkatteforholdTilNorge().apply {
        fomDato = LocalDate.parse(start)
        tomDato = LocalDate.parse(slutt)
        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
    }
}
