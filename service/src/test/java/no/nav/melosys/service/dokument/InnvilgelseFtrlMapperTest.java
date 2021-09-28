package no.nav.melosys.service.dokument;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfoNorge;
import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfoUtland;
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.adresse.UstrukturertAdresse;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.domain.person.familie.*;
import no.nav.melosys.integrasjon.dokgen.dto.InnvilgelseFtrl;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.FamiliemedlemInfo;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.TrygdeavgiftInfo;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.representant.RepresentantService;
import no.nav.melosys.service.representant.dto.RepresentantDataDto;
import org.assertj.core.api.Condition;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static no.nav.melosys.domain.kodeverk.Loenn_forhold.LØNN_FRA_NORGE;
import static no.nav.melosys.domain.kodeverk.Loenn_forhold.LØNN_FRA_UTLANDET;
import static no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser.ANSATT_I_NORSK_VIRKSOMHET_IKKE_UTSENDT;
import static no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl.IKKE_SOEKERS_BARN;
import static no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl.IKKE_TRE_AV_FEM_SISTE_ÅR;
import static no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.IdentType.FNR;
import static org.assertj.core.api.Assertions.anyOf;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InnvilgelseFtrlMapperTest {

    public static final String UUID_EKTEFELLE = "uuidEktefelle";
    public static final String UUID_BARN_1 = "uuidBarn1";
    public static final String EKTEFELLE_FNR = "09080723451";
    private static final String BARN1_FNR = "12131456789";
    public static final String BEGRUNNELSE_FRITEKST = "Begrunnelse fritekst";
    public static final String SAKSBEHANDLER_NAVN = "Fetter Anton";
    public static final String ARBEIDSGIVER_NAVN = "Bang Hansen";
    public static final String FNR_BRUKER = "05058892382";
    public static final String SAKSNUMMER = "MEL-123";
    public static final String NAVN_BRUKER = "Donald Duck";
    public static final String POSTNR_BRUKER = "9999";
    public static final String POSTSTED_BRUKER = "Andeby";
    public static final String EKTEFELLE_NAVN = "Dolly Duck";
    public static final String BARN1_NAVN = "Doffen Duck";
    public static final String REPRESENTANT_NAVN = "Representant AS";
    @Mock
    private PersondataFasade mockPersondataFasade;

    @Mock
    private TrygdeavgiftsgrunnlagService mockTrygdeavgiftsgrunnlagService;

    @Mock
    private BehandlingsresultatService mockBehandlingsresultatService;

    @Mock
    private AvklarteVirksomheterService mockAvklarteVirksomheterService;

    @Mock
    private AvklarteMedfolgendeFamilieService mockAvklarteMedfolgendeFamilieService;

    @Mock
    private LandvelgerService mockLandvelgerService;

    @Mock
    private RepresentantService mockRepresentantService;

    @Mock
    private EregFasade mockEregFasade;

    private InnvilgelseFtrlMapper innvilgelseFtrlMapper;

    @BeforeEach
    void setup() {
        innvilgelseFtrlMapper = new InnvilgelseFtrlMapper(mockPersondataFasade, mockTrygdeavgiftsgrunnlagService,
            mockBehandlingsresultatService, mockAvklarteVirksomheterService,
            mockAvklarteMedfolgendeFamilieService, mockLandvelgerService, mockRepresentantService, mockEregFasade);
    }

    @Test
    void map_InnvilgetMedOmfattetFamilieKunNorskInntektFullstendigInnvilget_populererFelter() {
        mockHappyCase();

        InnvilgelseFtrl innvilgelseFtrl = innvilgelseFtrlMapper.map(lagInnvilgelseBrevbestilling());

        assertThat(innvilgelseFtrl).isNotNull();
        assertThat(innvilgelseFtrl.getDatoMottatt()).isEqualTo(Instant.EPOCH);
        assertThat(innvilgelseFtrl.getPerioder().size()).isEqualTo(1);
        assertThat(innvilgelseFtrl.isErFullstendigInnvilget()).isTrue();
        assertThat(innvilgelseFtrl.getFtrl_2_8_begrunnelse()).isEqualTo(ANSATT_I_NORSK_VIRKSOMHET_IKKE_UTSENDT.getKode());
        assertThat(innvilgelseFtrl.isVurderingMedlemskapEktefelle()).isTrue();
        assertThat(innvilgelseFtrl.isVurderingLovvalgBarn()).isTrue();
        assertThat(innvilgelseFtrl.getOmfattetFamilie().size()).isEqualTo(2);
        for (FamiliemedlemInfo familiemedlemInfo : innvilgelseFtrl.getOmfattetFamilie()) {
            assertThat(familiemedlemInfo.ident()).is(new Condition<>(s -> List.of(EKTEFELLE_FNR, BARN1_FNR).contains(s), "fnr"));
            assertThat(familiemedlemInfo.navn()).is(new Condition<>(s -> List.of(EKTEFELLE_NAVN, BARN1_NAVN).contains(s), "navn"));
            assertThat(familiemedlemInfo.identType()).isEqualTo(FNR);
        }
        assertThat(innvilgelseFtrl.getIkkeOmfattetBarn().size()).isZero();
        assertThat(innvilgelseFtrl.getIkkeOmfattetEktefelle()).isNull();
        assertThat(innvilgelseFtrl.getFritekstInnledning()).isNull();
        assertThat(innvilgelseFtrl.getFritekstBegrunnelse()).isEqualTo(BEGRUNNELSE_FRITEKST);
        assertThat(innvilgelseFtrl.getFritekstEktefelle()).isNull();
        assertThat(innvilgelseFtrl.getFritekstBarn()).isNull();
        assertThat(innvilgelseFtrl.getSaksbehandlerNavn()).isEqualTo(SAKSBEHANDLER_NAVN);
        assertThat(innvilgelseFtrl.getArbeidsgiverNavn()).isEqualTo(ARBEIDSGIVER_NAVN);
        assertThat(innvilgelseFtrl.getArbeidsland()).isEqualTo(Landkoder.AT.getBeskrivelse());
        assertThat(innvilgelseFtrl.isTrygdeavtaleMedArbeidsland()).isFalse();
        assertThat(innvilgelseFtrl.getVurderingTrygdeavgift()).isNotNull();
        assertThat(innvilgelseFtrl.getVurderingTrygdeavgift().selvbetalende()).isFalse();
        assertThat(innvilgelseFtrl.getVurderingTrygdeavgift().representantNavn()).isEqualTo(REPRESENTANT_NAVN);
        assertThat(innvilgelseFtrl.getVurderingTrygdeavgift().utenlandsk()).isNull();

        TrygdeavgiftInfo trygdeavgiftInfoNorsk = innvilgelseFtrl.getVurderingTrygdeavgift().norsk();
        assertThat(trygdeavgiftInfoNorsk).isNotNull();
        assertThat(trygdeavgiftInfoNorsk.avgiftspliktigInntektMd()).isEqualTo(50000);
        assertThat(trygdeavgiftInfoNorsk.trygdeavgiftNav()).isFalse();
        assertThat(trygdeavgiftInfoNorsk.erSkattepliktig()).isTrue();
        assertThat(trygdeavgiftInfoNorsk.arbeidsgiverBetalerAvgift()).isTrue();
        assertThat(trygdeavgiftInfoNorsk.saerligeavgiftsgruppe()).isNull();

        assertThat(innvilgelseFtrl.getLoennsforhold()).isEqualTo(LØNN_FRA_NORGE.getKode());
        assertThat(innvilgelseFtrl.getArbeidsgiverFullmektigNavn()).isNull();
        assertThat(innvilgelseFtrl.isBrukerHarFullmektig()).isFalse();
        assertThat(innvilgelseFtrl.getAvgiftssatsAar()).isEqualTo(String.valueOf(DateTime.now().getYear()));
        assertThat(innvilgelseFtrl.isLoennNorgeSkattepliktig()).isTrue();
        assertThat(innvilgelseFtrl.isLoennUtlandSkattepliktig()).isFalse();
        assertThat(innvilgelseFtrl.getFnr()).isEqualTo(FNR_BRUKER);
        assertThat(innvilgelseFtrl.getSaksnummer()).isEqualTo(SAKSNUMMER);
        assertThat(innvilgelseFtrl.getDagensDato().truncatedTo(ChronoUnit.DAYS)).isEqualTo(Instant.now().truncatedTo(ChronoUnit.DAYS));
        assertThat(innvilgelseFtrl.getNavnBruker()).isEqualTo(NAVN_BRUKER);
        assertThat(innvilgelseFtrl.getAdresselinjer().isEmpty()).isFalse();
        assertThat(innvilgelseFtrl.getPostnr()).isEqualTo(POSTNR_BRUKER);
        assertThat(innvilgelseFtrl.getPoststed()).isEqualTo(POSTSTED_BRUKER);
        assertThat(innvilgelseFtrl.getLand()).isNull();
    }

    @Test
    void map_InnvilgetMedIkkeOmfattetFamilie_populererFelter() {
        mockHappyCase();
        when(mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(anyLong())).thenReturn(lagAvklartIkkeMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(lagAvklartIkkeMedfølgendeBarn());

        InnvilgelseFtrl innvilgelseFtrl = innvilgelseFtrlMapper.map(lagInnvilgelseBrevbestilling());

        assertThat(innvilgelseFtrl.getOmfattetFamilie().size()).isZero();

        assertThat(innvilgelseFtrl.getIkkeOmfattetBarn().size()).isEqualTo(1);
        assertThat(innvilgelseFtrl.getIkkeOmfattetBarn().get(0))
            .extracting("info.ident", "info.navn", "begrunnelse.kode")
            .containsExactly(BARN1_FNR, BARN1_NAVN, IKKE_SOEKERS_BARN.getKode());

        assertThat(innvilgelseFtrl.getIkkeOmfattetEktefelle()).isNotNull();
        assertThat(innvilgelseFtrl.getIkkeOmfattetEktefelle())
            .extracting("info.ident", "info.navn", "begrunnelse")
            .containsExactly(EKTEFELLE_FNR, EKTEFELLE_NAVN, IKKE_TRE_AV_FEM_SISTE_ÅR.getKode());
    }

    @Test
    void map_InnvilgetMedUtenlandskInntekt_harTrygdeavtaleMedLand_populererFelter() {
        mockHappyCase();
        when(mockTrygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(anyLong())).thenReturn(lagUtenlandskTrygdeAvgiftsgrunnlag());
        when(mockLandvelgerService.hentArbeidsland(anyLong())).thenReturn(Landkoder.GB);

        InnvilgelseFtrl innvilgelseFtrl = innvilgelseFtrlMapper.map(lagInnvilgelseBrevbestilling());

        assertThat(innvilgelseFtrl.isTrygdeavtaleMedArbeidsland()).isTrue();

        assertThat(innvilgelseFtrl.getVurderingTrygdeavgift()).isNotNull();
        assertThat(innvilgelseFtrl.getVurderingTrygdeavgift().norsk()).isNull();

        TrygdeavgiftInfo trygdeavgiftInfoUtenlandsk = innvilgelseFtrl.getVurderingTrygdeavgift().utenlandsk();
        assertThat(trygdeavgiftInfoUtenlandsk).isNotNull();
        assertThat(trygdeavgiftInfoUtenlandsk.avgiftspliktigInntektMd()).isEqualTo(50000);
        assertThat(trygdeavgiftInfoUtenlandsk.trygdeavgiftNav()).isTrue();
        assertThat(trygdeavgiftInfoUtenlandsk.erSkattepliktig()).isTrue();
        assertThat(trygdeavgiftInfoUtenlandsk.arbeidsgiverBetalerAvgift()).isFalse();
        assertThat(trygdeavgiftInfoUtenlandsk.saerligeavgiftsgruppe()).isEqualTo(Saerligeavgiftsgrupper.ARBEIDSTAKER_MALAYSIA.getKode());
    }

    @Test
    void map_delvisInnvilget_populererFelter() {
        mockHappyCase();
        Medlemskapsperiode delvisInnvilgetPeriode = new Medlemskapsperiode();
        delvisInnvilgetPeriode.setBestemmelse(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8);
        delvisInnvilgetPeriode.setInnvilgelsesresultat(InnvilgelsesResultat.DELVIS_INNVILGET);
        delvisInnvilgetPeriode.setMedlemskapstype(Medlemskapstyper.FRIVILLIG);
        delvisInnvilgetPeriode.setTrygdedekning(Trygdedekninger.HELSEDEL);

        Behandlingsresultat behandlingsresultat = lagBehandlingsResultat();
        MedlemAvFolketrygden medlemAvFolketrygden = behandlingsresultat.getMedlemAvFolketrygden();
        medlemAvFolketrygden.setMedlemskapsperioder(List.of(medlemAvFolketrygden.getMedlemskapsperioder().iterator().next(), delvisInnvilgetPeriode));

        when(mockBehandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        InnvilgelseFtrl innvilgelseFtrl = innvilgelseFtrlMapper.map(lagInnvilgelseBrevbestilling());

        assertThat(innvilgelseFtrl.getPerioder().size()).isEqualTo(2);
        assertThat(innvilgelseFtrl.isErFullstendigInnvilget()).isFalse();
    }

    private InnvilgelseBrevbestilling lagInnvilgelseBrevbestilling() {
        return new InnvilgelseBrevbestilling.Builder()
            .medBehandling(lagBehandling())
            .medPersonDokument(lagPersonDokument())
            .medForsendelseMottatt(Instant.EPOCH)
            .medBegrunnelseFritekst(BEGRUNNELSE_FRITEKST)
            .medSaksbehandlerNavn(SAKSBEHANDLER_NAVN)
            .build();
    }

    private PersonDokument lagPersonDokument() {
        PersonDokument persondata = new PersonDokument();
        persondata.setFnr(FNR_BRUKER);
        persondata.setSammensattNavn(NAVN_BRUKER);
        UstrukturertAdresse gjeldendePostadresse = new UstrukturertAdresse();
        gjeldendePostadresse.adresselinje1 = "Andebyveien 1";
        gjeldendePostadresse.postnr = POSTNR_BRUKER;
        gjeldendePostadresse.poststed = POSTSTED_BRUKER;
        persondata.setGjeldendePostadresse(gjeldendePostadresse);
        return persondata;
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        behandling.setFagsak(fagsak);
        return behandling;
    }

    private Trygdeavgiftsgrunnlag lagNorskTrygdeAvgiftsgrunnlag() {
        return new Trygdeavgiftsgrunnlag(LØNN_FRA_NORGE, lagAvgiftsGrunnlagNorge(), null);
    }

    private Trygdeavgiftsgrunnlag lagUtenlandskTrygdeAvgiftsgrunnlag() {
        return new Trygdeavgiftsgrunnlag(LØNN_FRA_UTLANDET, null, lagAvgiftsGrunnlagUtland());
    }

    private AvgiftsgrunnlagInfoNorge lagAvgiftsGrunnlagNorge() {
        return new AvgiftsgrunnlagInfoNorge(true, true, null,
            Vurderingsutfall_trygdeavgift_norsk_inntekt.NORSK_INNTEKT_TRYGDEAVGIFT_NAV);
    }

    private AvgiftsgrunnlagInfoUtland lagAvgiftsGrunnlagUtland() {
        return new AvgiftsgrunnlagInfoUtland(true, false, Saerligeavgiftsgrupper.ARBEIDSTAKER_MALAYSIA,
            Vurderingsutfall_trygdeavgift_utenlandsk_inntekt.UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV);
    }

    private Behandlingsresultat lagBehandlingsResultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setMedlemAvFolketrygden(lagMedlemAvFolketrygden());
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setVilkaar(Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE);
        VilkaarBegrunnelse vilkaarBegrunnelse = new VilkaarBegrunnelse();
        vilkaarBegrunnelse.setKode(ANSATT_I_NORSK_VIRKSOMHET_IKKE_UTSENDT.getKode());
        vilkaarsresultat.setBegrunnelser(Set.of(vilkaarBegrunnelse));
        behandlingsresultat.setVilkaarsresultater(Set.of(vilkaarsresultat));

        return behandlingsresultat;
    }

    private List<AvklartVirksomhet> lagAvklarteVirksomheter() {
        return List.of(new AvklartVirksomhet(ARBEIDSGIVER_NAVN, "987654321", BrevDataTestUtils.lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID));
    }

    private AvklarteMedfolgendeBarn lagAvklartMedfølgendeBarn() {
        return new AvklarteMedfolgendeBarn(Set.of(new OmfattetFamilie(UUID_BARN_1)), Set.of());
    }

    private AvklarteMedfolgendeFamilie lagAvklartMedfølgendeEktefelle() {
        OmfattetFamilie ektefelle = new OmfattetFamilie(UUID_EKTEFELLE);
        return new AvklarteMedfolgendeFamilie(Set.of(ektefelle), Set.of());
    }

    private AvklarteMedfolgendeBarn lagAvklartIkkeMedfølgendeBarn() {
        IkkeOmfattetBarn barn1 = new IkkeOmfattetBarn(UUID_BARN_1, IKKE_SOEKERS_BARN.getKode(), "Ikke omfattet");
        return new AvklarteMedfolgendeBarn(Set.of(), Set.of(barn1));
    }

    private AvklarteMedfolgendeFamilie lagAvklartIkkeMedfølgendeEktefelle() {
        IkkeOmfattetFamilie ektefelle = new IkkeOmfattetFamilie(UUID_EKTEFELLE, IKKE_TRE_AV_FEM_SISTE_ÅR.getKode(), "Ikke omfattet");
        return new AvklarteMedfolgendeFamilie(Set.of(), Set.of(ektefelle));
    }

    private Map<String, MedfolgendeFamilie> lagMedfølgendeEktefelle() {
        MedfolgendeFamilie ektefelle = new MedfolgendeFamilie();
        ektefelle.fnr = EKTEFELLE_FNR;
        return Map.of(UUID_EKTEFELLE, ektefelle);
    }

    private Map<String, MedfolgendeFamilie> lagMedfølgendeBarn() {
        MedfolgendeFamilie medfolgendeBarn1 = new MedfolgendeFamilie();
        medfolgendeBarn1.fnr = BARN1_FNR;
        return Map.of(UUID_BARN_1, medfolgendeBarn1);
    }

    private MedlemAvFolketrygden lagMedlemAvFolketrygden() {
        MedlemAvFolketrygden medlemAvFolketrygden = new MedlemAvFolketrygden();
        medlemAvFolketrygden.setMedlemskapsperioder(lagMedlemskapsperioder());
        medlemAvFolketrygden.setFastsattTrygdeavgift(lagFastsattTrygdeavgift());

        return medlemAvFolketrygden;
    }

    private List<Medlemskapsperiode> lagMedlemskapsperioder() {
        Medlemskapsperiode periode1 = new Medlemskapsperiode();
        periode1.setBestemmelse(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8);
        periode1.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        periode1.setMedlemskapstype(Medlemskapstyper.FRIVILLIG);
        periode1.setTrygdedekning(Trygdedekninger.HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER);
        return List.of(periode1);
    }

    private FastsattTrygdeavgift lagFastsattTrygdeavgift() {
        FastsattTrygdeavgift fastsattTrygdeavgift = new FastsattTrygdeavgift();
        fastsattTrygdeavgift.setAvgiftspliktigNorskInntektMnd(50000L);
        fastsattTrygdeavgift.setAvgiftspliktigUtenlandskInntektMnd(50000L);
        fastsattTrygdeavgift.setBetalesAv(lagBetalesAv());
        fastsattTrygdeavgift.setRepresentantNr("1234");
        return fastsattTrygdeavgift;
    }

    private Aktoer lagBetalesAv() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.REPRESENTANT_TRYGDEAVGIFT);
        return aktoer;
    }

    private void mockHappyCase() {
        when(mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(anyLong())).thenReturn(lagAvklartMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(lagAvklartMedfølgendeBarn());
        when(mockTrygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(anyLong())).thenReturn(lagNorskTrygdeAvgiftsgrunnlag());
        when(mockBehandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(lagBehandlingsResultat());
        when(mockAvklarteVirksomheterService.hentNorskeArbeidsgivere(any(Behandling.class))).thenReturn(lagAvklarteVirksomheter());
        when(mockAvklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(anyLong())).thenReturn(lagMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentMedfølgendeBarn(anyLong())).thenReturn(lagMedfølgendeBarn());
        when(mockLandvelgerService.hentArbeidsland(anyLong())).thenReturn(Landkoder.AT);
        when(mockRepresentantService.hentRepresentant(anyString())).thenReturn(new RepresentantDataDto("1234", REPRESENTANT_NAVN, null, null, null));
        when(mockPersondataFasade.hentSammensattNavn(anyString())).thenAnswer((Answer<String>) invocationOnMock -> {
            String fnr = invocationOnMock.getArgument(0);
            return switch (fnr) {
                case EKTEFELLE_FNR -> EKTEFELLE_NAVN;
                case BARN1_FNR -> BARN1_NAVN;
                default -> null;
            };
        });
    }
}
