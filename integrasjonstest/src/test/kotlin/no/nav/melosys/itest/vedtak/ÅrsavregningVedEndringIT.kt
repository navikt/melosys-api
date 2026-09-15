package no.nav.melosys.itest.vedtak

import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.*
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.itest.AvgiftFaktureringTestBase
import no.nav.melosys.repository.BehandlingRepository
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
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.sak.OpprettBehandlingForSak
import no.nav.melosys.service.sak.OpprettSakDto
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import no.nav.melosys.service.vedtak.FattVedtakRequest
import no.nav.melosys.service.vedtak.VedtaksfattingFasade
import no.nav.melosys.service.vilkaar.VilkaarDto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.time.LocalDate

/**
 * MELOSYS-8006: Ny vurdering fjerner et tidligere år → automatisk årsavregning som krediterer hele året.
 *
 * Flyt:
 *  1. Toggle «ikke tidligere perioder» AV. Førstegangsbehandling FTRL yrkesaktiv med medlemskapsperiode
 *     1. desember i fjor – 31. desember i år, ikke skattepliktig. Fakturaserie opprettes for begge år.
 *  2. Toggle PÅ. Ny vurdering avkorter perioden til 1. januar – 31. desember i år.
 *  3. Endringsvedtaket oppretter automatisk årsavregning for fjoråret (OppretteÅrsavregningVedEndring).
 *  4. Årsavregningen har ingen avgiftspliktig periode for fjoråret: endelig avgift settes til 0 ved
 *     opprettelse, og beløp til fakturering blir minus det som ble fakturert for fjoråret.
 *  5. Vedtak på årsavregningen går gjennom uten manuell beregning: brev produseres og kreditnota sendes.
 *
 * Med transformeren [ÅrsdeltTrygdeavgiftsberegningTransformer] er avgiften 1000 kr/md, så desember i fjor
 * gir 1000 kr fakturert, og krediteringen skal være -1000 kr.
 *
 * Scenario 2 (akseptansekriterium 2): fjoråret er allerede årsavregnet og fastsatt manuelt før den nye
 * vurderingen. Den nye, automatisk opprettede årsavregningen skal ikke arve det manuelle beløpet, men
 * settes til 0 og kreditere det som sist ble fastsatt for året.
 */
class ÅrsavregningVedEndringIT(
    @Autowired private val avklartefaktaService: AvklartefaktaService,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val behandlingsresultatService: BehandlingsresultatService,
    @Autowired private val mottatteOpplysningerService: MottatteOpplysningerService,
    @Autowired private val vilkaarsresultatService: VilkaarsresultatService,
    @Autowired private val medlemskapsperiodeService: MedlemskapsperiodeService,
    @Autowired private val opprettForslagMedlemskapsperiodeService: OpprettForslagMedlemskapsperiodeService,
    @Autowired private val oppfriskSaksopplysningerService: OppfriskSaksopplysningerService,
    @Autowired private val vedtaksfattingFasade: VedtaksfattingFasade,
    @Autowired private val opprettBehandlingForSak: OpprettBehandlingForSak,
    @Autowired private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
    @Autowired private val årsavregningService: ÅrsavregningService,
) : AvgiftFaktureringTestBase(ÅrsdeltTrygdeavgiftsberegningTransformer()) {

    override val fakturaserieReferanse: String = "01J8006AARFALLERBORT000001"

    private val inneværendeÅr = LocalDate.now().year
    private val fjoråret = inneværendeÅr - 1

    private val periodeFørstegang = Periode(LocalDate.of(fjoråret, 12, 1), LocalDate.of(inneværendeÅr, 12, 31))
    private val periodeNyVurdering = Periode(LocalDate.of(inneværendeÅr, 1, 1), LocalDate.of(inneværendeÅr, 12, 31))

    /** 1000 kr/md for desember i fjor, jf. transformeren. */
    private val fakturertForFjoråret = BigDecimal(1000)

    @Test
    fun `ny vurdering som fjerner fjoråret gir automatisk årsavregning med endelig avgift 0 og full kreditering`() {
        // ---- 1. Førstegangsbehandling over to år med toggle AV ----
        fakeUnleash.enableAllExcept(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)

        val saksnummer = lagFørstegangsbehandling()

        mockServer.verify(
            1,
            WireMock.postRequestedFor(WireMock.urlEqualTo("/fakturaserier"))
                .withRequestBody(WireMock.matchingJsonPath("$.perioder[1]"))
        )

        // ---- 2. Toggle PÅ, ny vurdering avkorter til inneværende år, endringsvedtak ----
        fakeUnleash.enableAll()
        lagNyVurderingSomAvkorterTilInneværendeÅrOgFattVedtak(saksnummer)

        mockServer.verify(2, WireMock.postRequestedFor(WireMock.urlEqualTo("/fakturaserier")))

        // ---- 3. Årsavregningen for fjoråret har endelig avgift 0 og full kreditering, uten manuelle steg ----
        val årsavregningBehandling = hentAutomatiskOpprettetÅrsavregning(saksnummer)
        verifiserÅrsavregningMedEndeligAvgiftNull(årsavregningBehandling.id, forventetKreditering = fakturertForFjoråret)

        // ---- 4. Vedtak på årsavregningen: brev produseres og kreditnota sendes ----
        fattÅrsavregningsvedtakOgVerifiserKreditnota(årsavregningBehandling.id, forventetKreditering = fakturertForFjoråret)
    }

    @Test
    fun `ny vurdering som fjerner et allerede årsavregnet år overstyrer manuelt fastsatt avgift med 0 og krediterer`() {
        // ---- 1. Førstegangsbehandling over to år med toggle AV ----
        fakeUnleash.enableAllExcept(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)

        val saksnummer = lagFørstegangsbehandling()

        // ---- 2. Årsavregning for fjoråret fastsettes manuelt til 1500 og vedtas (1500 - 1000 = 500 faktureres) ----
        val manueltFastsattForFjoråret = BigDecimal(1500)
        val førsteÅrsavregningId = executeAndWait(
            mapOf(ProsessType.OPPRETT_NY_BEHANDLING_FOR_SAK to 1)
        ) {
            opprettBehandlingForSak.opprettBehandling(saksnummer, lagOpprettManuellÅrsavregningDto())
        }.hentBehandling.id

        val førsteÅrsavregning = årsavregningService.opprettÅrsavregning(førsteÅrsavregningId, fjoråret)
        årsavregningService.oppdater(
            førsteÅrsavregningId,
            førsteÅrsavregning.årsavregningID,
            beregnetAvgiftBelop = null,
            endeligAvgift = EndeligAvgiftValg.MANUELL_ENDELIG_AVGIFT,
            manueltAvgiftBeloep = manueltFastsattForFjoråret
        )

        executeAndWait(
            mapOf(
                ProsessType.IVERKSETT_VEDTAK_AARSAVREGNING to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        ) {
            vedtaksfattingFasade.fattVedtak(førsteÅrsavregningId, lagÅrsavregningsvedtak())
        }
        behandlingsresultatService.hentBehandlingsresultat(førsteÅrsavregningId).hentÅrsavregning().run {
            manueltAvgiftBeloep.shouldNotBeNull() shouldBeEqualComparingTo manueltFastsattForFjoråret
            tilFaktureringBeloep.shouldNotBeNull() shouldBeEqualComparingTo manueltFastsattForFjoråret - fakturertForFjoråret
        }
        sisteFakturaKall().let { body ->
            body["belop"].decimalValue() shouldBeEqualComparingTo manueltFastsattForFjoråret - fakturertForFjoråret
        }

        // ---- 3. Toggle PÅ, ny vurdering avkorter til inneværende år, endringsvedtak ----
        fakeUnleash.enableAll()
        lagNyVurderingSomAvkorterTilInneværendeÅrOgFattVedtak(saksnummer)

        // ---- 4. Ny årsavregning for fjoråret: det manuelle beløpet arves ikke, endelig avgift er 0 ----
        val andreÅrsavregning = hentAutomatiskOpprettetÅrsavregning(saksnummer)
        andreÅrsavregning.id shouldNotBe førsteÅrsavregningId
        verifiserÅrsavregningMedEndeligAvgiftNull(andreÅrsavregning.id, forventetKreditering = manueltFastsattForFjoråret)

        // ---- 5. Vedtak: kreditnota for hele det sist fastsatte beløpet ----
        fattÅrsavregningsvedtakOgVerifiserKreditnota(andreÅrsavregning.id, forventetKreditering = manueltFastsattForFjoråret)
    }

    private fun lagNyVurderingSomAvkorterTilInneværendeÅrOgFattVedtak(saksnummer: String) {
        val nyVurderingId = executeAndWait(
            mapOf(ProsessType.OPPRETT_REPLIKERT_BEHANDLING_FOR_SAK to 1)
        ) {
            opprettBehandlingForSak.opprettBehandling(saksnummer, lagOpprettNyVurderingDto())
        }.hentBehandling.id

        val replikertMedlemskapsperiodeId = behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(nyVurderingId)
            .medlemskapsperioder.single().hentId()
        medlemskapsperiodeService.oppdaterMedlemskapsperiode(
            nyVurderingId,
            replikertMedlemskapsperiodeId,
            periodeNyVurdering.fom,
            periodeNyVurdering.tom,
            InnvilgelsesResultat.INNVILGET,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
        )
        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
            nyVurderingId,
            lagSkatteforhold(periodeNyVurdering),
            lagInntektsperioder(periodeNyVurdering),
        ).shouldHaveSize(1)

        val endringsvedtak = FattVedtakRequest.Builder()
            .medBehandlingsresultatType(Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)
            .medVedtakstype(Vedtakstyper.ENDRINGSVEDTAK)
            .medBestillersId("komponent test")
            .build()

        // Endringsvedtaket erstatter fakturaserien og oppretter automatisk årsavregning for fjoråret
        executeAndWait(
            mapOf(
                ProsessType.IVERKSETT_VEDTAK_FTRL to 1,
                ProsessType.OPPRETT_NY_BEHANDLING_AARSAVREGNING to 1,
                // vedtaksbrev + innhentingsbrev for den automatisk opprettede årsavregningen (MELOSYS-8148)
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 2
            )
        ) {
            vedtaksfattingFasade.fattVedtak(nyVurderingId, endringsvedtak)
        }
    }

    private fun hentAutomatiskOpprettetÅrsavregning(saksnummer: String) =
        fagsakRepository.findBySaksnummer(saksnummer).shouldBePresent()
            .hentAktiveÅrsavregninger().single()
            .also { it.behandlingsårsak.shouldNotBeNull().type shouldBe Behandlingsaarsaktyper.AUTOMATISK_OPPRETTELSE }

    private fun verifiserÅrsavregningMedEndeligAvgiftNull(årsavregningBehandlingId: Long, forventetKreditering: BigDecimal) {
        behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(årsavregningBehandlingId).run {
            type shouldBe Behandlingsresultattyper.IKKE_FASTSATT
            withClue("Fjoråret er fjernet, så årsavregningen skal ikke ha noen avgiftspliktig periode") {
                medlemskapsperioder.shouldBeEmpty()
            }
            hentÅrsavregning().run {
                aar shouldBe fjoråret
                endeligAvgiftValg shouldBe EndeligAvgiftValg.OPPLYSNINGER_ENDRET
                manueltAvgiftBeloep.shouldBeNull()
                beregnetAvgiftBelop.shouldNotBeNull() shouldBeEqualComparingTo BigDecimal.ZERO
                tidligereFakturertBeloep.shouldNotBeNull() shouldBeEqualComparingTo forventetKreditering
                tilFaktureringBeloep.shouldNotBeNull() shouldBeEqualComparingTo forventetKreditering.negate()
            }
        }
    }

    private fun fattÅrsavregningsvedtakOgVerifiserKreditnota(årsavregningBehandlingId: Long, forventetKreditering: BigDecimal) {
        executeAndWait(
            mapOf(
                ProsessType.IVERKSETT_VEDTAK_AARSAVREGNING to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        ) {
            vedtaksfattingFasade.fattVedtak(årsavregningBehandlingId, lagÅrsavregningsvedtak())
        }

        behandlingRepository.findById(årsavregningBehandlingId).shouldBePresent().run {
            type shouldBe Behandlingstyper.ÅRSAVREGNING
            status shouldBe Behandlingsstatus.AVSLUTTET
        }
        behandlingsresultatService.hentBehandlingsresultat(årsavregningBehandlingId).run {
            type shouldBe Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            hentÅrsavregning().beregnetAvgiftBelop.shouldNotBeNull() shouldBeEqualComparingTo BigDecimal.ZERO
            hentÅrsavregning().tilFaktureringBeloep.shouldNotBeNull() shouldBeEqualComparingTo forventetKreditering.negate()
            fakturaserieReferanse shouldBe this@ÅrsavregningVedEndringIT.fakturaserieReferanse
        }

        val fakturaBody = sisteFakturaKall()
        withClue("Kreditnota for fjoråret skal være minus alt som sist ble fastsatt for året: $fakturaBody") {
            fakturaBody["belop"].decimalValue() shouldBeEqualComparingTo forventetKreditering.negate()
            fakturaBody["fakturaGjelderInnbetalingstype"].asText() shouldBe "AARSAVREGNING"
        }
    }

    private fun sisteFakturaKall(): JsonNode =
        jacksonObjectMapper().readTree(
            mockServer.findAll(WireMock.postRequestedFor(WireMock.urlEqualTo("/fakturaer"))).last().bodyAsString
        )

    private fun lagÅrsavregningsvedtak() = FattVedtakRequest.Builder()
        .medBehandlingsresultatType(Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT)
        .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
        .medBestillersId("komponent test")
        .build()

    private fun lagFørstegangsbehandling(): String {
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

        val mottatteOpplysninger = mottatteOpplysningerService.hentEllerOpprettMottatteOpplysninger(behandling.id, true)
            .shouldNotBeNull()
            .apply {
                mottatteOpplysningerData
                    .shouldBeInstanceOf<SøknadNorgeEllerUtenforEØS>()
                    .apply {
                        periode = periodeFørstegang
                        soeknadsland = Soeknadsland(listOf("AF"), false)
                        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE
                    }
            }
        mottatteOpplysningerService.oppdaterMottatteOpplysninger(behandling.id, mottatteOpplysninger.mottatteOpplysningerData.toJsonNode)
        oppfriskSaksopplysningerService.oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(behandling.id, false)

        avklartefaktaService.lagreAvklarteFakta(
            behandling.id,
            setOf(
                avklartFakta(Avklartefaktatyper.YRKESGRUPPE, "ORDINAER", "YRKESGRUPPE", null),
                avklartFakta(Avklartefaktatyper.VIRKSOMHET, "TRUE", "VIRKSOMHET", "999999999"),
                avklartFakta(Avklartefaktatyper.ARBEIDSLAND, "TRUE", "ARBEIDSLAND", "AF"),
            )
        )
        vilkaarsresultatService.registrerVilkår(
            behandling.id,
            listOf(
                Vilkaar.FTRL_2_1A_TRYGDEKOORDINGERING,
                Vilkaar.FTRL_FORUTGÅENDE_TRYGDETID,
                Vilkaar.FTRL_2_8_FØRSTE_LEDD_NÆR_TILKNYTNING_NORGE
            ).map { VilkaarDto().apply { vilkaar = it.kode; isOppfylt = true } }
        )

        // Forslaget deler søknadsperioden i flere perioder når den starter lenge før mottaksdato;
        // vi vil ha én innvilget periode over hele spennet, så vi beholder den første og sletter resten.
        val forslag = opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(
            behandling.id,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
        ).sortedBy { it.fom }
        forslag.drop(1).forEach { medlemskapsperiodeService.slettMedlemskapsperiode(behandling.id, it.hentId()) }
        val medlemskapsperiodeId = forslag.first().hentId()
        medlemskapsperiodeService.oppdaterMedlemskapsperiode(
            behandling.id,
            medlemskapsperiodeId,
            periodeFørstegang.fom,
            periodeFørstegang.tom,
            InnvilgelsesResultat.INNVILGET,
            Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8_FØRSTE_LEDD_A
        )
        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
            behandling.id,
            lagSkatteforhold(periodeFørstegang),
            lagInntektsperioder(periodeFørstegang),
        ).shouldHaveSize(2)

        val førstegangsvedtak = FattVedtakRequest.Builder()
            .medBehandlingsresultatType(Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medBestillersId("komponent test")
            .build()

        // Toggle er AV: ingen automatisk årsavregning for fjoråret her
        executeAndWait(
            mapOf(
                ProsessType.IVERKSETT_VEDTAK_FTRL to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        ) {
            vedtaksfattingFasade.fattVedtak(behandling.id, førstegangsvedtak)
        }

        return behandling.fagsak.saksnummer
    }

    private fun avklartFakta(type: Avklartefaktatyper, verdi: String, navn: String, subjekt: String?) =
        AvklartefaktaDto(listOf(verdi), navn).apply {
            avklartefaktaType = type
            subjektID = subjekt
            begrunnelseKoder = emptyList()
            begrunnelseFritekst = null
        }

    private fun lagSkatteforhold(periode: Periode) = listOf(
        SkatteforholdTilNorge().apply {
            fomDato = periode.fom
            tomDato = periode.tom
            skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
        }
    )

    private fun lagInntektsperioder(periode: Periode) = listOf(
        Inntektsperiode().apply {
            fomDato = periode.fom
            tomDato = periode.tom
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            isArbeidsgiversavgiftBetalesTilSkatt = false
            avgiftspliktigMndInntekt = Penger(10000.toBigDecimal())
            avgiftspliktigTotalinntekt = Penger(10000.toBigDecimal())
        }
    )

    private fun lagOpprettManuellÅrsavregningDto() = OpprettSakDto().apply {
        sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG
        sakstype = Sakstyper.FTRL
        behandlingstema = Behandlingstema.YRKESAKTIV
        behandlingstype = Behandlingstyper.ÅRSAVREGNING
        mottaksdato = LocalDate.now()
        behandlingsaarsakType = Behandlingsaarsaktyper.HENVENDELSE
    }

    private fun lagOpprettNyVurderingDto() = OpprettSakDto().apply {
        behandlingstema = Behandlingstema.YRKESAKTIV
        behandlingstype = Behandlingstyper.NY_VURDERING
        mottaksdato = LocalDate.now()
        behandlingsaarsakType = Behandlingsaarsaktyper.SØKNAD
    }
}
