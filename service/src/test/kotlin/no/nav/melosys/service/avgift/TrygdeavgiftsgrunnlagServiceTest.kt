package no.nav.melosys.service.avgift

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.*
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.avgift.dto.InntektskildeRequest
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import kotlin.test.Test

@ExtendWith(MockKExtension::class)
class TrygdeavgiftsgrunnlagServiceTest {

    @MockK
    private lateinit var mockBehandlingsresultatService: BehandlingsresultatService

    private lateinit var trygdeavgiftsgrunnlagService: TrygdeavgiftsgrunnlagService

    private lateinit var behandlingsresultat: Behandlingsresultat
    private val slotBehandlingsresultat = slot<Behandlingsresultat>()
    private val BEHANDLING_ID: Long = 1291


    @BeforeEach
    fun setup() {
        trygdeavgiftsgrunnlagService = TrygdeavgiftsgrunnlagService(mockBehandlingsresultatService)
        behandlingsresultat = Behandlingsresultat()
        every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
    }

    @Test
    fun hentTrygdeavgiftsgrunnlag_ingenGrunnlag_returnerNull() {
        trygdeavgiftsgrunnlagService.hentTrygdeavgiftsgrunnlag(BEHANDLING_ID).shouldBeNull()
    }

    @Test
    fun oppdaterTrygdeavgiftsgrunnlag_ingenMedlemskapsperioder_kasterFeil() {
        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply { medlemskapsperioder = emptyList() }


        shouldThrow<FunksjonellException> {
            trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
                BEHANDLING_ID, OppdaterTrygdeavgiftsgrunnlagRequest(Skatteplikttype.SKATTEPLIKTIG, emptyList())
            )
        }.message.shouldContain("Kan ikke oppdatere trygdeavgiftsgrunnlaget før medlemskapsperioder finnes")
    }

    @Test
    fun oppdaterTrygdeavgiftsgrunnlag_eksisterBeregnetTrygdeavgift_sletterEksisterendeBeregning() {
        every { mockBehandlingsresultatService.lagre(any()) }.returns(Unit)
        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                fom = LocalDate.now().minusMonths(1)
                tom = LocalDate.now().plusMonths(3)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            })
            fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
                trygdeavgiftsperioder = mutableSetOf(Trygdeavgiftsperiode())
            }
        }
        behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.shouldNotBeEmpty()


        trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
            BEHANDLING_ID, OppdaterTrygdeavgiftsgrunnlagRequest(Skatteplikttype.SKATTEPLIKTIG, emptyList())
        )


        verify { mockBehandlingsresultatService.lagre(capture(slotBehandlingsresultat)) }
        slotBehandlingsresultat.captured.shouldNotBeNull()
        slotBehandlingsresultat.captured.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.shouldBeEmpty()
    }

    @Test
    fun oppdaterTrygdeavgiftsgrunnlag_flereMedlemskapsperioder_riktigSkatteforhold() {
        val nå = LocalDate.now()
        every { mockBehandlingsresultatService.lagre(any()) }.returns(Unit)
        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                fom = nå.minusMonths(6)
                tom = nå.minusMonths(1)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            }, Medlemskapsperiode().apply {
                fom = nå.minusMonths(1)
                tom = nå.plusMonths(3)
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            })
        }


        trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
            BEHANDLING_ID, OppdaterTrygdeavgiftsgrunnlagRequest(Skatteplikttype.SKATTEPLIKTIG, emptyList())
        )


        verify { mockBehandlingsresultatService.lagre(capture(slotBehandlingsresultat)) }
        slotBehandlingsresultat.captured.shouldNotBeNull()
        val skatteforholdTilNorge =
            slotBehandlingsresultat.captured.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag.skatteforholdTilNorge
        skatteforholdTilNorge.shouldHaveSize(1)
        skatteforholdTilNorge.first().skatteplikttype.shouldBe(Skatteplikttype.SKATTEPLIKTIG)
        skatteforholdTilNorge.first().fomDato.shouldBe(nå.minusMonths(6))
        skatteforholdTilNorge.first().tomDato.shouldBe(nå.plusMonths(3))
    }

    @Test
    fun oppdaterTrygdeavgiftsgrunnlag_requestMedSkattepliktOgInntektskilder_lagrerAltKorrekt() {
        val fom = LocalDate.now().minusMonths(1)
        val tom = LocalDate.now().plusMonths(3)
        every { mockBehandlingsresultatService.lagre(any()) }.returns(Unit)
        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                this.fom = fom
                this.tom = tom
                this.innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            })
        }


        trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
            BEHANDLING_ID, OppdaterTrygdeavgiftsgrunnlagRequest(
                Skatteplikttype.SKATTEPLIKTIG, listOf(
                    InntektskildeRequest(Inntektskildetype.INNTEKT_FRA_UTLANDET, false, BigDecimal.valueOf(30000)),
                    InntektskildeRequest(Inntektskildetype.NÆRINGSINNTEKT_FRA_NORGE, false, BigDecimal.valueOf(10000)),
                    InntektskildeRequest(Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE, true, null)
                )
            )
        )


        verify(exactly = 1) { mockBehandlingsresultatService.lagre(capture(slotBehandlingsresultat)) }
        val lagretBehandlingsresultat = slotBehandlingsresultat.captured
        lagretBehandlingsresultat.shouldNotBeNull().medlemAvFolketrygden.shouldNotBeNull().fastsattTrygdeavgift.shouldNotBeNull().trygdeavgiftsgrunnlag.shouldNotBeNull()
        lagretBehandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.shouldBeEmpty()
        lagretBehandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag
            .skatteforholdTilNorge.shouldHaveSize(1).first().skatteplikttype.shouldBe(Skatteplikttype.SKATTEPLIKTIG)
        lagretBehandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag
            .inntektsperioder.shouldHaveSize(3).shouldContainExactlyInAnyOrder(
                Inntektsperiode().apply {
                    fomDato = fom
                    tomDato = tom
                    type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                    isArbeidsgiversavgiftBetalesTilSkatt = false
                    isTrygdeavgiftBetalesTilSkatt = true
                    avgiftspliktigInntektMnd = Penger(BigDecimal.valueOf(30000))
                    trygdeavgiftsgrunnlag =
                        lagretBehandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag
                },
                Inntektsperiode().apply {
                    fomDato = fom
                    tomDato = tom
                    type = Inntektskildetype.NÆRINGSINNTEKT_FRA_NORGE
                    isArbeidsgiversavgiftBetalesTilSkatt = false
                    isTrygdeavgiftBetalesTilSkatt = true
                    avgiftspliktigInntektMnd = Penger(BigDecimal.valueOf(10000))
                    trygdeavgiftsgrunnlag =
                        lagretBehandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag
                },
                Inntektsperiode().apply {
                    fomDato = fom
                    tomDato = tom
                    type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                    isArbeidsgiversavgiftBetalesTilSkatt = true
                    isTrygdeavgiftBetalesTilSkatt = true
                    avgiftspliktigInntektMnd = null
                    trygdeavgiftsgrunnlag =
                        lagretBehandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag
                }
            )
    }
}
