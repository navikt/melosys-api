package no.nav.melosys.itest.vedtak

import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.aarsavregning.Skattehendelse
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.*
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.integrasjon.trygdeavgift.dto.DatoPeriodeDto
import no.nav.melosys.itest.AvgiftFaktureringTestBase
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import no.nav.melosys.service.ftrl.medlemskapsperiode.OpprettForslagMedlemskapsperiodeService
import no.nav.melosys.service.journalforing.dto.PeriodeDto
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.sak.OpprettBehandlingForSak
import no.nav.melosys.service.sak.OpprettSak
import no.nav.melosys.service.sak.OpprettSakDto
import no.nav.melosys.service.sak.SøknadDto
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import no.nav.melosys.service.vedtak.FattVedtakRequest
import no.nav.melosys.service.vedtak.VedtaksfattingFasade
import no.nav.melosys.service.vilkaar.VilkaarDto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import java.math.BigDecimal
import java.time.LocalDate

class ÅrsavregningIT(
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
    @Autowired private val skatteHendelseMeldingKafkaTemplate: KafkaTemplate<String, Skattehendelse>,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired private val årsavregningService: ÅrsavregningService,
    @Autowired private val opprettSak: OpprettSak
) : AvgiftFaktureringTestBase(
    TrygdeavgiftsberegningTransformer()
) {

    override val fakturaserieReferanse: String = "AAJ17B5NTTDYKFB5DZTSSQEHZZ"

    @Test
    fun `oppretter prosess og påfølgende årsavregningsbehandling for alle saker knyttet til en skattehendelse `() {
        val saksnummer1 = lagFørstegangsbehandling(Skatteplikttype.IKKE_SKATTEPLIKTIG, false)
        val saksnummer2 = lagFørstegangsbehandling(Skatteplikttype.IKKE_SKATTEPLIKTIG, false)


        executeAndWait(
            mapOf(
                ProsessType.OPPRETT_NY_BEHANDLING_AARSAVREGNING to 2
            )
        ) {
            skatteHendelseMeldingKafkaTemplate.send(
                "teammelosys.skattehendelser.v1-local",
                Skattehendelse("2023", "30056928150", "ny")
            )
        }


        listOf(saksnummer1, saksnummer2).forEach { saksnummer ->
            fagsakRepository.findBySaksnummer(saksnummer)
                .shouldBePresent().run {
                    behandlinger.shouldHaveSize(2)
                        .firstOrNull { it.type == Behandlingstyper.ÅRSAVREGNING }
                        .shouldNotBeNull()
                        .run {
                            behandlingsresultatRepository.findById(id)
                                .shouldBePresent()
                                .årsavregning
                                .shouldNotBeNull()
                                .run {
                                    aar shouldBe 2023
                                }
                        }
                }
        }
    }

    @Test
    fun `fatter vedtak om årsavregning, uten tidligere grunnlag i melosys`() {
        val opprettSakDto = OpprettSakDto().apply {
            hovedpart = Aktoersroller.BRUKER
            brukerID = "30056928150"
            sakstype = Sakstyper.FTRL
            sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG
            behandlingstema = Behandlingstema.YRKESAKTIV
            behandlingstype = Behandlingstyper.ÅRSAVREGNING
            behandlingsaarsakType = Behandlingsaarsaktyper.SØKNAD
            soknadDto = SøknadDto().apply {
                periode = PeriodeDto(
                    LocalDate.of(2021, 10, 1),
                    LocalDate.of(2021, 10, 2)
                )
            }
            mottaksdato = LocalDate.of(2021, 10, 24)
            skalTilordnes = true
        }

        val årsavregningBehandlingID = executeAndWait(mapOf(ProsessType.OPPRETT_SAK to 1)) {
            opprettSak.opprettNySakOgBehandling(opprettSakDto)
        }.behandling.id


        årsavregningService.opprettÅrsavregning(årsavregningBehandlingID, 2023)
        val årsavregning =
            behandlingsresultatRepository.findWithLovvalgOgMedlemskapsperioderById(årsavregningBehandlingID).shouldBePresent().årsavregning
        val periode = DatoPeriodeDto(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 2, 1))
        val skattefordholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = periode.fom
                tomDato = periode.tom
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        )
        val inntektsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = periode.fom
                tomDato = periode.tom
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                isArbeidsgiversavgiftBetalesTilSkatt = true
                avgiftspliktigMndInntekt = Penger(10000.toBigDecimal())
                avgiftspliktigTotalinntekt = Penger(10000.toBigDecimal())
            }
        )

        medlemskapsperiodeService.opprettMedlemskapsperiode(
            behandlingsresultatID = årsavregningBehandlingID,
            fom = periode.fom,
            tom = periode.tom,
            innvilgelsesResultat = InnvilgelsesResultat.INNVILGET,
            trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL,
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_5_FØRSTE_LEDD_A,
        )
        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(årsavregningBehandlingID, skattefordholdsperioder, inntektsperioder)

        val tidligereFakturertBeloep = BigDecimal(1000)
        val nyttTotalbeloep = BigDecimal(2000)
        årsavregningService.oppdater(årsavregningBehandlingID, årsavregning.id, tidligereFakturertBeloep, nyttTotalbeloep)

        val vedtakRequestÅrsavregning = FattVedtakRequest.Builder()
            .medBehandlingsresultatType(Behandlingsresultattyper.FERDIGBEHANDLET)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medBestillersId("komponent test")
            .build()

        executeAndWait(
            mapOf(
                ProsessType.IVERKSETT_VEDTAK_AARSAVREGNING to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        ) {
            vedtaksfattingFasade.fattVedtak(årsavregningBehandlingID, vedtakRequestÅrsavregning)
        }


        behandlingsresultatService.hentBehandlingsresultat(årsavregningBehandlingID).run {
            type shouldBe Behandlingsresultattyper.FERDIGBEHANDLET
            behandlingsmåte shouldBe Behandlingsmaate.MANUELT
            this.årsavregning.aar shouldBe 2023
            this.årsavregning.tidligereFakturertBeloep shouldBe tidligereFakturertBeloep
            this.årsavregning.nyttTotalbeloep shouldBe nyttTotalbeloep
            this.årsavregning.tilFaktureringBeloep shouldBe nyttTotalbeloep - tidligereFakturertBeloep
            fakturaserieReferanse shouldBe this@ÅrsavregningIT.fakturaserieReferanse
        }
        behandlingRepository.findById(årsavregningBehandlingID)
            .shouldBePresent().run {
                withClue("Behandlingsstatus skal være AVSLUTTET") {
                    type shouldBe Behandlingstyper.ÅRSAVREGNING
                    status shouldBe Behandlingsstatus.AVSLUTTET
                }
            }
        mockServer.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/fakturaer")))
    }

    @Test
    fun `fatter vedtak om årsavregning`() {
        val saksnummer = lagFørstegangsbehandling(Skatteplikttype.IKKE_SKATTEPLIKTIG, false)

        val årsavregningBehandlingID = executeAndWait(
            mapOf(
                ProsessType.OPPRETT_NY_BEHANDLING_FOR_SAK to 1
            )
        ) {
            opprettBehandlingForSak.opprettBehandling(
                saksnummer,
                lagOpprettSakDtoÅrsavregning()
            )
        }.behandling.id
        val tidligereFakturertBeloep = BigDecimal(1000)
        val nyttTotalbeloep = BigDecimal(2000)
        årsavregningService.opprettÅrsavregning(årsavregningBehandlingID, 2023)
        val årsavregning =
            behandlingsresultatRepository.findWithLovvalgOgMedlemskapsperioderById(årsavregningBehandlingID).shouldBePresent().årsavregning
        val periode = DatoPeriodeDto(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 2, 1))
        val skattefordholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = periode.fom
                tomDato = periode.tom
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        )
        val inntektsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = periode.fom
                tomDato = periode.tom
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                isArbeidsgiversavgiftBetalesTilSkatt = true
                avgiftspliktigMndInntekt = Penger(10000.toBigDecimal())
                avgiftspliktigTotalinntekt = Penger(10000.toBigDecimal())
            }
        )
        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(årsavregningBehandlingID, skattefordholdsperioder, inntektsperioder)
        årsavregningService.oppdater(årsavregningBehandlingID, årsavregning.id, tidligereFakturertBeloep, nyttTotalbeloep)

        val vedtakRequestÅrsavregning = FattVedtakRequest.Builder()
            .medBehandlingsresultatType(Behandlingsresultattyper.FERDIGBEHANDLET)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medBestillersId("komponent test")
            .build()

        executeAndWait(
            mapOf(
                ProsessType.IVERKSETT_VEDTAK_AARSAVREGNING to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        ) {
            vedtaksfattingFasade.fattVedtak(årsavregningBehandlingID, vedtakRequestÅrsavregning)
        }


        behandlingsresultatService.hentBehandlingsresultat(årsavregningBehandlingID).run {
            type shouldBe Behandlingsresultattyper.FERDIGBEHANDLET
            behandlingsmåte shouldBe Behandlingsmaate.MANUELT
            this.årsavregning.aar shouldBe 2023
            this.årsavregning.tidligereFakturertBeloep shouldBe tidligereFakturertBeloep
            this.årsavregning.nyttTotalbeloep shouldBe nyttTotalbeloep
            this.årsavregning.tilFaktureringBeloep shouldBe nyttTotalbeloep - tidligereFakturertBeloep
            fakturaserieReferanse shouldBe this@ÅrsavregningIT.fakturaserieReferanse
        }
        behandlingRepository.findById(årsavregningBehandlingID)
            .shouldBePresent().run {
                withClue("Behandlingsstatus skal være AVSLUTTET") {
                    type shouldBe Behandlingstyper.ÅRSAVREGNING
                    status shouldBe Behandlingsstatus.AVSLUTTET
                }
            }
        mockServer.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/fakturaserier")))
    }

    private fun lagOpprettSakDtoÅrsavregning() = OpprettSakDto().apply {
        sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG
        sakstype = Sakstyper.FTRL
        behandlingstema = Behandlingstema.YRKESAKTIV
        behandlingstype = Behandlingstyper.ÅRSAVREGNING
        mottaksdato = LocalDate.of(2023, 1, 1)
        behandlingsaarsakType = Behandlingsaarsaktyper.HENVENDELSE
    }

    fun lagFørstegangsbehandling(skatteplikttype: Skatteplikttype, arbeidsgiversavgiftBetales: Boolean): String {
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

        medlemskapsperiode.trygdeavgiftsperioder =
            setOf(
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
    }
}
