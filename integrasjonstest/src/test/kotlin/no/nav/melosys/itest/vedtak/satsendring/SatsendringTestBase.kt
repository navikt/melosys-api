package no.nav.melosys.itest.vedtak.satsendring

import TrygdeavgiftsberegningMedSatsendring
import com.github.tomakehurst.wiremock.client.WireMock.*
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.integrasjon.faktureringskomponenten.NyFakturaserieResponseDto
import no.nav.melosys.itest.JournalfoeringBase
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.ftrl.medlemskapsperiode.OpprettForslagMedlemskapsperiodeService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.vedtak.VedtaksfattingFasade
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate


abstract class SatsendringTestBase(
    protected val avklartefaktaService: AvklartefaktaService,
    protected val mottatteOpplysningerService: MottatteOpplysningerService,
    protected val opprettForslagMedlemskapsperiodeService: OpprettForslagMedlemskapsperiodeService,
    protected val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
    protected val vedtaksfattingFasade: VedtaksfattingFasade,
    protected val vilkaarsresultatService: VilkaarsresultatService
) : JournalfoeringBase(TrygdeavgiftsberegningMedSatsendring()) {

    protected var originalSubjectHandler: SubjectHandler? = null

    @BeforeEach
    fun setupBase() {
        originalSubjectHandler = SubjectHandler.getInstance()

        val mockHandler = mockk<SpringSubjectHandler>()
        SubjectHandler.set(mockHandler)
        every { mockHandler.userID } returns "Z123456"
        every { mockHandler.userName } returns "test"

        setupWireMockStubs()
    }

    @AfterEach
    fun tearDownBase() {
        SubjectHandler.set(originalSubjectHandler)
    }

    protected fun setupWireMockStubs() {
        mockServer.stubFor(
            post("/api/v2/beregn")
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withTransformers("trygdeavgiftsberegning-med-satsendring-transformer")
                )
        )

        mockServer.stubFor(
            post("/fakturaserier")
                .withRequestBody(matchingJsonPath("$.fakturaserieReferanse", absent()))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(NyFakturaserieResponseDto("fakturaserieReferanse-1").toJsonNode.toString())
                )
        )

        mockServer.stubFor(
            post("/fakturaserier")
                .withRequestBody(matchingJsonPath("$.fakturaserieReferanse", equalTo("fakturaserieReferanse-1")))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(NyFakturaserieResponseDto("fakturaserieReferanse-2").toJsonNode.toString())
                )
        )
    }

    protected fun setupTrygdeavgift(behandlingID: Long, periode: Periode) {
        opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(
            behandlingID,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
        )

        val skattefordholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = periode.fom
                tomDato = periode.tom
                this.skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
            }
        )
        val inntektsforholdsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = periode.fom
                tomDato = periode.tom
                this.type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                isArbeidsgiversavgiftBetalesTilSkatt = false
                avgiftspliktigMndInntekt = Penger(10000.toBigDecimal())
                avgiftspliktigTotalinntekt = Penger(10000.toBigDecimal())
            }
        )

        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
            behandlingID,
            skattefordholdsperioder,
            inntektsforholdsperioder,
            LocalDate.of(periode.fom!!.year, 4, 4)
        )
    }
}
