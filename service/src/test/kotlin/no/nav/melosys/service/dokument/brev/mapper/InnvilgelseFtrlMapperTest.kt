package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.commons.foedselsnummer.Kjoenn
import no.nav.commons.foedselsnummer.testutils.FoedselsnummerGenerator
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfoNorge
import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfoUtland
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.mottatteopplysninger.data.IdentType
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie
import no.nav.melosys.domain.person.familie.OmfattetFamilie
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.dokument.DokgenTestData
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils
import org.assertj.core.api.Assertions
import org.assertj.core.api.Condition
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.mockito.stubbing.Answer
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@ExtendWith(MockitoExtension::class)
internal class InnvilgelseFtrlMapperTest {
    @Mock
    private val mockTrygdeavgiftsgrunnlagService: TrygdeavgiftsgrunnlagService? = null

    @Mock
    private val mockAvklarteVirksomheterService: AvklarteVirksomheterService? = null

    @Mock
    private val mockAvklarteMedfolgendeFamilieService: AvklarteMedfolgendeFamilieService? = null


    @Mock
    private val mockDokgenMapperDatahenter: DokgenMapperDatahenter? = null
    private val foedselsnummerGenerator = FoedselsnummerGenerator()
    private val EKTEFELLE_FNR = foedselsnummerGenerator.foedselsnummer(null, Kjoenn.KVINNE, true).asString
    private val BARN1_FNR = foedselsnummerGenerator.foedselsnummer(null, Kjoenn.MANN, false).asString
    private var innvilgelseFtrlMapper: InnvilgelseFtrlMapper? = null
    @BeforeEach
    fun setup() {
        innvilgelseFtrlMapper = InnvilgelseFtrlMapper(
            mockTrygdeavgiftsgrunnlagService!!,
            mockAvklarteVirksomheterService!!,
            mockAvklarteMedfolgendeFamilieService!!,
            mockDokgenMapperDatahenter!!
        )
    }

    @Test
    fun map_InnvilgetMedOmfattetFamilieKunNorskInntektFullstendigInnvilget_populererFelter() {
        mockHappyCase()

        innvilgelseFtrlMapper!!.map(lagInnvilgelseBrevbestilling()).shouldNotBeNull()
            .apply {
                datoMottatt.shouldBe(LocalDate.EPOCH)
                perioder.shouldHaveSize(1)
                isErFullstendigInnvilget.shouldBeTrue()
                ftrl_2_8_begrunnelse.shouldBe(Ftrl_2_8_naer_tilknytning_norge_begrunnelser.ANSATT_I_NORSK_VIRKSOMHET_IKKE_UTSENDT.kode)
                isVurderingMedlemskapEktefelle.shouldBeTrue()
                isVurderingLovvalgBarn.shouldBeTrue()
                omfattetFamilie.shouldHaveSize(2).sortedBy { it.navn }.apply {
                    first().apply {
                        ident.shouldBe(BARN1_FNR)
                        navn.shouldBe(BARN1_NAVN)
                        identType.shouldBe(IdentType.FNR)
                    }
                    last().apply {
                        ident.shouldBe(EKTEFELLE_FNR)
                        navn.shouldBe(EKTEFELLE_NAVN)
                        identType.shouldBe(IdentType.DNR)
                    }
                }
                ikkeOmfattetBarn.shouldBeEmpty()
                ikkeOmfattetEktefelle.shouldBeNull()
                innvilgelse.apply {
                    innledningFritekst().shouldBeNull()
                    begrunnelseFritekst().shouldBe(BEGRUNNELSE_FRITEKST)
                    ektefelleFritekst().shouldBeNull()
                    barnFritekst().shouldBeNull()
                }
                saksbehandlerNavn.shouldBe(SAKSBEHANDLER_NAVN)
                arbeidsgiverNavn.shouldBe(ARBEIDSGIVER_NAVN)
                arbeidsland.shouldBe(Landkoder.AT.beskrivelse)
                isTrygdeavtaleMedArbeidsland.shouldBeTrue()
                vurderingTrygdeavgift.shouldNotBeNull().apply {
                    selvbetalende.shouldBeTrue()
                    representantNavn.shouldBeNull()
                    utenlandsk.shouldBeNull()
                    norsk.shouldNotBeNull().apply {
                        avgiftspliktigInntektMd() shouldBe 50000
                        trygdeavgiftNav().shouldBeFalse()
                        erSkattepliktig().shouldBeTrue()
                        arbeidsgiverBetalerAvgift().shouldBeTrue()
                        saerligeavgiftsgruppe().shouldBeNull()
                    }
                }
                loennsforhold.shouldBe(Loenn_forhold.LØNN_FRA_NORGE.kode)
                arbeidsgiverFullmektigNavn.shouldBeNull()
                isBrukerHarFullmektig.shouldBeFalse()
                avgiftssatsAar.shouldBe(DateTime.now().year.toString())
                isLoennNorgeSkattepliktig.shouldBeTrue()
                isLoennUtlandSkattepliktig.shouldBeFalse()
                saksinfo.apply {
                    fnr().shouldBe(DokgenTestData.FNR_BRUKER)
                    saksnummer().shouldBe(SAKSNUMMER)
                    navnBruker().shouldBe(DokgenTestData.SAMMENSATT_NAVN_BRUKER)
                }
                dagensDato.truncatedTo(ChronoUnit.DAYS).shouldBe(Instant.now().truncatedTo(ChronoUnit.DAYS))
                mottaker.apply {
                    adresselinjer().shouldNotBeEmpty()
                    postnr().shouldBe(DokgenTestData.POSTNR_BRUKER)
                    poststed().shouldBe(DokgenTestData.POSTSTED_BRUKER)
                    land().shouldBeNull()
                }
            }
    }

    @Test
    fun map_InnvilgetMedIkkeOmfattetFamilie_populererFelter() {
        mockHappyCase()
        whenever(mockAvklarteMedfolgendeFamilieService!!.hentAvklartMedfølgendeEktefelle(ArgumentMatchers.anyLong()))
            .thenReturn(lagAvklartIkkeMedfølgendeEktefelle())
        whenever(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(ArgumentMatchers.anyLong()))
            .thenReturn(lagAvklartIkkeMedfølgendeBarn())
        val innvilgelseFtrl = innvilgelseFtrlMapper!!.map(lagInnvilgelseBrevbestilling())
        Assertions.assertThat(innvilgelseFtrl.omfattetFamilie).isEmpty()
        Assertions.assertThat(innvilgelseFtrl.ikkeOmfattetBarn).hasSize(2)
        for (familiemedlemInfo in innvilgelseFtrl.ikkeOmfattetBarn) {
            Assertions.assertThat(familiemedlemInfo.info().ident()).`is`(
                Condition(
                    { s: String -> java.util.List.of(BARN1_FNR, BARN2_FNR).contains(s) }, "fnr"
                )
            )
            Assertions.assertThat(familiemedlemInfo.info().navn()).`is`(
                Condition(
                    { s: String -> java.util.List.of(BARN1_NAVN, BARN2_NAVN).contains(s) }, "navn"
                )
            )
            Assertions.assertThat(familiemedlemInfo.info().identType()).`is`(
                Condition(
                    { s: IdentType -> java.util.List.of(IdentType.FNR, IdentType.DATO).contains(s) }, "identType"
                )
            )
            Assertions.assertThat(familiemedlemInfo.begrunnelse().kode)
                .isEqualTo(Medfolgende_barn_begrunnelser_ftrl.IKKE_SOEKERS_BARN.kode)
        }
        Assertions.assertThat(innvilgelseFtrl.ikkeOmfattetEktefelle).isNotNull
        Assertions.assertThat(innvilgelseFtrl.ikkeOmfattetEktefelle)
            .extracting("info.ident", "info.navn", "info.identType", "begrunnelse")
            .containsExactly(
                EKTEFELLE_FNR,
                EKTEFELLE_NAVN,
                IdentType.DNR,
                Medfolgende_ektefelle_samboer_begrunnelser_ftrl.IKKE_TRE_AV_FEM_SISTE_ÅR.kode
            )
    }

    @Test
    fun map_InnvilgetMedUtenlandskInntekt_harTrygdeavtaleMedLand_populererFelter() {
        mockHappyCase()
        whenever(mockTrygdeavgiftsgrunnlagService!!.hentAvgiftsgrunnlag(ArgumentMatchers.anyLong()))
            .thenReturn(lagUtenlandskTrygdeAvgiftsgrunnlag())
        val behandlingsresultat = lagBehandlingsResultat()
        behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData.soeknadsland =
            Soeknadsland(java.util.List.of("GB"), false)
        whenever(mockDokgenMapperDatahenter!!.hentBehandlingsresultat(ArgumentMatchers.anyLong()))
            .thenReturn(behandlingsresultat)
        val innvilgelseFtrl = innvilgelseFtrlMapper!!.map(lagInnvilgelseBrevbestilling())
        Assertions.assertThat(innvilgelseFtrl.isTrygdeavtaleMedArbeidsland).isTrue
        Assertions.assertThat(innvilgelseFtrl.vurderingTrygdeavgift).isNotNull
        Assertions.assertThat(innvilgelseFtrl.vurderingTrygdeavgift.norsk).isNull()
        val trygdeavgiftInfoUtenlandsk = innvilgelseFtrl.vurderingTrygdeavgift.utenlandsk
        Assertions.assertThat(trygdeavgiftInfoUtenlandsk).isNotNull
        Assertions.assertThat(trygdeavgiftInfoUtenlandsk?.avgiftspliktigInntektMd()).isEqualTo(50000)
        Assertions.assertThat(trygdeavgiftInfoUtenlandsk?.trygdeavgiftNav()).isTrue
        Assertions.assertThat(trygdeavgiftInfoUtenlandsk?.erSkattepliktig()).isTrue
        Assertions.assertThat(trygdeavgiftInfoUtenlandsk?.arbeidsgiverBetalerAvgift()).isFalse
        Assertions.assertThat(trygdeavgiftInfoUtenlandsk?.saerligeavgiftsgruppe())
            .isEqualTo(Saerligeavgiftsgrupper.ARBEIDSTAKER_MALAYSIA.kode)
    }

    @Test
    fun map_delvisInnvilget_populererFelter() {
        mockHappyCase()
        val delvisInnvilgetPeriode = Medlemskapsperiode()
        delvisInnvilgetPeriode.bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
        delvisInnvilgetPeriode.innvilgelsesresultat = InnvilgelsesResultat.DELVIS_INNVILGET
        delvisInnvilgetPeriode.medlemskapstype = Medlemskapstyper.FRIVILLIG
        delvisInnvilgetPeriode.setTrygdedekning(Trygdedekninger.HELSEDEL)
        val behandlingsresultat = lagBehandlingsResultat()
        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden
        medlemAvFolketrygden.medlemskapsperioder = java.util.List.of(
            medlemAvFolketrygden.medlemskapsperioder.iterator().next(),
            delvisInnvilgetPeriode
        )
        whenever(mockDokgenMapperDatahenter!!.hentBehandlingsresultat(ArgumentMatchers.anyLong()))
            .thenReturn(behandlingsresultat)
        val innvilgelseFtrl = innvilgelseFtrlMapper!!.map(lagInnvilgelseBrevbestilling())
        Assertions.assertThat(innvilgelseFtrl.perioder).hasSize(2)
        Assertions.assertThat(innvilgelseFtrl.isErFullstendigInnvilget).isFalse
    }

    private fun lagInnvilgelseBrevbestilling(): InnvilgelseBrevbestilling {
        return InnvilgelseBrevbestilling.Builder()
            .medBehandling(DokgenTestData.lagBehandling())
            .medPersonDokument(DokgenTestData.lagPersondata())
            .medPersonMottaker(DokgenTestData.lagPersondata())
            .medForsendelseMottatt(Instant.EPOCH)
            .medBegrunnelseFritekst(BEGRUNNELSE_FRITEKST)
            .medSaksbehandlerNavn(SAKSBEHANDLER_NAVN)
            .build()
    }

    private fun lagNorskTrygdeAvgiftsgrunnlag(): Trygdeavgiftsgrunnlag {
        return Trygdeavgiftsgrunnlag(Loenn_forhold.LØNN_FRA_NORGE, lagAvgiftsGrunnlagNorge(), null)
    }

    private fun lagUtenlandskTrygdeAvgiftsgrunnlag(): Trygdeavgiftsgrunnlag {
        return Trygdeavgiftsgrunnlag(Loenn_forhold.LØNN_FRA_UTLANDET, null, lagAvgiftsGrunnlagUtland())
    }

    private fun lagAvgiftsGrunnlagNorge(): AvgiftsgrunnlagInfoNorge {
        return AvgiftsgrunnlagInfoNorge(
            true, true, null,
            Vurderingsutfall_trygdeavgift_norsk_inntekt.NORSK_INNTEKT_TRYGDEAVGIFT_NAV
        )
    }

    private fun lagAvgiftsGrunnlagUtland(): AvgiftsgrunnlagInfoUtland {
        return AvgiftsgrunnlagInfoUtland(
            true, false, Saerligeavgiftsgrupper.ARBEIDSTAKER_MALAYSIA,
            Vurderingsutfall_trygdeavgift_utenlandsk_inntekt.UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV
        )
    }

    private fun lagBehandlingsResultat(): Behandlingsresultat {
        val behandlingsresultat = Behandlingsresultat()
        behandlingsresultat.medlemAvFolketrygden = lagMedlemAvFolketrygden()
        val vilkaarsresultat = Vilkaarsresultat()
        vilkaarsresultat.vilkaar = Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE
        val vilkaarBegrunnelse = VilkaarBegrunnelse()
        vilkaarBegrunnelse.kode =
            Ftrl_2_8_naer_tilknytning_norge_begrunnelser.ANSATT_I_NORSK_VIRKSOMHET_IKKE_UTSENDT.kode
        vilkaarsresultat.begrunnelser = setOf(vilkaarBegrunnelse)
        behandlingsresultat.vilkaarsresultater = setOf(vilkaarsresultat)
        behandlingsresultat.behandling = DokgenTestData.lagBehandling()
        return behandlingsresultat
    }

    private fun lagAvklarteVirksomheter(): List<AvklartVirksomhet> {
        return java.util.List.of(
            AvklartVirksomhet(
                ARBEIDSGIVER_NAVN,
                "987654321",
                BrevDataTestUtils.lagStrukturertAdresse(),
                Yrkesaktivitetstyper.LOENNET_ARBEID
            )
        )
    }

    private fun lagAvklartMedfølgendeBarn(): AvklarteMedfolgendeFamilie {
        return AvklarteMedfolgendeFamilie(setOf(OmfattetFamilie(UUID_BARN_1)), setOf())
    }

    private fun lagAvklartMedfølgendeEktefelle(): AvklarteMedfolgendeFamilie {
        val ektefelle = OmfattetFamilie(UUID_EKTEFELLE)
        return AvklarteMedfolgendeFamilie(setOf(ektefelle), setOf())
    }

    private fun lagAvklartIkkeMedfølgendeBarn(): AvklarteMedfolgendeFamilie {
        val barn1 =
            IkkeOmfattetFamilie(UUID_BARN_1, Medfolgende_barn_begrunnelser_ftrl.IKKE_SOEKERS_BARN.kode, "Ikke omfattet")
        val barn2 =
            IkkeOmfattetFamilie(UUID_BARN_2, Medfolgende_barn_begrunnelser_ftrl.IKKE_SOEKERS_BARN.kode, "Ikke omfattet")
        return AvklarteMedfolgendeFamilie(setOf(), setOf(barn1, barn2))
    }

    private fun lagAvklartIkkeMedfølgendeEktefelle(): AvklarteMedfolgendeFamilie {
        val ektefelle = IkkeOmfattetFamilie(
            UUID_EKTEFELLE,
            Medfolgende_ektefelle_samboer_begrunnelser_ftrl.IKKE_TRE_AV_FEM_SISTE_ÅR.kode,
            "Ikke omfattet"
        )
        return AvklarteMedfolgendeFamilie(setOf(), setOf(ektefelle))
    }

    private fun lagMedfølgendeEktefelle(): Map<String, MedfolgendeFamilie> {
        val ektefelle = MedfolgendeFamilie.tilMedfolgendeFamilie(
            UUID_EKTEFELLE,
            EKTEFELLE_FNR,
            EKTEFELLE_NAVN,
            MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER
        )
        return java.util.Map.of(UUID_EKTEFELLE, ektefelle)
    }

    private fun lagMedfølgendeBarn(): Map<String, MedfolgendeFamilie> {
        val medfolgendeBarn1 = MedfolgendeFamilie.tilMedfolgendeFamilie(
            UUID_BARN_1,
            BARN1_FNR,
            BARN1_NAVN,
            MedfolgendeFamilie.Relasjonsrolle.BARN
        )
        val medfolgendeBarn2 = MedfolgendeFamilie.tilMedfolgendeFamilie(
            UUID_BARN_2,
            BARN2_FNR,
            BARN2_NAVN,
            MedfolgendeFamilie.Relasjonsrolle.BARN
        )
        return java.util.Map.of(UUID_BARN_1, medfolgendeBarn1, UUID_BARN_2, medfolgendeBarn2)
    }

    private fun lagMedlemAvFolketrygden(): MedlemAvFolketrygden {
        val medlemAvFolketrygden = MedlemAvFolketrygden()
        medlemAvFolketrygden.medlemskapsperioder = lagMedlemskapsperioder()
        medlemAvFolketrygden.fastsattTrygdeavgift = lagFastsattTrygdeavgift()
        return medlemAvFolketrygden
    }

    private fun lagMedlemskapsperioder(): List<Medlemskapsperiode> {
        val periode1 = Medlemskapsperiode()
        periode1.bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
        periode1.innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        periode1.medlemskapstype = Medlemskapstyper.FRIVILLIG
        periode1.setTrygdedekning(Trygdedekninger.HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER)
        return java.util.List.of(periode1)
    }

    private fun lagFastsattTrygdeavgift(): FastsattTrygdeavgift {
        val fastsattTrygdeavgift = FastsattTrygdeavgift()
        fastsattTrygdeavgift.avgiftspliktigNorskInntektMnd = 50000L
        fastsattTrygdeavgift.avgiftspliktigUtenlandskInntektMnd = 50000L
        fastsattTrygdeavgift.betalesAv = lagBetalesAv()
        fastsattTrygdeavgift.representantNr = "1234"
        return fastsattTrygdeavgift
    }

    private fun lagBetalesAv(): Aktoer {
        val aktoer = Aktoer()
        aktoer.rolle = Aktoersroller.REPRESENTANT_TRYGDEAVGIFT
        return aktoer
    }

    private fun mockHappyCase() {
        whenever(mockAvklarteMedfolgendeFamilieService!!.hentAvklartMedfølgendeEktefelle(ArgumentMatchers.anyLong()))
            .thenReturn(lagAvklartMedfølgendeEktefelle())
        whenever(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(ArgumentMatchers.anyLong()))
            .thenReturn(lagAvklartMedfølgendeBarn())
        whenever(mockTrygdeavgiftsgrunnlagService!!.hentAvgiftsgrunnlag(ArgumentMatchers.anyLong()))
            .thenReturn(lagNorskTrygdeAvgiftsgrunnlag())
        whenever(
            mockAvklarteVirksomheterService!!.hentNorskeArbeidsgivere(
                ArgumentMatchers.any(
                    Behandling::class.java
                )
            )
        ).thenReturn(lagAvklarteVirksomheter())
        whenever(mockAvklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(ArgumentMatchers.anyLong()))
            .thenReturn(lagMedfølgendeEktefelle())
        whenever(mockAvklarteMedfolgendeFamilieService.hentMedfølgendeBarn(ArgumentMatchers.anyLong()))
            .thenReturn(lagMedfølgendeBarn())
        whenever(mockDokgenMapperDatahenter!!.hentBehandlingsresultat(ArgumentMatchers.anyLong()))
            .thenReturn(lagBehandlingsResultat())
        whenever(mockDokgenMapperDatahenter.hentLandnavnFraLandkode(ArgumentMatchers.anyString())).thenAnswer(
            Answer { invocationOnMock: InvocationOnMock ->
                Trygdeavtale_myndighetsland.valueOf(
                    invocationOnMock.getArgument(
                        0
                    )
                ).beskrivelse
            } as Answer<String>)
        whenever(mockDokgenMapperDatahenter.hentSammensattNavn(ArgumentMatchers.anyString())).thenAnswer { invocationOnMock: InvocationOnMock ->
            val fnr = invocationOnMock.getArgument<String>(0)
            var navn: String? = null
            if (fnr == EKTEFELLE_FNR) {
                navn = EKTEFELLE_NAVN
            } else if (fnr == BARN1_FNR) {
                navn = BARN1_NAVN
            } else if (fnr == BARN2_FNR) {
                navn = BARN2_NAVN
            }
            navn
        }
    }

    companion object {
        private const val UUID_EKTEFELLE = "uuidEktefelle"
        private const val UUID_BARN_1 = "uuidBarn1"
        private const val UUID_BARN_2 = "uuidBarn2"
        private const val BARN2_FNR = "02.05.11"
        const val BEGRUNNELSE_FRITEKST = "Begrunnelse fritekst"
        const val SAKSBEHANDLER_NAVN = "Fetter Anton"
        const val ARBEIDSGIVER_NAVN = "Bang Hansen"
        const val SAKSNUMMER = "MEL-123"
        const val EKTEFELLE_NAVN = "Dolly Duck"
        const val BARN1_NAVN = "Doffen Duck"
        const val BARN2_NAVN = "Ole Duck"
    }
}
