package no.nav.melosys.service.avgift.aarsavregning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.AarsavregningRepository
import no.nav.melosys.service.behandling.BehandlingService
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
    private lateinit var behandlingService: BehandlingService

    private lateinit var årsavregningService: ÅrsavregningService

    @BeforeEach
    fun setup() {
        årsavregningService = ÅrsavregningService(
            aarsavregningRepository,
            behandlingsresultatService,
            fagsakService,
            behandlingService
        )
        SpringSubjectHandler.set(TestSubjectHandler())
    }

    @Nested
    inner class OpprettÅrsavregning {
        @Test
        fun `Ny årsavregning kaster feil når flere årsavregninger eksisterer for samme år på samme Fagsak`() {
            val årsavregningEntity1 = Årsavregning().apply {
                aar = 2023
                behandlingsresultat = Behandlingsresultat()
            }
            val eksisterendeBehandling = Behandling().apply { id = 1L }
            every { aarsavregningRepository.findById(1L) }.returns(Optional.of(årsavregningEntity1))
            every { aarsavregningRepository.finnAntallÅrsavregningerPåFagsakForÅr(1, 2023) }.returns(1)
            every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(Behandlingsresultat().apply {
                behandling = eksisterendeBehandling
            })

            shouldThrow<FunksjonellException> {
                årsavregningService.opprettÅrsavregning(1, 2023)
            }
        }
    }

    @Nested
    inner class FinnÅrsavregningForBehandling {
        @Test
        fun `finnÅrsavregning for ny årsavregning uten info i Melosys`() {
            val behandlingsresultat = Behandlingsresultat().apply {
                behandling = Behandling().apply {
                    id = 1L
                    type = Behandlingstyper.ÅRSAVREGNING
                }
            }
            val årsavregningEntity = Årsavregning().apply {
                id = 112
                aar = 2023
                this.behandlingsresultat = behandlingsresultat
            }
            behandlingsresultat.årsavregning = årsavregningEntity
            every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)

            årsavregningService.finnÅrsavregningForBehandling(1) shouldBe ÅrsavregningModel(
                årsavregningID = 112,
                år = 2023,
                tidligereGrunnlag = null,
                tidligereAvgift = emptyList(),
                nyttGrunnlag = null,
                endeligAvgift = emptyList(),
                tidligereFakturertBeloep = null,
                nyttTotalbeloep = null,
                tilFaktureringBeloep = null
            )
        }

        @Test
        fun `finnÅrsavregning for ny årsavregning, grunnlag finnes i Melosys`() {
            val behandlingsresultat = Behandlingsresultat().apply {
                behandling = Behandling().apply {
                    id = 1L
                    type = Behandlingstyper.ÅRSAVREGNING
                }
            }
            val årsavregningEntity = Årsavregning().apply {
                id = 112
                aar = 2023
                this.behandlingsresultat = behandlingsresultat
                tidligereBehandlingsresultat = lagTidligereBehandlingsresultat()
            }
            behandlingsresultat.årsavregning = årsavregningEntity
            every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)

            årsavregningService.finnÅrsavregningForBehandling(1) shouldBe ÅrsavregningModel(
                årsavregningID = 112,
                år = 2023,
                tidligereGrunnlag = Trygdeavgiftsgrunnlag(
                    listOf(
                        MedlemskapsperiodeForAvgift(
                            fom = LocalDate.of(2023, 1, 1),
                            tom = LocalDate.of(2023, 5, 31),
                            dekning = Trygdedekninger.FULL_DEKNING_FTRL,
                            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8,
                            medlemskapstyper = Medlemskapstyper.FRIVILLIG
                        )
                    ),
                    listOf(
                        SkatteforholdTilNorgeForAvgift(lagSkatteforholdTilNorge("2023-01-01", "2023-05-01"))
                    ),
                    listOf(
                        InntektsperioderForAvgift(lagInntektsperiode("2023-01-01", "2023-05-01"))
                    )
                ),
                tidligereAvgift = listOf(
                    lagTrygdeavgift("2023-01-01", "2023-05-01")
                ),
                nyttGrunnlag = null,
                endeligAvgift = emptyList(),
                tidligereFakturertBeloep = null,
                nyttTotalbeloep = null,
                tilFaktureringBeloep = null
            )
        }
    }

    @Nested
    inner class Oppdater {
        @Test
        fun `tilFaktureringBeloep skal settes til diff mellom nytt totalbeloep og tidligere fakturert beloep`() {
            val behandlingsresultat = Behandlingsresultat().apply resultat@{
                behandling = Behandling()
                årsavregning = Årsavregning().apply {
                    id = 1
                    aar = 2023
                    behandlingsresultat = this@resultat
                }
            }
            every { aarsavregningRepository.findById(1L) }.returns(Optional.of(behandlingsresultat.årsavregning))
            every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)

            årsavregningService.oppdater(1L, 1L, BigDecimal.valueOf(12.4), BigDecimal.valueOf(5.2))
            behandlingsresultat.årsavregning.tilFaktureringBeloep shouldBe BigDecimal.valueOf(-7.2)
        }

        @Test
        fun `tilFaktureringBeloep skal ikke settes hvis tidligere eller ny avgift er null`() {
            val behandlingsresultat = Behandlingsresultat().apply resultat@{
                behandling = Behandling()
                årsavregning = Årsavregning().apply {
                    id = 1L
                    aar = 2023
                    behandlingsresultat = this@resultat
                }
            }
            every { aarsavregningRepository.findById(1L) }.returns(Optional.of(behandlingsresultat.årsavregning))
            every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)

            årsavregningService.oppdater(1L, 1L, null, BigDecimal.ONE)
            behandlingsresultat.årsavregning.tilFaktureringBeloep shouldBe null
        }
    }

    @Nested
    inner class HentSisteBehandlingsresultatMedInnvilgetMedlemskapsperiodeOgAvgiftsgrunnlag {
        @Test
        fun `henter nyeste behandlingsresultat med grunnlag og riktig år for opprettelse av ny årsavregning`() {
            val aktivFagsak = FagsakTestFactory.Builder().saksnummer("123456").build()

            val eldreBehandlingsresultat = lagTidligereBehandlingsresultat().apply {
                id = 1
                type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
                behandling = Behandling().apply behandling@{
                    id = 1
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = listOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31").apply { trygdeavgiftsperioder = null })
            }

            val nyesteBehandlingsresultat = lagTidligereBehandlingsresultat().apply {
                id = 2
                type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
                behandling = Behandling().apply behandling@{
                    id = 2
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 1, 10).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = listOf(lagMedlemskapsperiode("2023-01-01", "2023-08-31").apply { trygdeavgiftsperioder = null })
            }


            every { fagsakService.hentFagsak("123456") } returns aktivFagsak
            every { behandlingsresultatService.hentBehandlingsresultat(1) } returns eldreBehandlingsresultat
            every { behandlingsresultatService.hentBehandlingsresultat(2) } returns nyesteBehandlingsresultat


            årsavregningService.hentSisteBehandlingsresultatMedInnvilgetMedlemskapsperiodeOgAvgiftsgrunnlag("123456", 2023)
                .shouldBe(nyesteBehandlingsresultat)
            verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
        }

        @ParameterizedTest
        @EnumSource(Behandlingsresultattyper::class, names = ["FERDIGBEHANDLET", "HENLEGGELSE_BORTFALT"])
        fun `ekskluderer årsavregninger uten vedtak`(behandlingsresultattyper: Behandlingsresultattyper) {
            val aktivFagsak = FagsakTestFactory.Builder().saksnummer("123456").build()

            val eldreForstegangsbehandlingsresultat = lagTidligereBehandlingsresultat().apply {
                id = 1
                type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
                behandling = Behandling().apply behandling@{
                    id = 1
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = listOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31").apply { trygdeavgiftsperioder = null })
            }

            val nyttÅrsavregningsbehandlingsresultat = lagTidligereBehandlingsresultat().apply {
                id = 2
                type = Behandlingsresultattyper.FERDIGBEHANDLET
                behandling = Behandling().apply behandling@{
                    id = 1
                    type = Behandlingstyper.ÅRSAVREGNING
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = listOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31").apply { trygdeavgiftsperioder = null })
            }

            every { fagsakService.hentFagsak("123456") } returns aktivFagsak
            every { behandlingsresultatService.hentBehandlingsresultat(1) } returns eldreForstegangsbehandlingsresultat
            every { behandlingsresultatService.hentBehandlingsresultat(2) } returns nyttÅrsavregningsbehandlingsresultat


            årsavregningService.hentSisteBehandlingsresultatMedInnvilgetMedlemskapsperiodeOgAvgiftsgrunnlag("123456", 2023)
                .shouldBe(eldreForstegangsbehandlingsresultat)
            verify(exactly = 2) { behandlingsresultatService.hentBehandlingsresultat(any()) }
        }

        @Test
        fun `henter årsavregning med resulttatype FASTSATT_TRYGDEAVGIFT`() {
            val aktivFagsak = FagsakTestFactory.Builder().saksnummer("123456").build()

            val forstegangsbehandlingsresultat = lagTidligereBehandlingsresultat().apply {
                id = 1
                type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
                behandling = Behandling().apply behandling@{
                    id = 1
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = listOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31").apply { trygdeavgiftsperioder = null })
            }

            val vedtattAarsavregningsresultat = lagTidligereBehandlingsresultat().apply {
                id = 2
                type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
                behandling = Behandling().apply behandling@{
                    id = 2
                    type = Behandlingstyper.ÅRSAVREGNING
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = listOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31").apply { trygdeavgiftsperioder = null })
            }

            val ferdigbehandletAarsavregningsresultat = lagTidligereBehandlingsresultat().apply {
                id = 3
                type = Behandlingsresultattyper.FERDIGBEHANDLET
                behandling = Behandling().apply behandling@{
                    id = 3
                    type = Behandlingstyper.ÅRSAVREGNING
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak = aktivFagsak.apply { leggTilBehandling(this@behandling) }
                }
                registrertDato = LocalDate.of(2023, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
                medlemskapsperioder = listOf(lagMedlemskapsperiode("2023-09-01", "2023-12-31").apply { trygdeavgiftsperioder = null })
            }

            every { fagsakService.hentFagsak("123456") } returns aktivFagsak
            every { behandlingsresultatService.hentBehandlingsresultat(1) } returns forstegangsbehandlingsresultat
            every { behandlingsresultatService.hentBehandlingsresultat(2) } returns vedtattAarsavregningsresultat
            every { behandlingsresultatService.hentBehandlingsresultat(3) } returns ferdigbehandletAarsavregningsresultat


            årsavregningService.hentSisteBehandlingsresultatMedInnvilgetMedlemskapsperiodeOgAvgiftsgrunnlag("123456", 2023)
                .shouldBe(vedtattAarsavregningsresultat)
            verify(exactly = 3) { behandlingsresultatService.hentBehandlingsresultat(any()) }
        }
    }

    @Nested
    inner class endreÅrsavregningOppsummering{
        @Test
        fun `oppdaterer mottaksdato og behandlingsstatus for årsavregning oppsummering`(){

            val behandlingsresultat = Behandlingsresultat().apply resultat@{
                behandling = Behandling().apply {
                    id = 1
                    type = Behandlingstyper.ÅRSAVREGNING
                    status = Behandlingsstatus.UNDER_BEHANDLING
                    behandlingsårsak = Behandlingsaarsak().apply {
                        mottaksdato = LocalDate.of(2023, 1, 1)
                    }
                }

                årsavregning = Årsavregning().apply {
                    id = 1
                    aar = 2023
                    behandlingsresultat = this@resultat
                }
            }


        }
    }


    fun lagTidligereBehandlingsresultat(): Behandlingsresultat = Behandlingsresultat().apply {
        id = 1L
        type = Behandlingsresultattyper.FERDIGBEHANDLET
        vedtakMetadata = VedtakMetadata()
        vedtakMetadata.vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
        medlemskapsperioder = listOf(
            lagMedlemskapsperiode("2022-01-01", "2022-08-31"),
            lagMedlemskapsperiode("2022-09-01", "2023-05-31"),
            lagMedlemskapsperiode("2023-07-01", "2023-08-31", InnvilgelsesResultat.AVSLAATT)
        )
    }

    private fun lagMedlemskapsperiode(
        start: String,
        slutt: String,
        innvilgelsesResultat: InnvilgelsesResultat = InnvilgelsesResultat.INNVILGET
    ): Medlemskapsperiode {
        return Medlemskapsperiode().apply {
            trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
            innvilgelsesresultat = innvilgelsesResultat
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            fom = LocalDate.parse(start)
            tom = LocalDate.parse(slutt)
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
            trygdeavgiftsperioder = setOf(lagTrygdeavgift("2022-01-01", "2022-08-31"), lagTrygdeavgift("2023-01-01", "2023-05-01"))
        }
    }

    private fun lagTrygdeavgift(start: String, slutt: String): Trygdeavgiftsperiode = Trygdeavgiftsperiode(
        periodeFra = LocalDate.parse(start),
        periodeTil = LocalDate.parse(slutt),
        trygdeavgiftsbeløpMd = Penger(5000.0),
        trygdesats = BigDecimal(3.5),
        grunnlagInntekstperiode = lagInntektsperiode(start, slutt),
        grunnlagSkatteforholdTilNorge = lagSkatteforholdTilNorge(start, slutt)
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
