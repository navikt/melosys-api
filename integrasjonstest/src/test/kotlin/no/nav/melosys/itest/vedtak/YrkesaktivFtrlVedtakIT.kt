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
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.itest.JournalfoeringBase
import no.nav.melosys.itest.OAuthMockServer
import no.nav.melosys.melosysmock.journalpost.JournalpostRepo
import no.nav.melosys.melosysmock.medl.MedlRepo
import no.nav.melosys.melosysmock.testdata.TestDataGenerator
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.MedlemAvFolketrygdenService
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService
import no.nav.melosys.service.avgift.dto.InntektskildeRequest
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest
import no.nav.melosys.service.avgift.dto.SkatteforholdTilNorgeRequest
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade
import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService
import no.nav.melosys.service.medlemskapsperiode.OpprettMedlemskapsperiodeService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.OpprettBehandlingForSak
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import no.nav.melosys.service.vedtak.FattVedtakRequest
import no.nav.melosys.service.vedtak.VedtaksfattingFasade
import no.nav.melosys.service.vilkaar.VilkaarDto
import no.nav.melosys.service.vilkaar.VilkaarsresultatService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.LocalDate

@Import(OAuthMockServer::class)
class YrkesaktivFtrlVedtakIT(
    @Autowired testDataGenerator: TestDataGenerator,
    @Autowired journalføringService: JournalfoeringService,
    @Autowired oppgaveService: OppgaveService,
    @Autowired prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val avklartefaktaService: AvklartefaktaService,
    @Autowired private val behandlingsresultatService: BehandlingsresultatService,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val oAuthMockServer: OAuthMockServer,
    @Autowired private val mottatteOpplysningerService: MottatteOpplysningerService,
    @Autowired private val vilkaarsresultatService: VilkaarsresultatService,
    @Autowired private val medlemskapsperiodeService: MedlemskapsperiodeService,
    @Autowired private val opprettMedlemskapsperiodeService: OpprettMedlemskapsperiodeService,
    @Autowired private val medlemAvFolketrygdenService: MedlemAvFolketrygdenService,
    @Autowired private val ferdigbehandlingKontrollFacade: FerdigbehandlingKontrollFacade,
    @Autowired private val oppfriskSaksopplysningerService: OppfriskSaksopplysningerService,
    @Autowired private val vedtaksfattingFasade: VedtaksfattingFasade,
    @Autowired private val unleash: FakeUnleash,
    @Autowired private val journalpostRepo: JournalpostRepo,
    @Autowired private val trygdeavgiftsgrunnlagService: TrygdeavgiftsgrunnlagService,
    @Autowired private val opprettBehandlingForSak: OpprettBehandlingForSak,
) : JournalfoeringBase(testDataGenerator, journalføringService, oppgaveService, prosessinstansRepository) {

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
    }

    @AfterEach
    fun afterEach() {
        oAuthMockServer.stop()
        MedlRepo.repo.clear()
    }

    @Test
    fun `yrkesaktiv vedtak - FTRL - test`() {
        val saksnummer = lagFørstegangsBehandling()

        val journalfoeringTilordneDto = lagJournalfoeringOppgaveOgTilordneDto(
            saksnummer = saksnummer,
            journalfoeringTilordneDto = defaultJournalfoeringTilordneDto().apply {
                behandlingstemaKode = Behandlingstema.YRKESAKTIV.kode
                behandlingstypeKode = Behandlingstyper.NY_VURDERING.kode
            }
        )
        val behandling = journalførAndregangsOgVentTilProsesserErFerdige(
            journalfoeringTilordneDto,
            waitFor = ProsessType.JFR_ANDREGANG_NY_BEHANDLING
        ).behandling


        val vedtakRequest = FattVedtakRequest.Builder()
            .medBehandlingsresultatType(Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)
            .medVedtakstype(Vedtakstyper.ENDRINGSVEDTAK)
            .medBestillersId("komponent test")
            .build()

        executeAndWait(
            waitForprosessType = ProsessType.IVERKSETT_VEDTAK_FTRL
        ) {
            vedtaksfattingFasade.fattVedtak(behandling.id, vedtakRequest)
        }

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

        val medlemskapsperiodeId = opprettMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(
            behandling.id
        ).single().id

        medlemskapsperiodeService.oppdaterMedlemskapsperiode(
            behandling.id,
            medlemskapsperiodeId,
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2023, 2, 1),
            InnvilgelsesResultat.INNVILGET,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
        ).apply { medlemskapstype.shouldBe(Medlemskapstyper.FRIVILLIG) }

        trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
            behandling.id,
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

    /*@Test
    @Disabled
    fun `yrkesaktiv vedtak - FTRL - innvilgelse nytt vedtak`() {
        val saksbehandler = "Z123456"
        val subjectHandler: SubjectHandler = Mockito.mock(SpringSubjectHandler::class.java)
        SubjectHandler.set(subjectHandler)
        Mockito.`when`(subjectHandler.getUserID()).thenReturn(saksbehandler)

        val behandling = journalførOgVentTilProsesserErFerdige(
            defaultJournalføringDto().apply {
                fagsak.sakstype = Sakstyper.FTRL.kode
                fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
                behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
                behandlingstemaKode = Behandlingstema.YRKESAKTIV.kode
            },
            waitFor = ProsessType.JFR_NY_SAK_BRUKER,
            alsoWaitForprosessType = listOf(ProsessType.OPPRETT_OG_DISTRIBUER_BREV)
        ).behandling

        val mottatteOpplysninger =
            mottatteOpplysningerService.hentEllerOpprettMottatteOpplysninger(behandling.id, true)
                .shouldNotBeNull()
                .mottatteOpplysningerData.apply {
                    periode = Periode(
                        LocalDate.of(2023, 1, 1),
                        LocalDate.of(2023, 2, 1),
                    )
                    soeknadsland = Soeknadsland(listOf(Landkoder.BE.kode), false)
                }
        mottatteOpplysningerService.oppdaterMottatteOpplysninger(behandling.id, mottatteOpplysninger.toJsonNode)
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
            listOf("ORDINAER_ARBEIDSTAKER"), "YRKESAKTIVITET"
        ).apply {
            avklartefaktaType = null
            subjektID = null
            begrunnelseKoder = emptyList()
            begrunnelseFritekst = null
        }
        avklartefaktaService.lagreAvklarteFakta(behandling.id, setOf(yrkesgruppe, virksomhet, yrkesaktivitet))

        val forutgåendeMedlemskap = VilkaarDto().apply {
            vilkaar = "ART12_1_FORUTGAAENDE_MEDLEMSKAP"
            isOppfylt = true
        }
        val vesentlingVirksomhet = VilkaarDto().apply {
            vilkaar = "ART12_1_VESENTLIG_VIRKSOMHET"
            isOppfylt = true
        }
        val art12_1 = VilkaarDto().apply {
            vilkaar = "FO_883_2004_ART12_1"
            isOppfylt = true
        }
        vilkaarsresultatService.registrerVilkår(behandling.id, listOf(forutgåendeMedlemskap, vesentlingVirksomhet, art12_1))

        lovvalgsperiodeService.lagreLovvalgsperioder(behandling.id, listOf(Lovvalgsperiode().apply {
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            lovvalgsland = Land_iso2.BE
            medlemskapstype = Medlemskapstyper.PLIKTIG
            dekning = Trygdedekninger.FULL_DEKNING

            fom = LocalDate.of(2023, 1, 1)
            tom = LocalDate.of(2023, 2, 1)
        }))

        ferdigbehandlingKontrollFacade.kontroller(behandling.id, false, null, setOf())

        val vedtakRequest = FattVedtakRequest.Builder()
            .medBehandlingsresultatType(Behandlingsresultattyper.FERDIGBEHANDLET)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medBestillersId("komponent test")
            .medBetalingsIntervall(FaktureringsIntervall.MANEDLIG)
            .build()


        executeAndWait(
            waitForprosessType = ProsessType.IVERKSETT_VEDTAK_FTRL
        ) {
            vedtaksfattingFasade.fattVedtak(behandling.id, vedtakRequest)
        }

        val journalfoeringTilordneDto = lagJournalfoeringOppgaveOgTilordneDto(
            saksnummer = behandling.fagsak.saksnummer,
            journalfoeringTilordneDto = defaultJournalfoeringTilordneDto().apply {
                behandlingstemaKode = Behandlingstema.YRKESAKTIV.kode
                behandlingstypeKode = Behandlingstyper.NY_VURDERING.kode
            }
        )

        val behandling2 = journalførAndregangsOgVentTilProsesserErFerdige(
            journalfoeringTilordneDto,
            waitFor = ProsessType.JFR_ANDREGANG_NY_BEHANDLING
        ).behandling

        executeAndWait(
            waitForprosessType = ProsessType.IVERKSETT_VEDTAK_FTRL
        ) {
            vedtaksfattingFasade.fattVedtak(behandling2.id, vedtakRequest)
        }

        val vedtakRequest2 = FattVedtakRequest.Builder()
            .medBehandlingsresultatType(Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)
            .medVedtakstype(Vedtakstyper.ENDRINGSVEDTAK)
            .medBestillersId("komponent test")
            .build()


        executeAndWait(
            waitForprosessType = ProsessType.IVERKSETT_VEDTAK_FTRL
        ) {
            vedtaksfattingFasade.fattVedtak(behandling.id, vedtakRequest2)
        }


        behandlingsresultatService.hentBehandlingsresultat(behandling2.id).apply {
            type shouldBe Behandlingsresultattyper.FERDIGBEHANDLET
            behandlingsmåte shouldBe Behandlingsmaate.MANUELT
            fastsattAvLand shouldBe Land_iso2.NO
        }
        lovvalgsperiodeService.hentLovvalgsperiode(behandling2.id).apply {
            innvilgelsesresultat shouldBe InnvilgelsesResultat.INNVILGET
            bestemmelse shouldBe Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            lovvalgsland shouldBe Land_iso2.BE
            medlemskapstype shouldBe Medlemskapstyper.PLIKTIG
            dekning shouldBe Trygdedekninger.FULL_DEKNING
            fom shouldBe LocalDate.of(2023, 1, 1)
            tom shouldBe LocalDate.of(2023, 2, 1)
        }
        behandlingRepository.findById(behandling.id).orElse(null)
            .shouldNotBeNull().apply {
                withClue("Behandlingsstatus skal være AVSLUTTET") {
                    status shouldBe Behandlingsstatus.AVSLUTTET
                }
                fagsak.apply {
                    withClue("Saksstatus skal være LOVVALG_AVKLART") {
                        status shouldBe Saksstatuser.LOVVALG_AVKLART
                    }
                }
            }
    }*/

    private val Any.toJsonNode: JsonNode
        get() {
            return jacksonObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(JavaTimeModule())
                .valueToTree(this)
        }
}
