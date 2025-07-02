package no.nav.melosys.itest.vedtak.satsendring

import TrygdeavgiftsberegningMedSatsendring
import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.integrasjon.faktureringskomponenten.NyFakturaserieResponseDto
import no.nav.melosys.itest.JournalfoeringBase
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.ftrl.medlemskapsperiode.OpprettForslagMedlemskapsperiodeService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.vedtak.FattVedtakRequest
import no.nav.melosys.service.vedtak.VedtaksfattingFasade
import no.nav.melosys.service.vilkaar.VilkaarDto
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import no.nav.melosys.tjenester.gui.config.ApiKeyInterceptor
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import wiremock.com.google.common.net.HttpHeaders
import java.time.LocalDate

@AutoConfigureMockMvc
class SatsendringAdminControllerIT(
    @Autowired private val mockMvc: MockMvc,
    @Autowired var mockOAuth2Server: MockOAuth2Server,
    @Autowired private val avklartefaktaService: AvklartefaktaService,
    @Autowired private val mottatteOpplysningerService: MottatteOpplysningerService,
    @Autowired private val opprettForslagMedlemskapsperiodeService: OpprettForslagMedlemskapsperiodeService,
    @Autowired private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
    @Autowired private val vedtaksfattingFasade: VedtaksfattingFasade,
    @Autowired private val vilkaarsresultatService: VilkaarsresultatService
) : JournalfoeringBase(TrygdeavgiftsberegningMedSatsendring()) {

    private val testYear = 2024
    private var originalSubjectHandler: SubjectHandler? = null

    private fun hentBearerToken(): String {
        return mockOAuth2Server.issueToken(
            issuerId = "issuer1",
            subject = "testbruker",
            audience = "dumbdumb",
            claims = mapOf(
                "oid" to "test-oid",
                "azp" to "test-azp",
                "NAVident" to "test123"
            )
        ).serialize()
    }

    @BeforeEach
    fun setup() {
        originalSubjectHandler = SubjectHandler.getInstance()

        val mockHandler = mockk<SpringSubjectHandler>()
        SubjectHandler.set(mockHandler)
        every { mockHandler.userID } returns "Z123456"
        every { mockHandler.userName } returns "test"

        mockServer.stubFor(
            WireMock.post("/api/v2/beregn")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withTransformers("trygdeavgiftsberegning-med-satsendring-transformer")
                )
        )

        mockServer.stubFor(
            WireMock.post("/fakturaserier")
                .withRequestBody(WireMock.matchingJsonPath("$.fakturaserieReferanse", WireMock.absent()))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(NyFakturaserieResponseDto("fakturaserieReferanse-1").toJsonNode.toString())
                )
        )

        mockServer.stubFor(
            WireMock.post("/fakturaserier")
                .withRequestBody(
                    WireMock.matchingJsonPath(
                        "$.fakturaserieReferanse",
                        WireMock.equalTo("fakturaserieReferanse-1")
                    )
                )
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(NyFakturaserieResponseDto("fakturaserieReferanse-2").toJsonNode.toString())
                )
        )
    }

    @AfterEach
    fun afterEach() {
        SubjectHandler.set(originalSubjectHandler)
    }

    @Test
    fun `satsendring job skal kjøre og håndtere behandlinger som trenger satsendring`() {
        // Opprett behandlinger som blir påvirket av satsendringer
        val behandlingMedSatsendring = lagFørstegangsbehandlingMedSatsendring()
        val behandlingUtenSatsendring = lagFørstegangsbehandlingUtenSatsendring()


        // Trigger satsendring-jobben via admin-endepunkt
        // executeAndWait verifiser at prosesser opprettes og lykkes
        executeAndWait(
            waitForProsesses = mapOf(ProsessType.SATSENDRING to 1)
        ) {
            // Lagre gjeldende prosessID fra testklasse før den slettes etter forespørselen fra controlleren.
            val processID = ThreadLocalAccessInfo.getProcessId()

            mockMvc.perform(
                MockMvcRequestBuilders.post("/admin/satsendringer/${testYear}")
                    .header(ApiKeyInterceptor.Companion.API_KEY_HEADER, "dummy")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken()}")
            ).andExpect(MockMvcResultMatchers.status().isAccepted)

            // Gjenopprett ThreadLocal-konteksten med samme UUID etter controller-forespørselen
            ThreadLocalAccessInfo.beforeExecuteProcess(processID, "steg")
        }
    }

    private fun lagFørstegangsbehandlingMedSatsendring(): Behandling {
        // Opprett en periode som vil bli påvirket av satsendring (April 2024)
        // Dette matcher den eksakte perioden i SatsendringIT som utløser satsendring
        val medlemskapsperiode = Periode(LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30))
        return lagFørstegangsbehandling(medlemskapsperiode)
    }

    private fun lagFørstegangsbehandlingUtenSatsendring(): Behandling {
        // Opprett en periode som IKKE vil bli påvirket av satsendring (Q1 2024)
        val medlemskapsperiode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31))
        return lagFørstegangsbehandling(medlemskapsperiode)
    }

    private fun lagFørstegangsbehandling(medlemskapsperiode: Periode): Behandling {
        val behandling = journalførOgVentTilProsesserErFerdige(
            defaultJournalføringDto().apply {
                fagsak.sakstype = Sakstyper.FTRL.name
                fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.name
                behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
                behandlingstemaKode = Behandlingstema.YRKESAKTIV.name
                mottattDato = medlemskapsperiode.fom?.minusDays(7)
            },
            mapOf(
                ProsessType.JFR_NY_SAK_BRUKER to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        ).behandling.shouldNotBeNull()

        val mottatteOpplysninger =
            mottatteOpplysningerService.hentEllerOpprettMottatteOpplysninger(behandling.id, true)
                .shouldNotBeNull()
                .apply {
                    mottatteOpplysningerData
                        .shouldBeInstanceOf<SøknadNorgeEllerUtenforEØS>()
                        .apply {
                            periode = medlemskapsperiode
                            soeknadsland = Soeknadsland(listOf("AF"), false)
                            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON
                        }
                }
        mottatteOpplysningerService.oppdaterMottatteOpplysninger(
            behandling.id,
            mottatteOpplysninger.mottatteOpplysningerData.toJsonNode
        )

        val virksomhet = AvklartefaktaDto(
            listOf("TRUE"), "VIRKSOMHET"
        ).apply {
            avklartefaktaType = Avklartefaktatyper.VIRKSOMHET
            subjektID = "999999999"
            begrunnelseKoder = emptyList()
            begrunnelseFritekst = null
        }
        avklartefaktaService.lagreAvklarteFakta(behandling.id, setOf(virksomhet))

        val vilkår = listOf(
            VilkaarDto().apply {
                vilkaar = Vilkaar.FTRL_2_1A_TRYGDEKOORDINGERING.kode
                isOppfylt = true
            },
            VilkaarDto().apply {
                vilkaar = Vilkaar.FTRL_FORUTGÅENDE_TRYGDETID.kode
                isOppfylt = true
            },
            VilkaarDto().apply {
                vilkaar = Vilkaar.FTRL_2_8_FØRSTE_LEDD_NÆR_TILKNYTNING_NORGE.kode
                isOppfylt = true
            }
        )
        vilkaarsresultatService.registrerVilkår(behandling.id, vilkår)

        setupTrygdeavgift(behandling.id, medlemskapsperiode)

        val vedtakRequest = FattVedtakRequest.Builder()
            .medBehandlingsresultatType(Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medBestillersId("component test")
            .build()

        executeAndWait(
            mapOf(
                ProsessType.IVERKSETT_VEDTAK_FTRL to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        ) {
            vedtaksfattingFasade.fattVedtak(behandling.id, vedtakRequest)
        }

        return behandling
    }

    private fun setupTrygdeavgift(behandlingID: Long, periode: Periode) {
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
            inntektsforholdsperioder
        )
    }
}
