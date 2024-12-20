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
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.integrasjon.faktureringskomponenten.NyFakturaserieResponseDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.DatoPeriodeDto
import no.nav.melosys.itest.JournalfoeringBase
import no.nav.melosys.itest.MelosysHendelseKafkaConsumer
import no.nav.melosys.melosysmock.medl.MedlRepo
import no.nav.melosys.melosysmock.testdata.JournalføringsoppgaveGenerator
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.avgift.satsendring.SatsendringFinner
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import no.nav.melosys.service.ftrl.medlemskapsperiode.OpprettForslagMedlemskapsperiodeService
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
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

class SatsendringIT(
    @Autowired journalføringsoppgaveGenerator: JournalføringsoppgaveGenerator,
    @Autowired journalføringService: JournalfoeringService,
    @Autowired oppgaveService: OppgaveService,
    @Autowired private val avklartefaktaService: AvklartefaktaService,
    @Autowired private val medlemskapsperiodeService: MedlemskapsperiodeService,
    @Autowired private val mottatteOpplysningerService: MottatteOpplysningerService,
    @Autowired private val oppfriskSaksopplysningerService: OppfriskSaksopplysningerService,
    @Autowired private val opprettForslagMedlemskapsperiodeService: OpprettForslagMedlemskapsperiodeService,
    @Autowired private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
    @Autowired private val vedtaksfattingFasade: VedtaksfattingFasade,
    @Autowired private val vilkaarsresultatService: VilkaarsresultatService,
    @Autowired private val satsendringFinner: SatsendringFinner,
    @Autowired private val unleash: FakeUnleash,
    @Autowired private val melosysHendelseKafkaConsumer: MelosysHendelseKafkaConsumer,
) : JournalfoeringBase(
    journalføringsoppgaveGenerator, journalføringService, oppgaveService,
    DynamiskTrygdeavgiftsberegningTransformer()
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
                        .withTransformers("dynamisk-trygdeavgiftsberegning-transformer")
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
    fun `Satsendring etter yrkesaktiv FTRL vedtak`() {
        lagFørstegangsbehandling(harSatsendring = false)
        val saksnummerMedSatsendring = lagFørstegangsbehandling(harSatsendring = true)

        // Skal finne saken med satsendring
        satsendringFinner.finn() shouldBe saksnummerMedSatsendring
    }


    fun lagFørstegangsbehandling(harSatsendring: Boolean): String {
        val behandling = journalførOgVentTilProsesserErFerdige(
            defaultJournalføringDto().apply {
                fagsak.sakstype = Sakstyper.FTRL.name
                fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.name
                behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
                behandlingstemaKode = Behandlingstema.YRKESAKTIV.name
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
                            periode = Periode(
                                LocalDate.of(2023, 1, 1),
                                LocalDate.of(2023, 2, 1),
                            )
                            soeknadsland = Soeknadsland(listOf("AF"), false)
                            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
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

        setupTrygdeavgift(behandling.id, Skatteplikttype.IKKE_SKATTEPLIKTIG, false)

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

        return behandling.fagsak.saksnummer.also {
            addCleanUpAction {
                slettSakMedAvhengigheter(it)
            }
        }
    }

    private fun setupTrygdeavgift(behandlingID: Long, skatteplikttype: Skatteplikttype, arbeidsgiversavgiftBetales: Boolean) {
        val medlemskapsperiodeID = opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(
            behandlingID,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
        ).single().id

        val medlemskapsperiode = medlemskapsperiodeService.oppdaterMedlemskapsperiode(
            behandlingID,
            medlemskapsperiodeID,
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2023, 2, 1),
            InnvilgelsesResultat.INNVILGET,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
        )
        val periode = DatoPeriodeDto(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 2, 1))
        val skattefordholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = periode.fom
                tomDato = periode.tom
                this.skatteplikttype = skatteplikttype
            }
        )
        val inntektsforholdsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = periode.fom
                tomDato = periode.tom
                this.type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                isArbeidsgiversavgiftBetalesTilSkatt = arbeidsgiversavgiftBetales
                avgiftspliktigMndInntekt = Penger(10000.toBigDecimal())
                avgiftspliktigTotalinntekt = Penger(10000.toBigDecimal())
            }
        )

        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(behandlingID, skattefordholdsperioder, inntektsforholdsperioder)


        val skatteforholdTilNorge = SkatteforholdTilNorge().apply {
            fomDato = LocalDate.of(2023, 1, 1)
            tomDato = LocalDate.of(2023, 2, 1)
            this@apply.skatteplikttype = skatteplikttype
        }

        val inntektsperiode = Inntektsperiode().apply {
            fomDato = LocalDate.of(2023, 1, 1)
            tomDato = LocalDate.of(2023, 2, 1)
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            isArbeidsgiversavgiftBetalesTilSkatt = arbeidsgiversavgiftBetales
            avgiftspliktigMndInntekt = Penger(10000.toBigDecimal(), "nok")
        }

        val trygdeavgiftsperioder = HashSet<Trygdeavgiftsperiode>()
        trygdeavgiftsperioder.add(
            Trygdeavgiftsperiode(periodeFra = LocalDate.of(2023, 1, 1),
                periodeTil = LocalDate.of(2023, 2, 1),
                trygdesats = 6.8.toBigDecimal(),
                trygdeavgiftsbeløpMd = Penger(1000.toBigDecimal(), "nok"),
                grunnlagMedlemskapsperiode = medlemskapsperiode,
                grunnlagSkatteforholdTilNorge = skatteforholdTilNorge,
                grunnlagInntekstperiode = inntektsperiode)
        )

        medlemskapsperiode.trygdeavgiftsperioder = trygdeavgiftsperioder
    }

    private val Any.toJsonNode: JsonNode
        get() {
            return jacksonObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(JavaTimeModule())
                .valueToTree(this)
        }
}
