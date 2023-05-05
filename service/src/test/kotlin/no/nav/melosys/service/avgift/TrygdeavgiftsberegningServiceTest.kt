package no.nav.melosys.service.avgift

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
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.Inntektskildetype
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
            TrygdeavgiftsberegningService(mockMedlemAvFolketrygdenService, mockTrygdeavgiftConsumer)
        medlemAvFolketrygden = MedlemAvFolketrygden()
        every { mockMedlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID) }.returns(medlemAvFolketrygden)
    }

    @Test
    fun hentTrygdeavgiftsberegning_ingenTrygdeavgift_returnerTomListe() {
        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift()
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgift = null


        trygdeavgiftsberegningService.hentTrygdeavgiftsberegning(BEHANDLING_ID)
            .shouldNotBeNull()
            .shouldBeEmpty()
    }

    @Test
    fun beregnTrygdeavgift_skalBetaleTrygeavgift_beregnerOgLagrerTrygdeavgift() {
        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode().apply {
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER
        })
        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                skatteforholdTilNorge = setOf(SkatteforholdTilNorge().apply {
                    fomDato = FOM
                    tomDato = TOM
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                })
                inntektsperioder = setOf(Inntektsperiode().apply {
                    fomDato = FOM
                    tomDato = TOM
                    type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                    isArbeidsgiversavgiftBetalesTilSkatt = false
                    isTrygdeavgiftBetalesTilSkatt = false
                    avgiftspliktigInntektMnd = Penger(10000.0)
                })
            }
        }
        every { mockMedlemAvFolketrygdenService.lagre(any()) }.returns(medlemAvFolketrygden)
        every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftBeregningsgrunnlagDto::class)) }
            .returns(
                listOf(
                    TrygdeavgiftsperiodeDto(
                        DatoPeriodeDto(FOM, TOM),
                        7.9,
                        PengerDto(BigDecimal.valueOf(790), NOK),
                        "1",
                        "1",
                        "1"
                    )
                )
            )


        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID)
            .shouldNotBeNull()
            .shouldNotBeEmpty()
            .forEach {
                it.apply {
                    trygdesats = 7.9
                    trygdeavgiftsbeløpMd = Penger(790.0)
                }
            }
        verify { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftBeregningsgrunnlagDto::class)) }
        verify { mockMedlemAvFolketrygdenService.lagre(medlemAvFolketrygden) }
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgift.shouldNotBeEmpty()
    }

    @Test
    fun beregnTrygdeavgift_skalIkkeBetaleTrygdeavgiftTilNav_sletterEksisterendeTrygdeavgiftOgReturnererTomListe() {
        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode())
        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                skatteforholdTilNorge = setOf(SkatteforholdTilNorge())
                inntektsperioder = setOf(Inntektsperiode().apply {
                    isTrygdeavgiftBetalesTilSkatt = true
                })
            }
            trygdeavgift.add(Trygdeavgiftsperiode())
        }


        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgift.shouldNotBeEmpty()
        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID)
            .shouldNotBeNull()
            .shouldBeEmpty()
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgift.shouldBeEmpty()
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
}
