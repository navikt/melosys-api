package no.nav.melosys.itest.vedtak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import io.getunleash.FakeUnleash
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.*
import no.nav.melosys.domain.manglendebetaling.Betalingsstatus
import no.nav.melosys.domain.manglendebetaling.ManglendeFakturabetalingMelding
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.integrasjon.faktureringskomponenten.NyFakturaserieResponseDto
import no.nav.melosys.integrasjon.hendelser.VedtakHendelseMelding
import no.nav.melosys.integrasjon.trygdeavgift.dto.DatoPeriodeDto
import no.nav.melosys.itest.JournalfoeringBase
import no.nav.melosys.itest.MelosysHendelseKafkaConsumer
import no.nav.melosys.melosysmock.medl.MedlRepo
import no.nav.melosys.melosysmock.testdata.JournalføringsoppgaveGenerator
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import no.nav.melosys.service.ftrl.medlemskapsperiode.OpprettForslagMedlemskapsperiodeService
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.OpprettBehandlingForSak
import no.nav.melosys.service.sak.OpprettSakDto
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
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import java.time.LocalDate

class YrkesaktivFtrlVedtakIT(
    @Autowired journalføringsoppgaveGenerator: JournalføringsoppgaveGenerator,
    @Autowired journalføringService: JournalfoeringService,
    @Autowired oppgaveService: OppgaveService,
    @Autowired private val avklartefaktaService: AvklartefaktaService,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingsresultatService: BehandlingsresultatService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val mottatteOpplysningerService: MottatteOpplysningerService,
    @Autowired private val vilkaarsresultatService: VilkaarsresultatService,
    @Autowired private val medlemskapsperiodeService: MedlemskapsperiodeService,
    @Autowired private val opprettForslagMedlemskapsperiodeService: OpprettForslagMedlemskapsperiodeService,
    @Autowired private val oppfriskSaksopplysningerService: OppfriskSaksopplysningerService,
    @Autowired private val vedtaksfattingFasade: VedtaksfattingFasade,
    @Autowired private val opprettBehandlingForSak: OpprettBehandlingForSak,
    @Autowired private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
    @Autowired @Qualifier("manglendeFakturabetalingMelding") private val manglendeFakturabetalingMeldingTemplate: KafkaTemplate<String, ManglendeFakturabetalingMelding>,
    @Autowired private val unleash: FakeUnleash,
    @Autowired private val melosysHendelseKafkaConsumer: MelosysHendelseKafkaConsumer,
) : JournalfoeringBase(
    journalføringsoppgaveGenerator, journalføringService, oppgaveService,
    DynamiskTrygdeavgiftsberegningTransformer()
) {

    private var originalSubjectHandler: SubjectHandler? = null
    private val kafkaTopic = "teammelosys.manglende-fakturabetaling-local"
    private val fakturaserieReferanse: String = "01J17B5NTTDYKFB5DZTSSQEHJ0"

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

        val fakturaResponse = NyFakturaserieResponseDto(fakturaserieReferanse)

        mockServer.stubFor(
            WireMock.post("/fakturaserier")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(fakturaResponse.toJsonNode.toString())
                )
        )
        mockServer.stubFor(
            WireMock.delete(WireMock.urlMatching("/fakturaserier/.*"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(fakturaResponse.toJsonNode.toString())
                )
        )
        mockServer.stubFor(
            WireMock.post(WireMock.urlMatching("/fakturaer"))
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
        melosysHendelseKafkaConsumer.clear()
        MedlRepo.repo.clear()
        SubjectHandler.set(originalSubjectHandler)

    }

    @Test
    fun `Yrkesaktiv vedtak - FTRL - opprett fakturaserie for førstegangsbehandling og kanseller fakturaserie i ny vurdering`() {
        val saksnummer = lagFørstegangsBehandling(Skatteplikttype.IKKE_SKATTEPLIKTIG, false)

        val behandlingsId = executeAndWait(
            mapOf(
                ProsessType.OPPRETT_REPLIKERT_BEHANDLING_FOR_SAK to 1
            )
        ) {
            opprettBehandlingForSak.opprettBehandling(
                saksnummer,
                lagOpprettSakDto()
            )
        }.behandling.id

        val periode = DatoPeriodeDto(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 2, 1))


        val skattefordholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = periode.fom
                tomDato = periode.tom
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        )
        val inntektsforholdsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = periode.fom
                tomDato = periode.tom
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                isArbeidsgiversavgiftBetalesTilSkatt = true
                avgiftspliktigMndInntekt = Penger(10000.toBigDecimal())
            }
        )

        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(behandlingsId, skattefordholdsperioder, inntektsforholdsperioder)

        val vedtakRequest = FattVedtakRequest.Builder()
            .medBehandlingsresultatType(Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)
            .medVedtakstype(Vedtakstyper.ENDRINGSVEDTAK)
            .medBestillersId("komponent test")
            .build()

        executeAndWait(
            mapOf(
                ProsessType.IVERKSETT_VEDTAK_FTRL to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        ) {
            vedtaksfattingFasade.fattVedtak(behandlingsId, vedtakRequest)
        }


        behandlingsresultatService.hentBehandlingsresultat(behandlingsId).run {
            type shouldBe Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            behandlingsmåte shouldBe Behandlingsmaate.MANUELT
            fastsattAvLand shouldBe Land_iso2.NO
        }
        behandlingRepository.findById(behandlingsId)
            .shouldBePresent().run {
                withClue("Behandlingsstatus skal være AVSLUTTET") {
                    status shouldBe Behandlingsstatus.AVSLUTTET
                }
                fagsak.run {
                    withClue("Saksstatus skal være LOVVALG_AVKLART") {
                        status shouldBe Saksstatuser.LOVVALG_AVKLART
                    }
                }
            }

        melosysHendelseKafkaConsumer.melosysHendelser.shouldHaveSize(2)
            .last().value()
            .melding.shouldBeInstanceOf<VedtakHendelseMelding>()
            .shouldBe(
                VedtakHendelseMelding(
                    folkeregisterIdent = "30056928150",
                    sakstype = Sakstyper.FTRL,
                    sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
                    behandligsresultatType = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN,
                    vedtakstype = Vedtakstyper.ENDRINGSVEDTAK,
                    medlemskapsperioder = listOf(
                        no.nav.melosys.integrasjon.hendelser.Periode(
                            LocalDate.of(2023, 1, 1),
                            LocalDate.of(2023, 2, 1),
                            InnvilgelsesResultat.INNVILGET
                        )
                    ),
                    lovvalgsperioder = listOf()
                )
            )

        mockServer.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/fakturaserier")))
        mockServer.verify(1, WireMock.deleteRequestedFor(WireMock.urlEqualTo("/fakturaserier/$fakturaserieReferanse")))
    }

    @Test
    fun `Håndtere manglende innbetaling i sak som allerede har en åpen behandling`() {
        val saksnummer = lagFørstegangsBehandling(Skatteplikttype.IKKE_SKATTEPLIKTIG, false)

        val behandlingID = executeAndWait(
            mapOf(
                ProsessType.OPPRETT_REPLIKERT_BEHANDLING_FOR_SAK to 1
            )
        ) {
            opprettBehandlingForSak.opprettBehandling(
                saksnummer,
                lagOpprettSakDto()
            )
        }.behandling.id

        executeAndWait(
            mapOf(
                ProsessType.OPPRETT_NY_BEHANDLING_MANGLENDE_INNBETALING to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        ) {
            val kafkaMelding = ManglendeFakturabetalingMelding(
                fakturaserieReferanse = fakturaserieReferanse,
                betalingsstatus = Betalingsstatus.IKKE_BETALT,
                datoMottatt = LocalDate.of(2023, 12, 13),
                fakturanummer = "23004119"
            )
            manglendeFakturabetalingMeldingTemplate.send(kafkaTopic, kafkaMelding)
        }

        fagsakRepository.findBySaksnummer(saksnummer)
            .shouldBePresent().run {
                behandlinger.shouldHaveSize(2)
                finnAktivBehandlingIkkeÅrsavregning().shouldNotBeNull().run {
                    tema shouldBe Behandlingstema.YRKESAKTIV
                    type shouldBe Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
                    behandlingsårsak.type shouldBe Behandlingsaarsaktyper.SØKNAD
                }
            }

        behandlingsresultatService.hentBehandlingsresultat(behandlingID).run {
            type shouldBe Behandlingsresultattyper.IKKE_FASTSATT
            behandlingsmåte shouldBe Behandlingsmaate.MANUELT
            fastsattAvLand shouldBe Land_iso2.NO
        }
        behandlingRepository.findById(behandlingID).shouldBePresent()
            .run {
                withClue("Behandlingsstatus skal være OPPRETTET") {
                    status shouldBe Behandlingsstatus.OPPRETTET
                }
                fagsak.apply {
                    withClue("Saksstatus skal være LOVVALG_AVKLART") {
                        status shouldBe Saksstatuser.LOVVALG_AVKLART
                    }
                }
            }

        mockServer.verify(
            1,
            WireMock.postRequestedFor(WireMock.urlEqualTo("/api/v1/mal/varsel_manglende_innbetaling/lag-pdf?somKopi=false&utkast=false"))
        )
    }

    private fun lagOpprettSakDto(): OpprettSakDto {
        val opprettsakdto = OpprettSakDto()
        opprettsakdto.behandlingstema = Behandlingstema.YRKESAKTIV
        opprettsakdto.behandlingstype = Behandlingstyper.NY_VURDERING
        opprettsakdto.mottaksdato = LocalDate.now()
        opprettsakdto.behandlingsaarsakType = Behandlingsaarsaktyper.SØKNAD
        return opprettsakdto
    }

    fun lagFørstegangsBehandling(skatteplikttype: Skatteplikttype, arbeidsgiversavgiftBetales: Boolean): String {
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

        setupTrygdeavgiftBeregning(behandling.id, skatteplikttype, arbeidsgiversavgiftBetales)

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

    private fun setupTrygdeavgiftBeregning(behandlingId: Long, skatteplikttype: Skatteplikttype, arbeidsgiversavgiftBetales: Boolean) {
        val medlemskapsperiodeId = opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(
            behandlingId,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
        ).single().id

        val medlemskapsperiode = medlemskapsperiodeService.oppdaterMedlemskapsperiode(
            behandlingId,
            medlemskapsperiodeId,
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

        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(behandlingId, skattefordholdsperioder, inntektsforholdsperioder)


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

