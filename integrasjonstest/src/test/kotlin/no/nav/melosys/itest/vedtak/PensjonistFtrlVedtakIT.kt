package no.nav.melosys.itest.vedtak

import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.manglendebetaling.Betalingsstatus
import no.nav.melosys.domain.manglendebetaling.ManglendeFakturabetalingMelding
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.integrasjon.trygdeavgift.dto.DatoPeriodeDto
import no.nav.melosys.itest.AvgiftFaktureringTestBase
import no.nav.melosys.itest.MelosysHendelseKafkaConsumer
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import no.nav.melosys.service.ftrl.medlemskapsperiode.OpprettForslagMedlemskapsperiodeService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import no.nav.melosys.service.vedtak.FattVedtakRequest
import no.nav.melosys.service.vedtak.VedtaksfattingFasade
import no.nav.melosys.service.vilkaar.VilkaarDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import java.time.LocalDate

class PensjonistFtrlVedtakIT(
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val mottatteOpplysningerService: MottatteOpplysningerService,
    @Autowired private val avklartefaktaService: AvklartefaktaService,
    @Autowired private val vilkaarsresultatService: VilkaarsresultatService,
    @Autowired private val medlemskapsperiodeService: MedlemskapsperiodeService,
    @Autowired private val opprettForslagMedlemskapsperiodeService: OpprettForslagMedlemskapsperiodeService,
    @Autowired private val oppfriskSaksopplysningerService: OppfriskSaksopplysningerService,
    @Autowired private val vedtaksfattingFasade: VedtaksfattingFasade,
    @Autowired private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
    @Autowired @Qualifier("manglendeFakturabetalingMelding") private val manglendeFakturabetalingMeldingTemplate: KafkaTemplate<String, ManglendeFakturabetalingMelding>,
    @Autowired private val melosysHendelseKafkaConsumer: MelosysHendelseKafkaConsumer,
) : AvgiftFaktureringTestBase(TrygdeavgiftsberegningTransformer()){

    private val kafkaTopic = "teammelosys.manglende-fakturabetaling-local"
    override val fakturaserieReferanse: String = "01J17B5NTTDYKFB5DZTSSQEHJ0"

    @AfterEach
    fun afterEach() {
        melosysHendelseKafkaConsumer.clear()
    }


    @Test
    fun `Oppretter riktig oppgave ved manglende innbetaling for pensjonister`() {
        val saksnummer = lagFørstegangsbehandling(Skatteplikttype.IKKE_SKATTEPLIKTIG, false)

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
                behandlinger.shouldHaveSize(1)
                finnAktivBehandlingIkkeÅrsavregning().shouldNotBeNull().run {
                    tema shouldBe Behandlingstema.PENSJONIST
                    type shouldBe Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
                    behandlingsårsak.type shouldBe Behandlingsaarsaktyper.SØKNAD
                }
            }

        mockServer.verify(
            1,
            WireMock.postRequestedFor(WireMock.urlEqualTo("/api/v1/mal/varsel_manglende_innbetaling/lag-pdf?somKopi=false&utkast=false"))
        )
    }

    fun lagFørstegangsbehandling(skatteplikttype: Skatteplikttype, arbeidsgiversavgiftBetales: Boolean): String {
        val behandling = journalførOgVentTilProsesserErFerdige(
            defaultJournalføringDto().apply {
                fagsak.sakstype = Sakstyper.FTRL.name
                fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.name
                behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
                behandlingstemaKode = Behandlingstema.PENSJONIST.name
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
                            trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
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
            vilkaar = Vilkaar.FTRL_2_1_BOSATT_NORGE_FORUT.kode
            isOppfylt = true
        }, VilkaarDto().apply {
            vilkaar = Vilkaar.FTRL_2_1_LOVLIG_OPPHOLD.kode
            isOppfylt = true
        }, VilkaarDto().apply {
            vilkaar = Vilkaar.FTRL_2_1_OPPHOLD_UNDER_12MND.kode
            isOppfylt = true
        })
        vilkaarsresultatService.registrerVilkår(behandling.id, vilkår)

        setupTrygdeavgiftBeregning(behandling.id, skatteplikttype, arbeidsgiversavgiftBetales)

        val vedtakRequest = FattVedtakRequest.Builder()
            .medBehandlingsresultatType(Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medBestillersId("komponent test")
            .medFritekstSed("Fritekst")
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
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
        ).single().id

        val medlemskapsperiode = medlemskapsperiodeService.oppdaterMedlemskapsperiode(
            behandlingId,
            medlemskapsperiodeId,
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2023, 2, 1),
            InnvilgelsesResultat.INNVILGET,
            Trygdedekninger.FULL_DEKNING_FTRL,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
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
                this.type = Inntektskildetype.UFØRETRYGD
                isArbeidsgiversavgiftBetalesTilSkatt = arbeidsgiversavgiftBetales
                avgiftspliktigMndInntekt = Penger(1000.toBigDecimal())
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
            type = Inntektskildetype.UFØRETRYGD
            isArbeidsgiversavgiftBetalesTilSkatt = arbeidsgiversavgiftBetales
            avgiftspliktigMndInntekt = Penger(10000.toBigDecimal(), "nok")
        }

        val trygdeavgiftsperioder = HashSet<Trygdeavgiftsperiode>()
        trygdeavgiftsperioder.add(
            Trygdeavgiftsperiode(
                periodeFra = LocalDate.of(2023, 1, 1),
                periodeTil = LocalDate.of(2023, 2, 1),
                trygdesats = 6.8.toBigDecimal(),
                trygdeavgiftsbeløpMd = Penger(1000.toBigDecimal(), "nok"),
                grunnlagMedlemskapsperiode = medlemskapsperiode,
                grunnlagSkatteforholdTilNorge = skatteforholdTilNorge,
                grunnlagInntekstperiode = inntektsperiode
            )
        )

        medlemskapsperiode.trygdeavgiftsperioder = trygdeavgiftsperioder
    }

}
