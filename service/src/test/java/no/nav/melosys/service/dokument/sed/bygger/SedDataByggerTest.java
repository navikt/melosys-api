package no.nav.melosys.service.dokument.sed.bygger;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.Diskresjonskode;
import no.nav.melosys.domain.dokument.person.Gateadresse;
import no.nav.melosys.domain.dokument.person.UstrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.LuftfartBase;
import no.nav.melosys.domain.eessi.sed.Adresse;
import no.nav.melosys.domain.eessi.sed.Adressetype;
import no.nav.melosys.domain.eessi.sed.Arbeidssted;
import no.nav.melosys.domain.eessi.sed.SedDataDto;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.*;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagMedSoknad;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagUtenSoknad;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.eessi.sed.Adresse.IKKE_TILGJENGELIG;
import static no.nav.melosys.domain.eessi.sed.Adresse.INGEN_FAST_ADRESSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SedDataByggerTest {
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

    private SedDataBygger dataBygger;
    private Behandling behandling;
    private Behandlingsresultat behandlingsresultat;
    private Lovvalgsperiode lovvalgsperiode;
    private Anmodningsperiode anmodningsperiode;
    private Utpekingsperiode utpekingsperiode;

    @Before
    public void setup() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {

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

        dataBygger = new SedDataBygger(lovvalgsperiodeService, landvelgerService);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusMonths(2L));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2);
        when(lovvalgsperiodeService.hentTidligereLovvalgsperioder(any())).thenReturn(List.of(lovvalgsperiode));
    }

    private SedDataGrunnlagMedSoknad lagDokumentressurser() throws TekniskException {
        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService);
        return new SedDataGrunnlagMedSoknad(behandling, kodeverkService, avklarteVirksomheterService, avklartefaktaService);
    }

    private SedDataGrunnlagUtenSoknad lagDokumentressurserUtenSøknad() throws TekniskException {
        return new SedDataGrunnlagUtenSoknad(behandling, kodeverkService);
    }

    @Test
    public void lag_medlemsperiodeTypeLovvalgsperiodeMedSøknad_forventLovvalgsperiodeBrukt() throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lag(lagDokumentressurser(), behandlingsresultat, MedlemsperiodeType.LOVVALGSPERIODE);

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
    public void lag_medlemsperiodeTypeAnmodningsperiodeMedSøknad_forventAnmodningsperiode() throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lag(lagDokumentressurser(), behandlingsresultat, MedlemsperiodeType.ANMODNINGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getLovvalgsperioder()).isNotEmpty();
        var sedLovvalgsperiode = sedData.getLovvalgsperioder().get(0);
        assertThat(sedLovvalgsperiode.getFom()).isEqualTo(anmodningsperiode.getFom());
        assertThat(sedLovvalgsperiode.getTom()).isEqualTo(anmodningsperiode.getTom());
        assertThat(sedLovvalgsperiode.getLovvalgsland()).isEqualTo(anmodningsperiode.getLovvalgsland().getKode());
    }

    @Test
    public void lag_medlemsperiodeTypeUtpekingsperiodeMedSøknad_forventUtpekingsperiode() throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lag(lagDokumentressurser(), behandlingsresultat, MedlemsperiodeType.UTPEKINGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getLovvalgsperioder()).isNotEmpty();
        var sedLovvalgsperiode = sedData.getLovvalgsperioder().get(0);
        assertThat(sedLovvalgsperiode.getFom()).isEqualTo(utpekingsperiode.getFom());
        assertThat(sedLovvalgsperiode.getTom()).isEqualTo(utpekingsperiode.getTom());
        assertThat(sedLovvalgsperiode.getLovvalgsland()).isEqualTo(utpekingsperiode.getLovvalgsland().getKode());
    }

    @Test
    public void lag_medlemsperiodeTypeAnmodningsperiodeUtenSøknad_forventAnmodningsperiode() throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lag(lagDokumentressurserUtenSøknad(), behandlingsresultat, MedlemsperiodeType.ANMODNINGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getLovvalgsperioder()).isNotEmpty();
        assertThat(sedData.getLovvalgsperioder().get(0).getFom()).isEqualTo(anmodningsperiode.getFom());
    }

    @Test
    public void lag_medlemsperiodeTypeIngenMedSøknad_forventAnmodningsperiode() throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lag(lagDokumentressurser(), behandlingsresultat, MedlemsperiodeType.INGEN);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getLovvalgsperioder()).isEmpty();
    }

    @Test
    public void lag_bostedsadresseUtenGateadresse_gatenavnBlirNA() throws TekniskException, FunksjonellException {
        Bostedsadresse bostedsadresse = new Bostedsadresse();
        bostedsadresse.setGateadresse(null);
        bostedsadresse.setLand(new Land(Land.SVERIGE));

        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagDokumentressurser();
        sedDataGrunnlagMedSoknad.getPerson().bostedsadresse = bostedsadresse;

        SedDataDto sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, MedlemsperiodeType.LOVVALGSPERIODE);

        assertThat(sedData.getBostedsadresse()).extracting(Adresse::getGateadresse).isEqualTo(IKKE_TILGJENGELIG);
    }

    @Test
    public void lag_bostedsadresseUtenGatenavn_gatenavnBlirNA() throws TekniskException, FunksjonellException {
        Gateadresse gateadresse = new Gateadresse();
        gateadresse.setGatenavn("");
        gateadresse.setHusnummer(123);

        Bostedsadresse bostedsadresse = new Bostedsadresse();
        bostedsadresse.setGateadresse(gateadresse);
        bostedsadresse.setLand(new Land(Land.SVERIGE));

        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagDokumentressurser();
        sedDataGrunnlagMedSoknad.getPerson().bostedsadresse = bostedsadresse;

        SedDataDto sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, MedlemsperiodeType.LOVVALGSPERIODE);

        assertThat(sedData.getBostedsadresse()).extracting(Adresse::getGateadresse).isEqualTo(IKKE_TILGJENGELIG);
    }

    @Test
    public void lag_bostedsadresseMedBlanktGatenavn_gatenavnBlirNA() throws TekniskException, FunksjonellException {
        Gateadresse gateadresse = new Gateadresse();
        gateadresse.setGatenavn(" ");
        gateadresse.setHusnummer(123);

        Bostedsadresse bostedsadresse = new Bostedsadresse();
        bostedsadresse.setGateadresse(gateadresse);
        bostedsadresse.setLand(new Land(Land.SVERIGE));

        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagDokumentressurser();
        sedDataGrunnlagMedSoknad.getPerson().bostedsadresse = bostedsadresse;

        SedDataDto sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, MedlemsperiodeType.LOVVALGSPERIODE);

        assertThat(sedData.getBostedsadresse()).extracting(Adresse::getGateadresse).isEqualTo(IKKE_TILGJENGELIG);
    }

    @Test
    public void lag_bostedsadresseMedGatenavnOgHusnummer_rettFormatertGateadresse() throws TekniskException, FunksjonellException {
        Gateadresse gateadresse = new Gateadresse();
        gateadresse.setGatenavn("gate");
        gateadresse.setHusnummer(123);

        Bostedsadresse bostedsadresse = new Bostedsadresse();
        bostedsadresse.setGateadresse(gateadresse);
        bostedsadresse.setLand(new Land(Land.SVERIGE));

        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagDokumentressurser();
        sedDataGrunnlagMedSoknad.getPerson().bostedsadresse = bostedsadresse;

        SedDataDto sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, MedlemsperiodeType.LOVVALGSPERIODE);

        assertThat(sedData.getBostedsadresse()).extracting(Adresse::getGateadresse).isEqualTo("gate 123");
    }

    @Test
    public void lag_medMaritimtArbeid_gatenavnBlirNA() throws TekniskException, FunksjonellException {
        Map<String, AvklartMaritimtArbeid> alleAvklarteMaritimeArbeid = new HashMap<>();
        Avklartefakta maritimtFakta = new Avklartefakta();
        maritimtFakta.setFakta("SE");
        maritimtFakta.setType(Avklartefaktatyper.ARBEIDSLAND);
        AvklartMaritimtArbeid avklartMaritimtArbeid = new AvklartMaritimtArbeid("navn", Collections.singletonList(maritimtFakta));
        alleAvklarteMaritimeArbeid.put("enhet", avklartMaritimtArbeid);

        when(avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(anyLong())).thenReturn(alleAvklarteMaritimeArbeid);

        SedDataDto sedData = dataBygger.lag(lagDokumentressurser(), behandlingsresultat, MedlemsperiodeType.LOVVALGSPERIODE);

        assertThat(sedData.getArbeidssteder())
            .extracting(Arbeidssted::getAdresse)
            .extracting(Adresse::getGateadresse)
            .contains(IKKE_TILGJENGELIG);
    }

    @Test
    public void lag_brukerErKode6_forventHarSensitiveOpplysninger() throws TekniskException, FunksjonellException {
        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagDokumentressurser();
        sedDataGrunnlagMedSoknad.getPerson().diskresjonskode = new Diskresjonskode("SPSF");
        SedDataDto sedData = dataBygger.lagUtkast(sedDataGrunnlagMedSoknad, behandlingsresultat, MedlemsperiodeType.LOVVALGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getBruker()).isNotNull();
        assertThat(sedData.getBruker().harSensitiveOpplysninger()).isTrue();
    }

    @Test
    public void lag_brukerHarKode7_forventHarIkkeSensitiveOpplysninger() throws TekniskException, FunksjonellException {
        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagDokumentressurser();
        sedDataGrunnlagMedSoknad.getPerson().diskresjonskode = new Diskresjonskode("SPSO");
        SedDataDto sedData = dataBygger.lagUtkast(sedDataGrunnlagMedSoknad, behandlingsresultat, MedlemsperiodeType.LOVVALGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getBruker()).isNotNull();
        assertThat(sedData.getBruker().harSensitiveOpplysninger()).isFalse();
    }

    @Test
    public void lag_brukerHarIngenDiskresjonskode_forventHarIkkeSensitiveOpplysninger() throws TekniskException, FunksjonellException {
        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagDokumentressurser();
        sedDataGrunnlagMedSoknad.getPerson().diskresjonskode = null;
        SedDataDto sedData = dataBygger.lagUtkast(sedDataGrunnlagMedSoknad, behandlingsresultat, MedlemsperiodeType.LOVVALGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getBruker()).isNotNull();
        assertThat(sedData.getBruker().harSensitiveOpplysninger()).isFalse();
    }

    @Test
    public void lagUtkast_medlemsperiodeTypeIngenMedSøknad_forventAnmodningsperiode() throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lagUtkast(lagDokumentressurser(), behandlingsresultat, MedlemsperiodeType.ANMODNINGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getLovvalgsperioder()).isNotEmpty();
        assertThat(sedData.getLovvalgsperioder().get(0).getUnntakFraLovvalgsland()).isNotEmpty();
    }

    @Test
    public void lagUtkast_medlemsperiodeTypeIngenMedSøknad_utenLovvalgsperioder()
        throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lagUtkast(lagDokumentressurser(), behandlingsresultat, MedlemsperiodeType.INGEN);

        lagUtkastAssertions(sedData);
        assertThat(sedData.getLovvalgsperioder().isEmpty()).isTrue();
    }

    @Test
    public void lagUtkast_medlemsperiodeTypeIngenUtenSøknad_utenLovvalgsperioder()
        throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lagUtkast(lagDokumentressurserUtenSøknad(), behandlingsresultat, MedlemsperiodeType.INGEN);

        assertThat(sedData.getBruker()).isNotNull();
        assertThat(sedData.getBostedsadresse()).isNotNull();
        assertThat(sedData.getLovvalgsperioder().isEmpty()).isTrue();
    }

    @Test
    public void lagUtkast_medlemsperiodeTypeLovvalgsperiodeMedSøknad_medLovvalgsperioder()
        throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lagUtkast(lagDokumentressurser(), behandlingsresultat, MedlemsperiodeType.LOVVALGSPERIODE);

        lagUtkastAssertions(sedData);
        assertThat(sedData.getLovvalgsperioder()).isNotEmpty();
        assertThat(sedData.getLovvalgsperioder().get(0).getFom()).isEqualTo(lovvalgsperiode.getFom());
    }

    @Test
    public void lagUtkast_ingenBostedsadresse_forventPostadresse() throws MelosysException {
        SedDataGrunnlagMedSoknad dataGrunnlag = lagDokumentressurser();
        dataGrunnlag.getPerson().bostedsadresse = new no.nav.melosys.domain.dokument.person.Bostedsadresse();

        UstrukturertAdresse postadresse = new UstrukturertAdresse();
        postadresse.land = new Land("NOR");
        postadresse.adresselinje1 = "gate";
        dataGrunnlag.getPerson().postadresse = postadresse;

        SedDataDto sedData = dataBygger.lagUtkast(dataGrunnlag, behandlingsresultat, MedlemsperiodeType.LOVVALGSPERIODE);

        lagUtkastAssertions(sedData);
        assertThat(sedData.getBostedsadresse().getLand()).isEqualTo("NOR");
        assertThat(sedData.getBostedsadresse().getGateadresse()).isEqualTo("gate");
        assertThat(sedData.getBostedsadresse().getAdressetype()).isEqualTo(Adressetype.POSTADRESSE);
    }

    @Test
    public void lagUtkast_ingenAdresse_forventTomAdresse() throws MelosysException {
        SedDataGrunnlagMedSoknad dataGrunnlag = lagDokumentressurser();
        dataGrunnlag.getPerson().bostedsadresse = new no.nav.melosys.domain.dokument.person.Bostedsadresse();
        dataGrunnlag.getPerson().postadresse = new UstrukturertAdresse();

        SedDataDto sedData = dataBygger.lagUtkast(dataGrunnlag, behandlingsresultat, MedlemsperiodeType.LOVVALGSPERIODE);

        lagUtkastAssertions(sedData);
        assertThat(sedData.getBostedsadresse()).isEqualToComparingFieldByField(Adresse.lagTomAdresse());
    }

    @Test
    public void lagUtkast_harIkkeFastArbeidsstedForArbeidsland_arbeidsstedBlirSatt() throws TekniskException, FunksjonellException {
        when(landvelgerService.hentAlleArbeidsland(anyLong())).thenReturn(List.of(Landkoder.SE));
        SedDataGrunnlagMedSoknad dataGrunnlag = lagDokumentressurser();
        SedDataDto sedData = dataBygger.lag(dataGrunnlag, behandlingsresultat, MedlemsperiodeType.LOVVALGSPERIODE);

        assertThat(sedData.getArbeidssteder().size()).isEqualTo(2);

        Arbeidssted ikkeOppgittArbeidsstedForLand = sedData.getArbeidssteder().get(1);

        assertThat(ikkeOppgittArbeidsstedForLand.getNavn()).isEqualTo(INGEN_FAST_ADRESSE);
        assertThat(ikkeOppgittArbeidsstedForLand.getAdresse().getPoststed()).isEqualTo(INGEN_FAST_ADRESSE);
    }

    @Test
    public void lagUtkast_medLuftfartBase_arbeidsstedBlirSatt() throws TekniskException, FunksjonellException {
        LuftfartBase luftfartBase = new LuftfartBase();
        luftfartBase.hjemmebaseNavn = "hjemmebaseNavn";
        luftfartBase.hjemmebaseLand = "GB";

        SedDataGrunnlagMedSoknad dataGrunnlag = lagDokumentressurser();
        dataGrunnlag.getBehandlingsgrunnlagData().luftfartBaser = List.of(luftfartBase);
        SedDataDto sedData = dataBygger.lag(dataGrunnlag, behandlingsresultat, MedlemsperiodeType.LOVVALGSPERIODE);

        assertThat(sedData.getArbeidssteder().size()).isEqualTo(2);

        Arbeidssted arbeidssted = sedData.getArbeidssteder().get(1);

        assertThat(arbeidssted.getNavn()).isEqualTo(luftfartBase.hjemmebaseNavn);
        assertThat(arbeidssted.getAdresse().getGateadresse()).isEqualTo("N/A");
        assertThat(arbeidssted.getAdresse().getLand()).isEqualTo(luftfartBase.hjemmebaseLand);
    }

    private void lagUtkastAssertions(SedDataDto sedData) {
        assertThat(sedData).isNotNull();
        assertThat(sedData.getArbeidsgivendeVirksomheter()).isNotEmpty();
        assertThat(sedData.getArbeidssteder()).isNotEmpty();
        assertThat(sedData.getBruker()).isNotNull();
        assertThat(sedData.getBostedsadresse()).isNotNull();
        assertThat(sedData.getFamilieMedlem()).isNotEmpty();
        assertThat(sedData.getUtenlandskIdent()).isNotEmpty();
        assertThat(sedData.getSelvstendigeVirksomheter()).isNotEmpty();
        assertThat(sedData.getTidligereLovvalgsperioder()).isNotNull();
        assertThat(sedData.getArbeidsgivendeVirksomheter().isEmpty()).isFalse();
    }
}
