package no.nav.melosys.itest.vedtak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import io.getunleash.FakeUnleash
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.integrasjon.faktureringskomponenten.NyFakturaserieResponseDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import no.nav.melosys.itest.JournalfoeringBase
import no.nav.melosys.itest.OAuthMockServer
import no.nav.melosys.melosysmock.medl.MedlRepo
import no.nav.melosys.melosysmock.testdata.TestDataGenerator
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.MedlemAvFolketrygdenService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService
import no.nav.melosys.service.avgift.dto.InntektskildeRequest
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest
import no.nav.melosys.service.avgift.dto.SkatteforholdTilNorgeRequest
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService
import no.nav.melosys.service.medlemskapsperiode.OpprettMedlemskapsperiodeService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.OpprettBehandlingForSak
import no.nav.melosys.service.sak.OpprettSakDto
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import no.nav.melosys.service.vedtak.FattVedtakRequest
import no.nav.melosys.service.vedtak.VedtaksfattingFasade
import no.nav.melosys.service.vilkaar.VilkaarDto
import no.nav.melosys.service.vilkaar.VilkaarsresultatService
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.LocalDate
import java.util.*

@Import(OAuthMockServer::class)
class YrkesaktivFtrlVedtakIT(
    @Autowired testDataGenerator: TestDataGenerator,
    @Autowired journalføringService: JournalfoeringService,
    @Autowired oppgaveService: OppgaveService,
    @Autowired prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val avklartefaktaService: AvklartefaktaService,
    @Autowired private val behandlingsresultatService: BehandlingsresultatService,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val oAuthMockServer: OAuthMockServer,
    @Autowired private val mottatteOpplysningerService: MottatteOpplysningerService,
    @Autowired private val vilkaarsresultatService: VilkaarsresultatService,
    @Autowired private val medlemskapsperiodeService: MedlemskapsperiodeService,
    @Autowired private val opprettMedlemskapsperiodeService: OpprettMedlemskapsperiodeService,
    @Autowired private val medlemAvFolketrygdenService: MedlemAvFolketrygdenService,
    @Autowired private val oppfriskSaksopplysningerService: OppfriskSaksopplysningerService,
    @Autowired private val vedtaksfattingFasade: VedtaksfattingFasade,
    @Autowired private val unleash: FakeUnleash,
    @Autowired private val opprettBehandlingForSak: OpprettBehandlingForSak,
    @Autowired private val trygdeavgiftsgrunnlagService: TrygdeavgiftsgrunnlagService,
    @Autowired private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
) : JournalfoeringBase(testDataGenerator, journalføringService, oppgaveService, prosessinstansRepository) {

    val uuid1 = UUID.randomUUID()
    val uuid2 = UUID.randomUUID()
    val uuid3 = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        oAuthMockServer.start()
        unleash.enableAll()
        MedlRepo.repo.clear()
        mockServer.stubFor(
            WireMock.post("/api/v1/mal/innvilgelse_ftrl/lag-pdf?somKopi=false&utkast=false").willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(ByteArray(0))
            )
        )

        val expectedResponse = listOf(
            TrygdeavgiftsberegningResponse(
                TrygdeavgiftsperiodeDto(
                    DatoPeriodeDto(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 2, 1)),
                    6.8.toBigDecimal(), PengerDto(1000.toBigDecimal() , NOK)
                ),
                TrygdeavgiftsgrunnlagDto(uuid1, uuid2, uuid3)
            )
        )

        mockServer.stubFor(
            WireMock.post("/api/v2/beregn")
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(expectedResponse.toJsonNode.toString())
                )
        )

        val fakturaResponse = NyFakturaserieResponseDto("test")

        mockServer.stubFor(
            WireMock.post("/fakturaserier")
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(fakturaResponse.toJsonNode.toString())
                )
        )
        mockServer.stubFor(
            WireMock.delete("/fakturaserier/test")
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(fakturaResponse.toJsonNode.toString())
                )
        )
    }

    @AfterEach
    fun afterEach() {
        oAuthMockServer.stop()
        MedlRepo.repo.clear()
    }

    @Test
    fun `yrkesaktiv vedtak - FTRL - test`() {
        val saksbehandler = "Z123456"
        val subjectHandler: SubjectHandler = Mockito.mock(SpringSubjectHandler::class.java)
        SubjectHandler.set(subjectHandler)
        Mockito.`when`(subjectHandler.getUserID()).thenReturn(saksbehandler)

        val saksnummer = lagFørstegangsBehandling()

        val behandlingsId = executeAndWait(waitForprosessType = ProsessType.OPPRETT_REPLIKERT_BEHANDLING_FOR_SAK) {
            opprettBehandlingForSak.opprettBehandling(
                saksnummer,
                lagOpprettSakDto()
            )
        }.behandling.id

        trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
            behandlingsId,
            OppdaterTrygdeavgiftsgrunnlagRequest(
                skatteforholdTilNorgeList = listOf(
                    SkatteforholdTilNorgeRequest(
                        fomDato = LocalDate.of(2023, 1, 1),
                        tomDato = LocalDate.of(2023, 2, 1),
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    )
                ),
                inntektskilder = listOf(
                    InntektskildeRequest(
                        type = Inntektskildetype.INNTEKT_FRA_UTLANDET,
                        arbeidsgiversavgiftBetales = true,
                        avgiftspliktigInntektMnd = 10000.toBigDecimal(),
                        fomDato = LocalDate.of(2023, 1, 1),
                        tomDato = LocalDate.of(2023, 2, 1)
                    )
                )
            )
        )

        val vedtakRequest = FattVedtakRequest.Builder()
            .medBehandlingsresultatType(Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)
            .medVedtakstype(Vedtakstyper.ENDRINGSVEDTAK)
            .medBestillersId("komponent test")
            .build()

        executeAndWait(
            waitForprosessType = ProsessType.IVERKSETT_VEDTAK_FTRL
        ) {
            vedtaksfattingFasade.fattVedtak(behandlingsId, vedtakRequest)
        }
    }

    private fun lagOpprettSakDto(): OpprettSakDto {
        val opprettsakdto = OpprettSakDto()
        opprettsakdto.behandlingstema = Behandlingstema.YRKESAKTIV
        opprettsakdto.behandlingstype = Behandlingstyper.NY_VURDERING
        opprettsakdto.mottaksdato = LocalDate.now()
        opprettsakdto.behandlingsaarsakType = Behandlingsaarsaktyper.SØKNAD
        return opprettsakdto
    }

    fun lagFørstegangsBehandling(): String {
        val behandling = journalførOgVentTilProsesserErFerdige(
            defaultJournalføringDto().apply {
                fagsak.sakstype = Sakstyper.FTRL.name
                fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.name
                behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
                behandlingstemaKode = Behandlingstema.YRKESAKTIV.name
            },
            waitFor = ProsessType.JFR_NY_SAK_BRUKER,
            alsoWaitForprosessType = listOf(ProsessType.OPPRETT_OG_DISTRIBUER_BREV)
        ).behandling.shouldNotBeNull()

        val mottatteOpplysninger =
            mottatteOpplysningerService.hentEllerOpprettMottatteOpplysninger(behandling.id, true)
                .shouldNotBeNull()
                .apply {
                    type shouldBe Mottatteopplysningertyper.SØKNAD_YRKESAKTIVE_NORGE_ELLER_UTENFOR_EØS
                    mottatteOpplysningerData
                        .shouldBeInstanceOf<SøknadNorgeEllerUtenforEØS>()
                        .apply {
                            periode = Periode(
                                LocalDate.of(2023, 1, 1),
                                LocalDate.of(2023, 2, 1),
                            )
                            soeknadsland = Soeknadsland(listOf("AF"), false)
                            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
                        }
                }
        mottatteOpplysningerService.oppdaterMottatteOpplysninger(behandling.id, mottatteOpplysninger.mottatteOpplysningerData.toJsonNode)
        oppfriskSaksopplysningerService.oppfriskSaksopplysning(behandling.id, false)

        val yrkesgruppe = AvklartefaktaDto(
            listOf("ORDINAER"), "YRKESGRUPPE"
        ).apply {
            avklartefaktaType = Avklartefaktatyper.YRKESGRUPPE
            subjektID = null
            begrunnelseKoder = emptyList()
            begrunnelseFritekst = null
        }
        val virksomhet = AvklartefaktaDto(
            listOf("TRUE"), "VIRKSOMHET"
        ).apply {
            avklartefaktaType = Avklartefaktatyper.VIRKSOMHET
            subjektID = "999999999"
            begrunnelseKoder = emptyList()
            begrunnelseFritekst = null
        }
        val yrkesaktivitet = AvklartefaktaDto(
            listOf("TRUE"), "ARBEIDSLAND"
        ).apply {
            avklartefaktaType = Avklartefaktatyper.ARBEIDSLAND
            subjektID = "AF"
            begrunnelseKoder = emptyList()
            begrunnelseFritekst = null
        }
        avklartefaktaService.lagreAvklarteFakta(behandling.id, setOf(yrkesgruppe, virksomhet, yrkesaktivitet))

        medlemAvFolketrygdenService.lagreBestemmelse(behandling.id, Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A)

        val vilkaar = VilkaarDto().apply {
            vilkaar = "FTRL_2_8_FORUTGÅENDE_TRYGDETID"
            isOppfylt = true
        }
        vilkaarsresultatService.registrerVilkår(behandling.id, listOf(vilkaar))

        simulerTrygdeavgiftBeregning(behandling.id);

        val vedtakRequest = FattVedtakRequest.Builder()
            .medBehandlingsresultatType(Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medBestillersId("komponent test")
            .build()

        executeAndWait(
            waitForprosessType = ProsessType.IVERKSETT_VEDTAK_FTRL,
            alsoWaitForprosessType = listOf(ProsessType.OPPRETT_OG_DISTRIBUER_BREV)
        ) {
            vedtaksfattingFasade.fattVedtak(behandling.id, vedtakRequest)
        }
        return behandling.fagsak.saksnummer
    }

    //Simulerer steget før vedtak
    private fun simulerTrygdeavgiftBeregning(behandlingId: Long) {
        val medlemskapsperiodeId = opprettMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(
            behandlingId
        ).single().id

        val medlemskapsperiode = medlemskapsperiodeService.oppdaterMedlemskapsperiode(
            behandlingId,
            medlemskapsperiodeId,
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2023, 2, 1),
            InnvilgelsesResultat.INNVILGET,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
        )

        trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
            behandlingId,
            OppdaterTrygdeavgiftsgrunnlagRequest(
                skatteforholdTilNorgeList = listOf(
                    SkatteforholdTilNorgeRequest(
                        fomDato = LocalDate.of(2023, 1, 1),
                        tomDato = LocalDate.of(2023, 2, 1),
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    )
                ),
                inntektskilder = listOf(
                    InntektskildeRequest(
                        type = Inntektskildetype.INNTEKT_FRA_UTLANDET,
                        arbeidsgiversavgiftBetales = false,
                        avgiftspliktigInntektMnd = 10000.toBigDecimal(),
                        fomDato = LocalDate.of(2023, 1, 1),
                        tomDato = LocalDate.of(2023, 2, 1)
                    )
                )
            )
        )

        val medlemAvFolketrygden = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingId)

        val trygdeavgiftsperioder = HashSet<Trygdeavgiftsperiode>()
        trygdeavgiftsperioder.add(
            Trygdeavgiftsperiode().apply {
                periodeFra = LocalDate.of(2023, 1, 1)
                periodeTil = LocalDate.of(2023, 2, 1)
                trygdesats = 6.8.toBigDecimal()
                trygdeavgiftsbeløpMd = Penger(1000.toBigDecimal(), "nok")
                fastsattTrygdeavgift = medlemAvFolketrygden.fastsattTrygdeavgift
                grunnlagMedlemskapsperiode = medlemskapsperiode
                grunnlagSkatteforholdTilNorge = medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag.skatteforholdTilNorge.first()
                grunnlagInntekstperiode = medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag.inntektsperioder.first()
            }
        )

        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder = trygdeavgiftsperioder
        //Simulerer TrygdeavgiftBergeginingservice.beregnOgLagreTrygdeavgift()
        medlemAvFolketrygdenService.lagre(medlemAvFolketrygden)
    }

    private val Any.toJsonNode: JsonNode
        get() {
            return jacksonObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(JavaTimeModule())
                .valueToTree(this)
        }
}
