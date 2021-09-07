package no.nav.melosys.service.dokument.sed.bygger;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Bosted;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.LuftfartBase;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Diskresjonskode;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.adresse.Gateadresse;
import no.nav.melosys.domain.eessi.Periode;
import no.nav.melosys.domain.eessi.sed.Adresse;
import no.nav.melosys.domain.eessi.sed.Arbeidssted;
import no.nav.melosys.domain.eessi.sed.SedDataDto;
import no.nav.melosys.domain.eessi.sed.Virksomhet;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagMedSoknad;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagUtenSoknad;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static no.nav.melosys.domain.eessi.sed.Adresse.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SedDataByggerTest {
    @Mock
    private KodeverkService kodeverkService;
    @Mock
    private RegisterOppslagService registerOppslagService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private LandvelgerService landvelgerService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private SedDataBygger dataBygger;
    private Behandling behandling;
    private Behandlingsresultat behandlingsresultat;
    private Lovvalgsperiode lovvalgsperiode;
    private Anmodningsperiode anmodningsperiode;
    private Utpekingsperiode utpekingsperiode;

    @BeforeEach
    void setup() {

        doReturn(DataByggerStubs.hentOrganisasjonDokumentSetStub()).when(registerOppslagService).hentOrganisasjoner(anySet());

        when(landvelgerService.hentBostedsland(anyLong(), any())).thenReturn(Landkoder.IT);

        lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1L));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        behandlingsresultat = new Behandlingsresultat();
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        VilkaarBegrunnelse vilkaarBegrunnelse = new VilkaarBegrunnelse();
        vilkaarBegrunnelse.setKode("SOEKT_FOR_SENT");
        vilkaarsresultat.setBegrunnelser(new HashSet<>(Collections.singletonList(vilkaarBegrunnelse)));
        behandlingsresultat.setVilkaarsresultater(Collections.singleton(vilkaarsresultat));
        lovvalgsperiode.setBehandlingsresultat(behandlingsresultat);

        anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now().plusYears(2), Landkoder.NO, Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A, Trygdedekninger.FULL_DEKNING_EOSFO);
        behandlingsresultat.setAnmodningsperioder(Collections.singleton(anmodningsperiode));
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));

        utpekingsperiode = new Utpekingsperiode(LocalDate.now(), LocalDate.now().plusYears(3), Landkoder.DK,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_4);
        behandlingsresultat.getUtpekingsperioder().add(utpekingsperiode);

        behandling = DataByggerStubs.hentBehandlingStub();
        behandlingsresultat.setBehandling(behandling);
        dataBygger = new SedDataBygger(behandlingsresultatService, landvelgerService, lovvalgsperiodeService);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusMonths(2L));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2);
        when(lovvalgsperiodeService.hentTidligereLovvalgsperioder(any())).thenReturn(List.of(lovvalgsperiode));
    }

    private SedDataGrunnlagMedSoknad lagGrunnlagMedSøknad() {
        return lagGrunnlagMedSøknad(behandling.hentPersonDokument());
    }

    private SedDataGrunnlagMedSoknad lagGrunnlagMedSøknad(Persondata persondata) {
        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService,
            registerOppslagService, mock(BehandlingService.class), kodeverkService);
        return new SedDataGrunnlagMedSoknad(behandling, kodeverkService, avklarteVirksomheterService,
            avklartefaktaService, persondata);
    }

    private SedDataGrunnlagUtenSoknad lagGrunnlagUtenSøknad() {
        return new SedDataGrunnlagUtenSoknad(behandling, kodeverkService, behandling.hentPersonDokument());
    }

    private SedDataGrunnlagMedSoknad lagGrunnlagMedManglendeAdressefelter(boolean arbeidsstedManglerLandkode,
                                                                          boolean arbeidsgivendeForetakUtlandManglerLandkode,
                                                                          boolean selvstendigForetakUtlandManglerLandkode) {
        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService, mock(BehandlingService.class), kodeverkService);
        return new SedDataGrunnlagMedSoknad(DataByggerStubs.hentBehandlingMedManglendeAdressefelterStub(
            arbeidsstedManglerLandkode, arbeidsgivendeForetakUtlandManglerLandkode, selvstendigForetakUtlandManglerLandkode),
            kodeverkService, avklarteVirksomheterService, avklartefaktaService, behandling.hentPersonDokument());
    }

    @Test
    void lag_medlemsperiodeTypeLovvalgsperiodeMedSøknad_forventLovvalgsperiodeBrukt() {
        SedDataDto sedData = dataBygger.lag(lagGrunnlagMedSøknad(), behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getArbeidsgivendeVirksomheter()).isNotEmpty();
        assertThat(sedData.getArbeidssteder()).isNotNull();
        assertThat(sedData.getBruker()).isNotNull();
        assertThat(sedData.getBostedsadresse()).isNotNull();
        assertThat(sedData.getFamilieMedlem()).isNotNull();
        assertThat(sedData.getSelvstendigeVirksomheter()).isNotNull();
        assertThat(sedData.getUtenlandskIdent()).isNotNull();

        assertThat(sedData.getLovvalgsperioder()).isNotEmpty();
        var sedLovvalgsperiode = sedData.getLovvalgsperioder().get(0);
        assertThat(sedLovvalgsperiode.getFom()).isEqualTo(lovvalgsperiode.getFom());
        assertThat(sedLovvalgsperiode.getTom()).isEqualTo(lovvalgsperiode.getTom());
        assertThat(sedLovvalgsperiode.getLovvalgsland()).isEqualTo(lovvalgsperiode.getLovvalgsland().getKode());

        assertThat(sedData.getArbeidsgivendeVirksomheter().isEmpty()).isFalse();
    }

    @Test
    void lag_medlemsperiodeTypeAnmodningsperiodeMedSøknad_forventAnmodningsperiode() {
        SedDataDto sedData = dataBygger.lag(lagGrunnlagMedSøknad(), behandlingsresultat, PeriodeType.ANMODNINGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getLovvalgsperioder()).isNotEmpty();
        var sedLovvalgsperiode = sedData.getLovvalgsperioder().get(0);
        assertThat(sedLovvalgsperiode.getFom()).isEqualTo(anmodningsperiode.getFom());
        assertThat(sedLovvalgsperiode.getTom()).isEqualTo(anmodningsperiode.getTom());
        assertThat(sedLovvalgsperiode.getLovvalgsland()).isEqualTo(anmodningsperiode.getLovvalgsland().getKode());
    }

    @Test
    void lag_medlemsperiodeTypeUtpekingsperiodeMedSøknad_forventUtpekingsperiode() {
        SedDataDto sedData = dataBygger.lag(lagGrunnlagMedSøknad(), behandlingsresultat, PeriodeType.UTPEKINGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getLovvalgsperioder()).isNotEmpty();
        var sedLovvalgsperiode = sedData.getLovvalgsperioder().get(0);
        assertThat(sedLovvalgsperiode.getFom()).isEqualTo(utpekingsperiode.getFom());
        assertThat(sedLovvalgsperiode.getTom()).isEqualTo(utpekingsperiode.getTom());
        assertThat(sedLovvalgsperiode.getLovvalgsland()).isEqualTo(utpekingsperiode.getLovvalgsland().getKode());
    }

    @Test
    void lag_medlemsperiodeTypeAnmodningsperiodeUtenSøknad_forventAnmodningsperiode() {
        SedDataDto sedData = dataBygger.lag(lagGrunnlagUtenSøknad(), behandlingsresultat, PeriodeType.ANMODNINGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getLovvalgsperioder()).isNotEmpty();
        assertThat(sedData.getLovvalgsperioder().get(0).getFom()).isEqualTo(anmodningsperiode.getFom());
    }

    @Test
    void lag_medlemsperiodeTypeIngenMedSøknad_forventAnmodningsperiode() {
        SedDataDto sedData = dataBygger.lag(lagGrunnlagMedSøknad(), behandlingsresultat, PeriodeType.INGEN);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getLovvalgsperioder()).isEmpty();
    }

    @Test
    void lag_bostedsadresseUtenGateadresse_gatenavnBlirNA() {
        Bostedsadresse bostedsadresse = new Bostedsadresse();
        bostedsadresse.setGateadresse(null);
        bostedsadresse.setLand(new Land(Land.SVERIGE));
        behandling.hentPersonDokument().setBostedsadresse(bostedsadresse);

        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad();

        SedDataDto sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getBostedsadresse()).extracting(Adresse::getGateadresse).isEqualTo(IKKE_TILGJENGELIG);
    }

    @Test
    void lag_bostedsadresseUtenGatenavn_gatenavnBlirNA() {
        Gateadresse gateadresse = new Gateadresse();
        gateadresse.setGatenavn("");
        gateadresse.setHusnummer(123);

        Bostedsadresse bostedsadresse = new Bostedsadresse();
        bostedsadresse.setGateadresse(gateadresse);
        bostedsadresse.setLand(new Land(Land.SVERIGE));
        behandling.hentPersonDokument().setBostedsadresse(bostedsadresse);

        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad();

        SedDataDto sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getBostedsadresse()).extracting(Adresse::getGateadresse).isEqualTo(IKKE_TILGJENGELIG);
    }

    @Test
    void lag_bostedsadresseMedBlanktGatenavn_gatenavnBlirNA() {
        Gateadresse gateadresse = new Gateadresse();
        gateadresse.setGatenavn(" ");
        gateadresse.setHusnummer(123);

        Bostedsadresse bostedsadresse = new Bostedsadresse();
        bostedsadresse.setGateadresse(gateadresse);
        bostedsadresse.setLand(new Land(Land.SVERIGE));
        behandling.hentPersonDokument().setBostedsadresse(bostedsadresse);

        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad();

        SedDataDto sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getBostedsadresse()).extracting(Adresse::getGateadresse).isEqualTo(IKKE_TILGJENGELIG);
    }

    @Test
    void lag_bostedsadresseMedGatenavnOgHusnummer_rettFormatertGateadresse() {
        Gateadresse gateadresse = new Gateadresse();
        gateadresse.setGatenavn("gate");
        gateadresse.setHusnummer(123);

        Bostedsadresse bostedsadresse = new Bostedsadresse();
        bostedsadresse.setGateadresse(gateadresse);
        bostedsadresse.setLand(new Land(Land.SVERIGE));
        behandling.hentPersonDokument().setBostedsadresse(bostedsadresse);

        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad();

        SedDataDto sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getBostedsadresse()).extracting(Adresse::getGateadresse).isEqualTo("gate 123");
    }

    @Test
    void lag_bostedsadresseFinnesIkke_kasterException() {
        behandling.hentPersonDokument().setBostedsadresse(new Bostedsadresse());
        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad();
        sedDataGrunnlagMedSoknad.getBehandlingsgrunnlagData().bosted = new Bosted();


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE))
            .withMessageContaining("Finner ingen bostedsadresse ");
    }

    @Test
    void lag_medKontaktadresse_kontadresseMappes() {
        Persondata persondataMedKontakadresse = PersonopplysningerObjectFactory.lagPersonopplysninger();
        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad(persondataMedKontakadresse);

        SedDataDto sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getKontaktadresse()).isNotNull()
            .extracting(Adresse::getGateadresse).isEqualTo("gatenavnKontaktadresseFreg");
    }

    @Test
    void lag_medOppholdsadresse_oppholdsadresseMappes() {
        Persondata persondataMedOppholdsadresse = PersonopplysningerObjectFactory.lagPersonopplysninger();
        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad(persondataMedOppholdsadresse);

        SedDataDto sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getOppholdsadresse()).isNotNull()
            .extracting(Adresse::getGateadresse).isEqualTo("gatenavnOppholdsadresseFreg");
    }

    @Test
    void lag_medMaritimtArbeid_gatenavnBlirNA() {
        Map<String, AvklartMaritimtArbeid> alleAvklarteMaritimeArbeid = new HashMap<>();
        Avklartefakta maritimtFakta = new Avklartefakta();
        maritimtFakta.setFakta("SE");
        maritimtFakta.setType(Avklartefaktatyper.ARBEIDSLAND);
        AvklartMaritimtArbeid avklartMaritimtArbeid = new AvklartMaritimtArbeid("navn", Collections.singletonList(maritimtFakta));
        alleAvklarteMaritimeArbeid.put("enhet", avklartMaritimtArbeid);

        when(avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(anyLong())).thenReturn(alleAvklarteMaritimeArbeid);

        SedDataDto sedData = dataBygger.lag(lagGrunnlagMedSøknad(), behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getArbeidssteder())
            .extracting(Arbeidssted::getAdresse)
            .extracting(Adresse::getGateadresse)
            .contains(IKKE_TILGJENGELIG);
    }

    @Test
    void lag_brukerErKode6_forventHarSensitiveOpplysninger() {
        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad();
        sedDataGrunnlagMedSoknad.getBehandling().hentPersonDokument().setDiskresjonskode(new Diskresjonskode("SPSF"));
        SedDataDto sedData = dataBygger.lagUtkast(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getBruker()).isNotNull();
        assertThat(sedData.getBruker().harSensitiveOpplysninger()).isTrue();
    }

    @Test
    void lag_brukerHarKode7_forventHarIkkeSensitiveOpplysninger() {
        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad();
        sedDataGrunnlagMedSoknad.getBehandling().hentPersonDokument().setDiskresjonskode(new Diskresjonskode("SPSO"));
        SedDataDto sedData = dataBygger.lagUtkast(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getBruker()).isNotNull();
        assertThat(sedData.getBruker().harSensitiveOpplysninger()).isFalse();
    }

    @Test
    void lag_brukerHarIngenDiskresjonskode_forventHarIkkeSensitiveOpplysninger() {
        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad();
        sedDataGrunnlagMedSoknad.getBehandling().hentPersonDokument().setDiskresjonskode(null);
        SedDataDto sedData = dataBygger.lagUtkast(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getBruker()).isNotNull();
        assertThat(sedData.getBruker().harSensitiveOpplysninger()).isFalse();
    }

    @Test
    void lagUtkast_medlemsperiodeTypeIngenMedSøknad_forventAnmodningsperiode() {
        SedDataDto sedData = dataBygger.lagUtkast(lagGrunnlagMedSøknad(), behandlingsresultat, PeriodeType.ANMODNINGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getLovvalgsperioder()).isNotEmpty();
        assertThat(sedData.getLovvalgsperioder().get(0).getUnntakFraLovvalgsland()).isNotEmpty();
    }

    @Test
    void lagUtkast_medlemsperiodeTypeIngenMedSøknad_utenLovvalgsperioder() {
        SedDataDto sedData = dataBygger.lagUtkast(lagGrunnlagMedSøknad(), behandlingsresultat, PeriodeType.INGEN);

        lagUtkastAssertions(sedData, true);
        assertThat(sedData.getLovvalgsperioder().isEmpty()).isTrue();
    }

    @Test
    void lagUtkast_medlemsperiodeTypeIngenUtenSøknad_utenLovvalgsperioder() {
        SedDataDto sedData = dataBygger.lagUtkast(lagGrunnlagUtenSøknad(), behandlingsresultat, PeriodeType.INGEN);

        assertThat(sedData.getBruker()).isNotNull();
        assertThat(sedData.getBostedsadresse()).isNotNull();
        assertThat(sedData.getLovvalgsperioder().isEmpty()).isTrue();
    }

    @Test
    void lagUtkast_medlemsperiodeTypeLovvalgsperiodeMedSøknad_medLovvalgsperioder() {
        SedDataDto sedData = dataBygger.lagUtkast(lagGrunnlagMedSøknad(), behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        lagUtkastAssertions(sedData, true);
        assertThat(sedData.getLovvalgsperioder()).isNotEmpty();
        assertThat(sedData.getLovvalgsperioder().get(0).getFom()).isEqualTo(lovvalgsperiode.getFom());
    }

    @Test
    void lagUtkast_ingenAdresse_forventTomAdresse() {
        behandling.hentPersonDokument().setBostedsadresse(new Bostedsadresse());
        SedDataGrunnlagMedSoknad dataGrunnlag = lagGrunnlagMedSøknad();

        SedDataDto sedData = dataBygger.lagUtkast(dataGrunnlag, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        lagUtkastAssertions(sedData, false);
        assertThat(sedData.getBostedsadresse()).isNull();
    }

    @Test
    void lagUtkast_harIkkeFastArbeidsstedForArbeidsland_arbeidsstedBlirSatt() {
        when(landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(anyLong())).thenReturn(List.of(Landkoder.SE));
        SedDataGrunnlagMedSoknad dataGrunnlag = lagGrunnlagMedSøknad();
        SedDataDto sedData = dataBygger.lag(dataGrunnlag, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getArbeidssteder().size()).isEqualTo(2);

        Arbeidssted ikkeOppgittArbeidsstedForLand = sedData.getArbeidssteder().get(1);

        assertThat(ikkeOppgittArbeidsstedForLand.getNavn()).isEqualTo(INGEN_FAST_ADRESSE);
        assertThat(ikkeOppgittArbeidsstedForLand.getAdresse().getPoststed()).isEqualTo(INGEN_FAST_ADRESSE);
    }

    @Test
    void lagUtkast_medLuftfartBase_arbeidsstedBlirSatt() {
        LuftfartBase luftfartBase = new LuftfartBase();
        luftfartBase.hjemmebaseNavn = "hjemmebaseNavn";
        luftfartBase.hjemmebaseLand = "GB";

        SedDataGrunnlagMedSoknad dataGrunnlag = lagGrunnlagMedSøknad();
        dataGrunnlag.getBehandlingsgrunnlagData().luftfartBaser = List.of(luftfartBase);
        SedDataDto sedData = dataBygger.lag(dataGrunnlag, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getArbeidssteder().size()).isEqualTo(2);

        Arbeidssted arbeidssted = sedData.getArbeidssteder().get(1);

        assertThat(arbeidssted.getNavn()).isEqualTo(luftfartBase.hjemmebaseNavn);
        assertThat(arbeidssted.getAdresse().getGateadresse()).isEqualTo("N/A");
        assertThat(arbeidssted.getAdresse().getLand()).isEqualTo(luftfartBase.hjemmebaseLand);
    }

    @Test
    void lagUtkast_medUtenlandskSelvstendigForetak_forventAtUtenlandskSelvstendigForetakIkkeSendesSomArbeidsgivendeVirksomhet() {
        ForetakUtland utenlandskSelvstendigForetak = new ForetakUtland();
        utenlandskSelvstendigForetak.adresse = new StrukturertAdresse();
        utenlandskSelvstendigForetak.adresse.setLandkode(Landkoder.DE.getKode());
        utenlandskSelvstendigForetak.selvstendigNæringsvirksomhet = true;
        utenlandskSelvstendigForetak.navn = "selvstendig";
        utenlandskSelvstendigForetak.uuid = "123";

        SedDataGrunnlagMedSoknad dataGrunnlag = lagGrunnlagMedSøknad();
        dataGrunnlag.getBehandlingsgrunnlagData().foretakUtland = List.of(utenlandskSelvstendigForetak);

        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(Set.of("123"));

        SedDataDto sedData = dataBygger.lag(dataGrunnlag, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getSelvstendigeVirksomheter())
            .extracting(Virksomhet::getNavn)
            .contains("selvstendig");

        assertThat(sedData.getArbeidsgivendeVirksomheter())
            .extracting(Virksomhet::getNavn)
            .doesNotContain("selvstendig");
    }

    @Test
    void lagVedtakDto_ikkeOpprinneligVedtakMedDagensDato_setterDatoOgVariablerISed() {
        Behandlingsresultat behandlingsresultatMedVedtak = new Behandlingsresultat();
        VedtakMetadata vedtakMetadata = new VedtakMetadata();
        vedtakMetadata.setVedtaksdato(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        behandlingsresultatMedVedtak.setVedtakMetadata(vedtakMetadata);

        Behandling avsluttetBehandling = DataByggerStubs.hentBehandlingStub();
        avsluttetBehandling.setStatus(Behandlingsstatus.AVSLUTTET);
        avsluttetBehandling.setId(2L);
        behandlingsresultatMedVedtak.setBehandling(avsluttetBehandling);

        ArrayList<Behandling> list = new ArrayList<>();
        list.add(behandling);
        list.add(avsluttetBehandling);
        behandling.getFagsak().setBehandlinger(list);
        behandlingsresultat.setBehandling(behandling);

        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultatMedVedtak);

        SedDataGrunnlagMedSoknad dataGrunnlag = lagGrunnlagMedSøknad();

        SedDataDto sedDataDto = dataBygger.lag(dataGrunnlag, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);
        assertThat(sedDataDto).isNotNull();
        assertThat(sedDataDto.getVedtakDto().erFørstegangsvedtak()).isFalse();
        assertThat(sedDataDto.getVedtakDto().datoForrigeVedtak()).isEqualTo(LocalDate.now().toString());
    }

    @Test
    void lag_arbeidsstedManglerLandkode_kasterFeil() {
        final var sedDataGrunnlagMedSoknad = lagGrunnlagMedManglendeAdressefelter(true, false, false);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE))
            .withMessageContaining("land er ikke utfylt for arbeidssted");
    }

    @Test
    void lag_arbeidsgivendeVirksomhetManglerLandkode_kasterFeil() {
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(Set.of("uuid"));
        final var sedDataGrunnlagMedSoknad = lagGrunnlagMedManglendeAdressefelter(false, true, false);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE))
            .withMessageContaining("land er ikke utfylt for virksomhet");
    }

    @Test
    void lag_selvstendigVirksomhetManglerLandkode_kasterFeil() {
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(Set.of("uuid"));
        final var sedDataGrunnlagMedSoknad = lagGrunnlagMedManglendeAdressefelter(false, false, true);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE))
            .withMessageContaining("land er ikke utfylt for selvstendig virksomhet");
    }

    @Test
    void lagArbeidssted_manglerObligatoriskeFelter_blirUnknown() {
        SedDataDto sedData = dataBygger.lag(lagGrunnlagMedManglendeAdressefelter(false, false, false),
            behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getArbeidssteder())
            .extracting(Arbeidssted::getAdresse)
            .extracting(Adresse::getPoststed)
            .contains(UKJENT);
    }

    @Test
    void lagVirksomhet_manglerObligatoriskeFelter_blirUnknown() {
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(Set.of("uuid"));
        SedDataDto sedData = dataBygger.lag(lagGrunnlagMedManglendeAdressefelter(false, false, false),
            behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getArbeidsgivendeVirksomheter())
            .filteredOn(virksomhet -> UKJENT.equals(virksomhet.getOrgnr()))
            .extracting(Virksomhet::getAdresse)
            .extracting(Adresse::getPoststed)
            .contains(UKJENT);
    }

    @Test
    void lagVirksomhet_harObligatoriskeFelter_blirSatt() {
        SedDataDto sedData = dataBygger.lag(lagGrunnlagMedSøknad(), behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getArbeidsgivendeVirksomheter())
            .extracting(Virksomhet::getOrgnr)
            .contains("orgnr");
    }

    @Test
    void lag_erBehandlingAvSøknad_søknadsperiodeBlirSatt() {
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        var søknad = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        var sedData = dataBygger.lag(lagGrunnlagMedSøknad(), behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getSøknadsperiode())
            .extracting(Periode::getFom, Periode::getTom)
            .containsExactly(søknad.periode.getFom(), søknad.periode.getTom());
    }

    @Test
    void lag_medFlereStatsborgerskap_alleStatsborgerSkapMappes() {
        Persondata personDataFraPDL = PersonopplysningerObjectFactory.lagPersonopplysninger();
        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad(personDataFraPDL);

        var sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);
        Collection<String> statsborgerskapList = sedData.getBruker().getStatsborgerskap();
        assertThat(statsborgerskapList).hasSize(3)
            .anyMatch("NOR"::equals)
            .anyMatch("SWE"::equals)
            .anyMatch("DEN"::equals);
    }

    private void lagUtkastAssertions(SedDataDto sedData, boolean forventAdresse) {
        assertThat(sedData).isNotNull();
        assertThat(sedData.getArbeidsgivendeVirksomheter()).isNotEmpty();
        assertThat(sedData.getArbeidssteder()).isNotEmpty();
        assertThat(sedData.getBruker()).isNotNull();
        if (forventAdresse) {
            assertThat(sedData.getBostedsadresse()).isNotNull();
        }
        assertThat(sedData.getFamilieMedlem()).isNotEmpty();
        assertThat(sedData.getUtenlandskIdent()).isNotEmpty();
        assertThat(sedData.getSelvstendigeVirksomheter()).isNotEmpty();
        assertThat(sedData.getTidligereLovvalgsperioder()).isNotNull();
        assertThat(sedData.getArbeidsgivendeVirksomheter().isEmpty()).isFalse();
    }
}
