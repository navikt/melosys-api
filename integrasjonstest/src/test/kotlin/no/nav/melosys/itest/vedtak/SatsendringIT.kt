package no.nav.melosys.itest.vedtak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.*
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.integrasjon.faktureringskomponenten.NyFakturaserieResponseDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import no.nav.melosys.itest.JournalfoeringBase
import no.nav.melosys.itest.MelosysHendelseKafkaConsumer
import no.nav.melosys.melosysmock.medl.MedlRepo
import no.nav.melosys.melosysmock.testdata.JournalføringsoppgaveGenerator
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.avgift.satsendring.SatsendringFinner
import no.nav.melosys.service.avgift.satsendring.SatsendringFinner.BehandlingForSatstendring
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.ftrl.medlemskapsperiode.OpprettForslagMedlemskapsperiodeService
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.vedtak.FattVedtakRequest
import no.nav.melosys.service.vedtak.VedtaksfattingFasade
import no.nav.melosys.service.vilkaar.VilkaarDto
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

private val logger = KotlinLogging.logger {}

class SatsendringIT(
    @Autowired journalføringsoppgaveGenerator: JournalføringsoppgaveGenerator,
    @Autowired journalføringService: JournalfoeringService,
    @Autowired oppgaveService: OppgaveService,
    @Autowired private val avklartefaktaService: AvklartefaktaService,
    @Autowired private val mottatteOpplysningerService: MottatteOpplysningerService,
    @Autowired private val opprettForslagMedlemskapsperiodeService: OpprettForslagMedlemskapsperiodeService,
    @Autowired private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
    @Autowired private val vedtaksfattingFasade: VedtaksfattingFasade,
    @Autowired private val vilkaarsresultatService: VilkaarsresultatService,
    @Autowired private val satsendringFinner: SatsendringFinner,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val unleash: FakeUnleash,
    @Autowired private val melosysHendelseKafkaConsumer: MelosysHendelseKafkaConsumer,
    @Autowired private val prosessinstansService: ProsessinstansService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val behandlingsresultatService: BehandlingsresultatService
) : JournalfoeringBase(
    journalføringsoppgaveGenerator, journalføringService, oppgaveService,
    TrygdeavgiftsberegningMedSatsendring()
) {
    private var originalSubjectHandler: SubjectHandler? = null

    @BeforeEach
    fun setup() {
        MedlRepo.repo.clear()
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

        val fakturaResponse = NyFakturaserieResponseDto("fakturaserieReferanse")

        mockServer.stubFor(
            WireMock.post("/fakturaserier")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(fakturaResponse.toJsonNode.toString())
                )
        )

        unleash.enableAll()
    }

    @AfterEach
    fun afterEach() {
        SubjectHandler.set(originalSubjectHandler)
        MedlRepo.repo.clear()
        melosysHendelseKafkaConsumer.clear()
    }

    @Test
    fun `Satsendring etter yrkesaktiv FTRL vedtak oppdages`() {
        // Lag 1 behandling utenfor SATSENDRING_ÅR
        lagFørstegangsbehandling(år = SATSENDRING_ÅR - 1)
        // Lag 2 behandlinger for SATSENDRING_ÅR, en med satsendring og en uten
        val behandlingMedSatsendring = lagFørstegangsbehandling(harSatsendringEtterÅrsskiftet = true)
        val behandlingUtenSatsendring = lagFørstegangsbehandling(harSatsendringEtterÅrsskiftet = false)


        val avgiftSatsendringInfo = satsendringFinner.finnBehandlingerMedSatsendring(SATSENDRING_ÅR)


        avgiftSatsendringInfo.run {
            år shouldBe SATSENDRING_ÅR
            behandlingerMedSatsendring.shouldContainOnly(
                BehandlingForSatstendring(
                    behandlingMedSatsendring.id,
                    behandlingMedSatsendring.fagsak.saksnummer,
                    Behandlingstyper.FØRSTEGANG,
                    true
                )
            )
            behandlingerUtenSatsendring.shouldContainOnly(
                BehandlingForSatstendring(
                    behandlingUtenSatsendring.id,
                    behandlingUtenSatsendring.fagsak.saksnummer,
                    Behandlingstyper.FØRSTEGANG,
                    false
                )
            )
        }
    }

    @Test
    fun `oppretter prosess og påfølgende satsendringbehandling som iverksettes og sender faktura`() {
        val førstegangsbehandling = lagFørstegangsbehandling(harSatsendringEtterÅrsskiftet = true)


        val satsendringID = executeAndWait(
            mapOf(
                ProsessType.BEHANDLE_SATSENDRING to 1
            )
        ) {
            prosessinstansService.opprettSatsendringBehandling(førstegangsbehandling)

        }.behandling.id


        val satsendring = behandlingService.hentBehandling(satsendringID)
        val førstegangsbehandlingRefresh = behandlingService.hentBehandling(førstegangsbehandling.id)
        val satsendingBehandlingresultat = behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(satsendringID)
        val førstegangsBehandlingsresultat = behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(førstegangsbehandling.id)

        satsendring.run {
            status shouldBe Behandlingsstatus.AVSLUTTET
            type shouldBe Behandlingstyper.SATSENDRING
            tema shouldBe Behandlingstema.YRKESAKTIV
            behandlingsårsak.type shouldBe Behandlingsaarsaktyper.ÅRLIG_SATSOPPDATERING
            oppgaveId shouldBe null
            fagsak.saksnummer shouldBe førstegangsbehandling.fagsak.saksnummer
            fagsak.status shouldBe førstegangsbehandlingRefresh.fagsak.status shouldBe Saksstatuser.LOVVALG_AVKLART
        }

        satsendingBehandlingresultat.run {
            type shouldBe Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            vedtakMetadata.vedtakstype shouldBe Vedtakstyper.ENDRINGSVEDTAK
            trygdeavgiftsperioder.run {
                shouldHaveSize(1)
                first().run {
                    periodeFra.shouldNotBeNull() shouldBe førstegangsBehandlingsresultat.trygdeavgiftsperioder.first().periodeFra
                    periodeTil.shouldNotBeNull() shouldBe førstegangsBehandlingsresultat.trygdeavgiftsperioder.first().periodeTil
                    trygdesats shouldBe 6.9.toBigDecimal()
                    trygdeavgiftsbeløpMd shouldBe Penger(69000.toBigDecimal())
                }
            }

            medlemskapsperioder.run {
                shouldHaveSize(1)
                first().run {
                    fom.shouldNotBeNull() shouldBe førstegangsBehandlingsresultat.medlemskapsperioder.first().fom
                    tom.shouldNotBeNull() shouldBe førstegangsBehandlingsresultat.medlemskapsperioder.first().tom
                    innvilgelsesresultat shouldBe førstegangsBehandlingsresultat.medlemskapsperioder.first().innvilgelsesresultat
                    medlemskapstype shouldBe førstegangsBehandlingsresultat.medlemskapsperioder.first().medlemskapstype
                    trygdedekning shouldBe førstegangsBehandlingsresultat.medlemskapsperioder.first().trygdedekning
                    medlPeriodeID shouldBe førstegangsBehandlingsresultat.medlemskapsperioder.first().medlPeriodeID
                    bestemmelse shouldBe førstegangsBehandlingsresultat.medlemskapsperioder.first().bestemmelse
                }
            }

            hentInntektsperioder().run {
                shouldHaveSize(1)
                first().run {
                    fom.shouldNotBeNull() shouldBe førstegangsBehandlingsresultat.hentInntektsperioder().first().fom
                    tom.shouldNotBeNull() shouldBe førstegangsBehandlingsresultat.hentInntektsperioder().first().tom
                    type shouldBe førstegangsBehandlingsresultat.hentInntektsperioder().first().type
                    avgiftspliktigMndInntekt shouldBe førstegangsBehandlingsresultat.hentInntektsperioder().first().avgiftspliktigMndInntekt
                    avgiftspliktigTotalinntekt shouldBe førstegangsBehandlingsresultat.hentInntektsperioder().first().avgiftspliktigTotalinntekt
                }
            }

            hentSkatteforholdTilNorge().run {
                shouldHaveSize(1)
                first().run {
                    fomDato.shouldNotBeNull() shouldBe førstegangsBehandlingsresultat.hentSkatteforholdTilNorge().first().fomDato
                    tomDato.shouldNotBeNull() shouldBe førstegangsBehandlingsresultat.hentSkatteforholdTilNorge().first().tomDato
                    skatteplikttype shouldBe førstegangsBehandlingsresultat.hentSkatteforholdTilNorge().first().skatteplikttype
                }
            }
        }


        val jsonDato = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val fakturaserieRequestJson = """
            {
                     "fodselsnummer" : "30056928150",
                     "fakturaserieReferanse" : "fakturaserieReferanse",
                     "fullmektig" : {
                       "fodselsnummer" : null,
                       "organisasjonsnummer" : null
                     },
                     "referanseBruker" : "Vedtak om satsendring datert $jsonDato",
                     "referanseNAV" : "Medlemskap og avgift",
                     "fakturaGjelderInnbetalingstype" : "TRYGDEAVGIFT",
                     "intervall" : "KVARTAL",
                     "perioder" : [ {
                       "enhetsprisPerManed" : 69000.0,
                       "startDato" : "2024-04-01",
                       "sluttDato" : "2024-04-30",
                       "beskrivelse" : "Faktura for årlige satsoppdateringen på trygdeavgift, Inntekt: 10000, Dekning: Pensjonsdel (§ 2-9), Sats: 6.9 %"
                     } ]
                    }
                    """.trimIndent()

        mockServer.verify(
            postRequestedFor(urlEqualTo("/fakturaserier"))
                .withRequestBody(equalToJson(fakturaserieRequestJson, true, true))
        )
    }


    fun lagFørstegangsbehandling(år: Int = SATSENDRING_ÅR, harSatsendringEtterÅrsskiftet: Boolean = false): Behandling {
        // Perioden brukes for å avgjøre om det blir satsendring
        val medlemskapsperiode = lagPeriode(år, harSatsendringEtterÅrsskiftet)

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
        mottatteOpplysningerService.oppdaterMottatteOpplysninger(behandling.id, mottatteOpplysninger.mottatteOpplysningerData.toJsonNode)

        val virksomhet = AvklartefaktaDto(
            listOf("TRUE"), "VIRKSOMHET"
        ).apply {
            avklartefaktaType = Avklartefaktatyper.VIRKSOMHET
            subjektID = "999999999"
            begrunnelseKoder = emptyList()
            begrunnelseFritekst = null
        }
        avklartefaktaService.lagreAvklarteFakta(behandling.id, setOf(virksomhet))

        val vilkår = listOf(VilkaarDto().apply {
            vilkaar = Vilkaar.FTRL_2_1A_TRYGDEKOORDINGERING.kode
            isOppfylt = true
        }, VilkaarDto().apply {
            vilkaar = Vilkaar.FTRL_FORUTGÅENDE_TRYGDETID.kode
            isOppfylt = true
        }, VilkaarDto().apply {
            vilkaar = Vilkaar.FTRL_2_8_FØRSTE_LEDD_NÆR_TILKNYTNING_NORGE.kode
            isOppfylt = true
        })
        vilkaarsresultatService.registrerVilkår(behandling.id, vilkår)

        setupTrygdeavgift(behandling.id, medlemskapsperiode)

        val vedtakRequest = FattVedtakRequest.Builder()
            .medBehandlingsresultatType(Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medBestillersId("komponent test")
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

    private fun lagPeriode(
        år: Int = SATSENDRING_ÅR,
        harSatsendringEtterÅrsskiftet: Boolean
    ): Periode {
        if (harSatsendringEtterÅrsskiftet) {
            return Periode(LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30))
        }
        val startDato = LocalDate.of(år, 1, 1)
        val sluttDato = LocalDate.of(år, 3, 31)
        return Periode(startDato, sluttDato)
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

        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(behandlingID, skattefordholdsperioder, inntektsforholdsperioder)
    }

    private val Any.toJsonNode: JsonNode
        get() = objectMapper.valueToTree(this)

    companion object {
        private const val SATSENDRING_ÅR = 2024
    }
}

class TrygdeavgiftsberegningMedSatsendring : ResponseTransformerV2 {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)


    private val kallPerMedlemskapsperiode = mutableMapOf<String, Int>()

    override fun transform(response: Response?, serveEvent: ServeEvent): Response {
        require(serveEvent.request?.url == "/api/v2/beregn") {
            "Invalid url. Denne transformeren støtter kun /api/v2/beregn"
        }

        val requestBody = objectMapper.readTree(serveEvent.request?.bodyAsString)

        val responseBody = createResponseBody(requestBody)
        logger.debug { "Transformed Response Body: $responseBody" }

        return Response.Builder.like(response)
            .body(responseBody)
            .build()
    }

    private fun createResponseBody(requestBody: JsonNode): String {
        val periodeString = medlemskapsperiodeStringFrom(requestBody)
        val antallKall = kallPerMedlemskapsperiode.getOrDefault(periodeString, 0) + 1
        kallPerMedlemskapsperiode[periodeString] = antallKall
        logger.debug { "Kall per medlemskapsperiode: $periodeString -> $antallKall" }

        val skatteforhold = requestBody["skatteforholdsperioder"][0]["skatteforhold"].asText()
        val sats = bestemSats(skatteforhold, periodeString, antallKall)
        val månedsavgift = sats * 10000

        val trygdeavgiftsberegningResponse =
            TrygdeavgiftsberegningResponse(
                TrygdeavgiftsperiodeDto(
                    DatoPeriodeDto(localDateFromRequest("fom", requestBody), localDateFromRequest("tom", requestBody)),
                    sats.toBigDecimal(),
                    PengerDto(månedsavgift.toBigDecimal(), NOK)
                ),
                TrygdeavgiftsgrunnlagDto(
                    UUID.fromString(requestBody["medlemskapsperioder"][0]["id"].asText()),
                    UUID.fromString(requestBody["skatteforholdsperioder"][0]["id"].asText()),
                    UUID.fromString(requestBody["inntektsperioder"][0]["id"].asText())
                )
            )

        return objectMapper.writeValueAsString(listOf(trygdeavgiftsberegningResponse))
    }

    private fun bestemSats(skatteforhold: String?, periodeString: String, antallKall: Int): Double {
        if (skatteforhold == "SKATTEPLIKTIG") return 0.0

        return if (periodeString == "2024-04-01 / 2024-04-30") {
            // Eneste periode med satsendring
            when (antallKall) {
                1 -> 6.7
                else -> 6.9
            }
        } else {
            8.3
        }
    }

    private fun medlemskapsperiodeStringFrom(requestBody: JsonNode): String {
        val fom = localDateFromRequest("fom", requestBody)
        val tom = localDateFromRequest("tom", requestBody)
        return "$fom / $tom"
    }

    private fun localDateFromRequest(datoID: String, requestBody: JsonNode): LocalDate =
        requestBody["medlemskapsperioder"][0]["periode"][datoID]
            .map { it.asInt() }
            .let { (year, month, day) -> LocalDate.of(year, month, day) }

    override fun getName(): String {
        return "trygdeavgiftsberegning-med-satsendring-transformer"
    }

    override fun applyGlobally(): Boolean {
        return false
    }
}
