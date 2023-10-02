package no.nav.melosys.service.avgift

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import no.nav.melosys.service.MedlemAvFolketrygdenService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
internal class TrygdeavgiftsberegningServiceTest {
    @MockK
    private lateinit var mockMedlemAvFolketrygdenService: MedlemAvFolketrygdenService

    @MockK
    private lateinit var mockTrygdeavgiftConsumer: TrygdeavgiftConsumer

    private lateinit var trygdeavgiftsberegningService: TrygdeavgiftsberegningService

    private lateinit var medlemAvFolketrygden: MedlemAvFolketrygden
    private val FOM: LocalDate = LocalDate.now()
    private val TOM: LocalDate = LocalDate.now().plusMonths(1)
    private val BEHANDLING_ID: Long = 1291


    @BeforeEach
    fun setup() {
        trygdeavgiftsberegningService =
            TrygdeavgiftsberegningService(
                mockMedlemAvFolketrygdenService,
                mockTrygdeavgiftConsumer
            )
        medlemAvFolketrygden = MedlemAvFolketrygden()
        every { mockMedlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID) }.returns(medlemAvFolketrygden)
    }

    @Test
    fun hentTrygdeavgiftsberegning_ingenTrygdeavgift_returnerTomListe() {
        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift()
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder = null
        every { mockMedlemAvFolketrygdenService.finnMedlemAvFolketrygden(BEHANDLING_ID) }.returns(Optional.of(medlemAvFolketrygden))


        trygdeavgiftsberegningService.hentTrygdeavgiftsberegning(BEHANDLING_ID)
            .shouldNotBeNull()
            .shouldBeEmpty()
    }

    @Test
    fun hentTrygdeavgiftsberegning_ingenFastsattTrygdeavgift_returnerTomListe() {
        medlemAvFolketrygden.fastsattTrygdeavgift = null
        every { mockMedlemAvFolketrygdenService.finnMedlemAvFolketrygden(BEHANDLING_ID) }.returns(Optional.of(medlemAvFolketrygden))


        trygdeavgiftsberegningService.hentTrygdeavgiftsberegning(BEHANDLING_ID)
            .shouldNotBeNull()
            .shouldBeEmpty()
    }

    @Test
    fun hentTrygdeavgiftsberegning_ingenMedlemAvFolketrygden_returnerTomListe() {
        every { mockMedlemAvFolketrygdenService.finnMedlemAvFolketrygden(BEHANDLING_ID) }.returns(Optional.empty())


        trygdeavgiftsberegningService.hentTrygdeavgiftsberegning(BEHANDLING_ID)
            .shouldNotBeNull()
            .shouldBeEmpty()
    }

    @Test
    fun beregnTrygdeavgift_skalBetaleTrygeavgift_beregnerOgLagrerTrygdeavgift() {
        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        })
        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                skatteforholdTilNorge = listOf(SkatteforholdTilNorge().apply {
                    id = 1L
                    fomDato = FOM
                    tomDato = TOM
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                })
                inntektsperioder = listOf(Inntektsperiode().apply {
                    id = 1L
                    fomDato = FOM
                    tomDato = TOM
                    type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                    isArbeidsgiversavgiftBetalesTilSkatt = false
                    isOrdinærTrygdeavgiftBetalesTilSkatt = false
                    avgiftspliktigInntektMnd = Penger(10000.0)
                })
            }
        }
        medlemAvFolketrygden.fastsattTrygdeavgift.medlemAvFolketrygden = medlemAvFolketrygden
        every { mockMedlemAvFolketrygdenService.lagre(any()) }.returns(medlemAvFolketrygden)
        every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }
            .returns(
                listOf(
                    TrygdeavgiftsberegningResponse(
                        TrygdeavgiftsperiodeDto(
                            DatoPeriodeDto(FOM, TOM),
                            BigDecimal.valueOf(7.9),
                            PengerDto(BigDecimal.valueOf(790), NOK)
                        ),
                        TrygdeavgiftsgrunnlagDto(
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            UUID.randomUUID()
                        )
                    )
                )
            )


        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID)
            .shouldNotBeNull()
            .shouldNotBeEmpty()
            .forEach {
                it.apply {
                    trygdesats = BigDecimal.valueOf(7.9)
                    trygdeavgiftsbeløpMd = Penger(790.0)
                }
            }
        verify { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }
        verify { mockMedlemAvFolketrygdenService.lagre(medlemAvFolketrygden) }
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.shouldNotBeEmpty()
    }

    @Test
    fun beregnTrygdeavgift_skalIkkeBetaleTrygdeavgiftTilNav_sletterEksisterendeTrygdeavgiftOgReturnererTomListe() {
        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        })
        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                skatteforholdTilNorge = listOf(SkatteforholdTilNorge().apply {
                    id = 1L
                    fomDato = FOM
                    tomDato = TOM
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                })
                inntektsperioder = listOf(Inntektsperiode().apply {
                    id = 1L
                    fomDato = FOM
                    tomDato = TOM
                    type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                    isOrdinærTrygdeavgiftBetalesTilSkatt = true
                    isArbeidsgiversavgiftBetalesTilSkatt = true
                    avgiftspliktigInntektMnd = null
                })
            }
            trygdeavgiftsperioder.add(Trygdeavgiftsperiode())
        }


        medlemAvFolketrygden.fastsattTrygdeavgift.medlemAvFolketrygden = medlemAvFolketrygden
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.shouldNotBeEmpty()
        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID)
            .shouldNotBeNull()
            .shouldBeEmpty()
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.shouldBeEmpty()
    }

    @Test
    fun beregnTrygdeavgift_manglerMedlemskapsperioder_kasterFeil() {
        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID)
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten medlemskapsperioder")
    }

    @Test
    fun beregnTrygdeavgift_manglerFastsattTrygdeavgift_kasterFeil() {
        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode())

        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID)
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten fastsattTrygdeavgift")
    }

    @Test
    fun beregnTrygdeavgift_manglerTrygdeavgiftsgrunnlag_kasterFeil() {
        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode())
        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift()

        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID)
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten trygdeavgiftsgrunnlag")
    }

    @Test
    fun beregnTrygdeavgift_manglerSkatteforholdINorge_kasterFeil() {
        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode())
        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag()
        }

        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID)
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten skatteforholdTilNorge")
    }

    @Test
    fun beregnTrygdeavgift_manglerInntektsperioder_kasterFeil() {
        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode())
        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                skatteforholdTilNorge.add(SkatteforholdTilNorge())
            }
        }

        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID)
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten inntektsperioder")
    }

    @Test
    fun beregnOgLagreTrygdeavgift_inntektsperioderDekkerIkkeAlleMedlemskapsperioder_kasterFeil() {
        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        })
        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 2L
            fom = TOM.plusDays(1)
            tom = TOM.plusMonths(1)
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        })

        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                skatteforholdTilNorge = listOf(SkatteforholdTilNorge().apply {
                    id = 1L
                    fomDato = FOM
                    tomDato = TOM
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                })
                inntektsperioder = listOf(Inntektsperiode().apply {
                    id = 1L
                    fomDato = FOM
                    tomDato = TOM
                    type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                    isArbeidsgiversavgiftBetalesTilSkatt = false
                    isOrdinærTrygdeavgiftBetalesTilSkatt = false
                    avgiftspliktigInntektMnd = Penger(10000.0)
                }, Inntektsperiode().apply {
                    id = 2L
                    fomDato = TOM.plusDays(2)
                    tomDato = TOM.plusMonths(1)
                    type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                    isArbeidsgiversavgiftBetalesTilSkatt = false
                    isOrdinærTrygdeavgiftBetalesTilSkatt = false
                    avgiftspliktigInntektMnd = Penger(10000.0)
                })
            }
        }
        medlemAvFolketrygden.fastsattTrygdeavgift.medlemAvFolketrygden = medlemAvFolketrygden
        every { mockMedlemAvFolketrygdenService.lagre(any()) }.returns(medlemAvFolketrygden)
        every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }

        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID)
        }.message.shouldContain("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun `Inntektsperioder er uten opphold og starter slutter på samme dato som medlemskapsperioder - ok`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28")),
            lagMedlemskapsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        val inntektsperioder: List<Inntektsperiode> = listOf(
            lagInntektsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-15")),
            lagInntektsperiode(LocalDate.parse("2023-01-10"), LocalDate.parse("2023-02-28")),
            lagInntektsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        shouldNotThrowAny {
            TrygdeavgiftsberegningService.validerInntekstperioderDekkerMedlemskapsperioder(
                inntektsperioder,
                medlemskapsperioder
            )
        }
    }

    @Test
    fun `Inntektsperioder er uten opphold og slutter ikke på samme dato som medlemskapsperioder - false`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28")),
            lagMedlemskapsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        val inntektsperioder: List<Inntektsperiode> = listOf(
            lagInntektsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-15")),
            lagInntektsperiode(LocalDate.parse("2023-01-10"), LocalDate.parse("2023-02-28")),
            lagInntektsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-05"))
        )

        shouldThrow<FunksjonellException> {
            TrygdeavgiftsberegningService.validerInntekstperioderDekkerMedlemskapsperioder(
                inntektsperioder,
                medlemskapsperioder
            )
        }.message.shouldContain("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun `Inntektsperioder er uten opphold og starter ikke på samme dato som medlemskapsperioder - false`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28")),
            lagMedlemskapsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        val inntektsperioder: List<Inntektsperiode> = listOf(
            lagInntektsperiode(LocalDate.parse("2023-01-03"), LocalDate.parse("2023-01-15")),
            lagInntektsperiode(LocalDate.parse("2023-01-10"), LocalDate.parse("2023-02-28")),
            lagInntektsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        shouldThrow<FunksjonellException> {
            TrygdeavgiftsberegningService.validerInntekstperioderDekkerMedlemskapsperioder(
                inntektsperioder,
                medlemskapsperioder
            )
        }.message.shouldContain("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun `Inntektsperioder har et opphold - false`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28")),
            lagMedlemskapsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        val inntektsperioder: List<Inntektsperiode> = listOf(
            lagInntektsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-15")),
            lagInntektsperiode(LocalDate.parse("2023-01-10"), LocalDate.parse("2023-02-20")),
            lagInntektsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        shouldThrow<FunksjonellException> {
            TrygdeavgiftsberegningService.validerInntekstperioderDekkerMedlemskapsperioder(
                inntektsperioder,
                medlemskapsperioder
            )
        }.message.shouldContain("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun `Inntektsperioder har flere med samme fom dato uten opphold- ok`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28")),
            lagMedlemskapsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        val inntektsperioder: List<Inntektsperiode> = listOf(
            lagInntektsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-02-28")),
            lagInntektsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-15")),
            lagInntektsperiode(LocalDate.parse("2023-01-14"), LocalDate.parse("2023-02-20")),
            lagInntektsperiode(LocalDate.parse("2023-02-22"), LocalDate.parse("2023-05-31")),
        )

        TrygdeavgiftsberegningService.validerInntekstperioderDekkerMedlemskapsperioder(
            inntektsperioder,
            medlemskapsperioder
        )
    }

    @Test
    fun `SkatteforholdTilNorge er uten opphold og starter slutter på samme dato som medlemskapsperioder - ok`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28")),
            lagMedlemskapsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        val skatteforholdTilNorge: List<SkatteforholdTilNorge> = listOf(
            lagSkatteforholdTilNorge(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-15")),
            lagSkatteforholdTilNorge(LocalDate.parse("2023-01-16"), LocalDate.parse("2023-02-28")),
            lagSkatteforholdTilNorge(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        shouldNotThrowAny {
            TrygdeavgiftsberegningService.validerSkatteforholdTilNorgeDekkerMedlemskapsperioderOgOverlapperIkke(
                skatteforholdTilNorge,
                medlemskapsperioder
            )
        }
    }

    @Test
    fun `SkatteforholdTilNorge er uten opphold og slutter ikke på samme dato som medlemskapsperioder - false`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28")),
            lagMedlemskapsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        val skatteforholdTilNorge: List<SkatteforholdTilNorge> = listOf(
            lagSkatteforholdTilNorge(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-15")),
            lagSkatteforholdTilNorge(LocalDate.parse("2023-01-16"), LocalDate.parse("2023-02-28")),
            lagSkatteforholdTilNorge(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-05"))
        )

        shouldThrow<FunksjonellException> {
            TrygdeavgiftsberegningService.validerSkatteforholdTilNorgeDekkerMedlemskapsperioderOgOverlapperIkke(
                skatteforholdTilNorge,
                medlemskapsperioder
            )
        }.message.shouldContain("Skatteforholdsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun `SkatteforholdTilNorge er uten opphold og starter ikke på samme dato som medlemskapsperioder - false`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28")),
            lagMedlemskapsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        val skatteforholdTilNorge: List<SkatteforholdTilNorge> = listOf(
            lagSkatteforholdTilNorge(LocalDate.parse("2023-01-03"), LocalDate.parse("2023-01-15")),
            lagSkatteforholdTilNorge(LocalDate.parse("2023-01-16"), LocalDate.parse("2023-02-28")),
            lagSkatteforholdTilNorge(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        shouldThrow<FunksjonellException> {
            TrygdeavgiftsberegningService.validerSkatteforholdTilNorgeDekkerMedlemskapsperioderOgOverlapperIkke(
                skatteforholdTilNorge,
                medlemskapsperioder
            )
        }.message.shouldContain("Skatteforholdsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun `SkatteforholdTilNorge har et opphold - false`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28")),
            lagMedlemskapsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        val skatteforholdTilNorge: List<SkatteforholdTilNorge> = listOf(
            lagSkatteforholdTilNorge(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-15")),
            lagSkatteforholdTilNorge(LocalDate.parse("2023-01-16"), LocalDate.parse("2023-02-20")),
            lagSkatteforholdTilNorge(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        shouldThrow<FunksjonellException> {
            TrygdeavgiftsberegningService.validerSkatteforholdTilNorgeDekkerMedlemskapsperioderOgOverlapperIkke(
                skatteforholdTilNorge,
                medlemskapsperioder
            )
        }.message.shouldContain("Skatteforholdsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun `SkatteforholdTilNorge har to perioder som overlapper med 1 dag - false`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-05-31"))
        )

        val skatteforholdTilNorge: List<SkatteforholdTilNorge> = listOf(
            lagSkatteforholdTilNorge(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-02-28")),
            lagSkatteforholdTilNorge(LocalDate.parse("2023-02-28"), LocalDate.parse("2023-05-31")),
        )

        shouldThrow<FunksjonellException> {
            TrygdeavgiftsberegningService.validerSkatteforholdTilNorgeDekkerMedlemskapsperioderOgOverlapperIkke(
                skatteforholdTilNorge,
                medlemskapsperioder
            )
        }.message.shouldContain("Skatteforholdsperiodene kan ikke overlappe")
    }

    private fun lagMedlemskapsperiode(fom: LocalDate, tom: LocalDate): Medlemskapsperiode {
        return Medlemskapsperiode().apply {
            this.fom = fom
            this.tom = tom
        }
    }

    private fun lagInntektsperiode(fom: LocalDate, tom: LocalDate): Inntektsperiode {
        return Inntektsperiode().apply {
            this.fomDato = fom
            this.tomDato = tom
        }
    }

    private fun lagSkatteforholdTilNorge(fom: LocalDate, tom: LocalDate): SkatteforholdTilNorge {
        return SkatteforholdTilNorge().apply {
            this.fomDato = fom
            this.tomDato = tom
        }
    }
}
