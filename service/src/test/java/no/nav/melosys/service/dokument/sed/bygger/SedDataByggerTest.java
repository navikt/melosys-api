package no.nav.melosys.service.dokument.sed.bygger;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Bosted;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.LuftfartBase;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.Diskresjonskode;
import no.nav.melosys.domain.dokument.person.Gateadresse;
import no.nav.melosys.domain.eessi.sed.Adresse;
import no.nav.melosys.domain.eessi.sed.Arbeidssted;
import no.nav.melosys.domain.eessi.sed.SedDataDto;
import no.nav.melosys.domain.eessi.sed.Virksomhet;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagMedSoknad;
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagUtenSoknad;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.eessi.sed.Adresse.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private SedDataBygger dataBygger;
    private Behandling behandling;
    private Behandlingsresultat behandlingsresultat;
    private Lovvalgsperiode lovvalgsperiode;
    private Anmodningsperiode anmodningsperiode;
    private Utpekingsperiode utpekingsperiode;

    @Before
    public void setup() throws IkkeFunnetException, TekniskException {

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
        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService, mock(BehandlingService.class), kodeverkService);
        return new SedDataGrunnlagMedSoknad(behandling, kodeverkService, avklarteVirksomheterService, avklartefaktaService);
    }

    private SedDataGrunnlagUtenSoknad lagDokumentressurserUtenSøknad() throws TekniskException {
        return new SedDataGrunnlagUtenSoknad(behandling, kodeverkService);
    }

    private SedDataGrunnlagMedSoknad lagDokumentressurserMedManglendeAdressefelter(boolean arbeidsstedManglerLandkode,
                                                                                   boolean arbeidsgivendeForetakUtlandManglerLandkode,
                                                                                   boolean selvstendigForetakUtlandManglerLandkode) throws TekniskException {
        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService, mock(BehandlingService.class), kodeverkService);
        return new SedDataGrunnlagMedSoknad(DataByggerStubs.hentBehandlingMedManglendeAdressefelterStub(
            arbeidsstedManglerLandkode, arbeidsgivendeForetakUtlandManglerLandkode, selvstendigForetakUtlandManglerLandkode),
            kodeverkService, avklarteVirksomheterService, avklartefaktaService);
    }

    @Test
    public void lag_medlemsperiodeTypeLovvalgsperiodeMedSøknad_forventLovvalgsperiodeBrukt() throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lag(lagDokumentressurser(), behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

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
        SedDataDto sedData = dataBygger.lag(lagDokumentressurser(), behandlingsresultat, PeriodeType.ANMODNINGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getLovvalgsperioder()).isNotEmpty();
        var sedLovvalgsperiode = sedData.getLovvalgsperioder().get(0);
        assertThat(sedLovvalgsperiode.getFom()).isEqualTo(anmodningsperiode.getFom());
        assertThat(sedLovvalgsperiode.getTom()).isEqualTo(anmodningsperiode.getTom());
        assertThat(sedLovvalgsperiode.getLovvalgsland()).isEqualTo(anmodningsperiode.getLovvalgsland().getKode());
    }

    @Test
    public void lag_medlemsperiodeTypeUtpekingsperiodeMedSøknad_forventUtpekingsperiode() throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lag(lagDokumentressurser(), behandlingsresultat, PeriodeType.UTPEKINGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getLovvalgsperioder()).isNotEmpty();
        var sedLovvalgsperiode = sedData.getLovvalgsperioder().get(0);
        assertThat(sedLovvalgsperiode.getFom()).isEqualTo(utpekingsperiode.getFom());
        assertThat(sedLovvalgsperiode.getTom()).isEqualTo(utpekingsperiode.getTom());
        assertThat(sedLovvalgsperiode.getLovvalgsland()).isEqualTo(utpekingsperiode.getLovvalgsland().getKode());
    }

    @Test
    public void lag_medlemsperiodeTypeAnmodningsperiodeUtenSøknad_forventAnmodningsperiode() throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lag(lagDokumentressurserUtenSøknad(), behandlingsresultat, PeriodeType.ANMODNINGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getLovvalgsperioder()).isNotEmpty();
        assertThat(sedData.getLovvalgsperioder().get(0).getFom()).isEqualTo(anmodningsperiode.getFom());
    }

    @Test
    public void lag_medlemsperiodeTypeIngenMedSøknad_forventAnmodningsperiode() throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lag(lagDokumentressurser(), behandlingsresultat, PeriodeType.INGEN);

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

        SedDataDto sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

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

        SedDataDto sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

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

        SedDataDto sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

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

        SedDataDto sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getBostedsadresse()).extracting(Adresse::getGateadresse).isEqualTo("gate 123");
    }

    @Test
    public void lag_bostedsadresseFinnesIkke_kasterException() throws TekniskException {

        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagDokumentressurser();
        sedDataGrunnlagMedSoknad.getBehandlingsgrunnlagData().bosted = new Bosted();
        sedDataGrunnlagMedSoknad.getPerson().bostedsadresse = new Bostedsadresse();

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE))
            .withMessageContaining("Finner ingen bostedsadresse ");
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

        SedDataDto sedData = dataBygger.lag(lagDokumentressurser(), behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getArbeidssteder())
            .extracting(Arbeidssted::getAdresse)
            .extracting(Adresse::getGateadresse)
            .contains(IKKE_TILGJENGELIG);
    }

    @Test
    public void lag_brukerErKode6_forventHarSensitiveOpplysninger() throws TekniskException, FunksjonellException {
        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagDokumentressurser();
        sedDataGrunnlagMedSoknad.getPerson().diskresjonskode = new Diskresjonskode("SPSF");
        SedDataDto sedData = dataBygger.lagUtkast(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getBruker()).isNotNull();
        assertThat(sedData.getBruker().harSensitiveOpplysninger()).isTrue();
    }

    @Test
    public void lag_brukerHarKode7_forventHarIkkeSensitiveOpplysninger() throws TekniskException, FunksjonellException {
        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagDokumentressurser();
        sedDataGrunnlagMedSoknad.getPerson().diskresjonskode = new Diskresjonskode("SPSO");
        SedDataDto sedData = dataBygger.lagUtkast(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getBruker()).isNotNull();
        assertThat(sedData.getBruker().harSensitiveOpplysninger()).isFalse();
    }

    @Test
    public void lag_brukerHarIngenDiskresjonskode_forventHarIkkeSensitiveOpplysninger() throws TekniskException, FunksjonellException {
        SedDataGrunnlagMedSoknad sedDataGrunnlagMedSoknad = lagDokumentressurser();
        sedDataGrunnlagMedSoknad.getPerson().diskresjonskode = null;
        SedDataDto sedData = dataBygger.lagUtkast(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getBruker()).isNotNull();
        assertThat(sedData.getBruker().harSensitiveOpplysninger()).isFalse();
    }

    @Test
    public void lagUtkast_medlemsperiodeTypeIngenMedSøknad_forventAnmodningsperiode() throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lagUtkast(lagDokumentressurser(), behandlingsresultat, PeriodeType.ANMODNINGSPERIODE);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getLovvalgsperioder()).isNotEmpty();
        assertThat(sedData.getLovvalgsperioder().get(0).getUnntakFraLovvalgsland()).isNotEmpty();
    }

    @Test
    public void lagUtkast_medlemsperiodeTypeIngenMedSøknad_utenLovvalgsperioder()
        throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lagUtkast(lagDokumentressurser(), behandlingsresultat, PeriodeType.INGEN);

        lagUtkastAssertions(sedData, true);
        assertThat(sedData.getLovvalgsperioder().isEmpty()).isTrue();
    }

    @Test
    public void lagUtkast_medlemsperiodeTypeIngenUtenSøknad_utenLovvalgsperioder()
        throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lagUtkast(lagDokumentressurserUtenSøknad(), behandlingsresultat, PeriodeType.INGEN);

        assertThat(sedData.getBruker()).isNotNull();
        assertThat(sedData.getBostedsadresse()).isNotNull();
        assertThat(sedData.getLovvalgsperioder().isEmpty()).isTrue();
    }

    @Test
    public void lagUtkast_medlemsperiodeTypeLovvalgsperiodeMedSøknad_medLovvalgsperioder()
        throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lagUtkast(lagDokumentressurser(), behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        lagUtkastAssertions(sedData, true);
        assertThat(sedData.getLovvalgsperioder()).isNotEmpty();
        assertThat(sedData.getLovvalgsperioder().get(0).getFom()).isEqualTo(lovvalgsperiode.getFom());
    }

    @Test
    public void lagUtkast_ingenAdresse_forventTomAdresse() throws MelosysException {
        SedDataGrunnlagMedSoknad dataGrunnlag = lagDokumentressurser();
        dataGrunnlag.getPerson().bostedsadresse = new no.nav.melosys.domain.dokument.person.Bostedsadresse();

        SedDataDto sedData = dataBygger.lagUtkast(dataGrunnlag, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        lagUtkastAssertions(sedData, false);
        assertThat(sedData.getBostedsadresse()).isNull();
    }

    @Test
    public void lagUtkast_harIkkeFastArbeidsstedForArbeidsland_arbeidsstedBlirSatt() throws TekniskException, FunksjonellException {
        when(landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(anyLong())).thenReturn(List.of(Landkoder.SE));
        SedDataGrunnlagMedSoknad dataGrunnlag = lagDokumentressurser();
        SedDataDto sedData = dataBygger.lag(dataGrunnlag, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

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
        SedDataDto sedData = dataBygger.lag(dataGrunnlag, behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getArbeidssteder().size()).isEqualTo(2);

        Arbeidssted arbeidssted = sedData.getArbeidssteder().get(1);

        assertThat(arbeidssted.getNavn()).isEqualTo(luftfartBase.hjemmebaseNavn);
        assertThat(arbeidssted.getAdresse().getGateadresse()).isEqualTo("N/A");
        assertThat(arbeidssted.getAdresse().getLand()).isEqualTo(luftfartBase.hjemmebaseLand);
    }

    @Test
    public void lagUtkast_medUtenlandskSelvstendigForetak_forventAtUtenlandskSelvstendigForetakIkkeSendesSomArbeidsgivendeVirksomhet() throws TekniskException, FunksjonellException {
        ForetakUtland utenlandskSelvstendigForetak = new ForetakUtland();
        utenlandskSelvstendigForetak.adresse = new StrukturertAdresse();
        utenlandskSelvstendigForetak.adresse.landkode = Landkoder.DE.getKode();
        utenlandskSelvstendigForetak.selvstendigNæringsvirksomhet = true;
        utenlandskSelvstendigForetak.navn = "selvstendig";
        utenlandskSelvstendigForetak.uuid = "123";

        SedDataGrunnlagMedSoknad dataGrunnlag = lagDokumentressurser();
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
    public void lag_arbeidsstedManglerLandkode_kasterFeil() throws TekniskException, FunksjonellException {
        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("land er ikke utfylt for arbeidssted");
        dataBygger.lag(lagDokumentressurserMedManglendeAdressefelter(true, false, false),
            behandlingsresultat, PeriodeType.LOVVALGSPERIODE);
    }

    @Test
    public void lag_arbeidsgivendeVirksomhetManglerLandkode_kasterFeil() throws TekniskException, FunksjonellException {
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(Set.of("uuid"));
        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("land er ikke utfylt for virksomhet");
        dataBygger.lag(lagDokumentressurserMedManglendeAdressefelter(false, true, false),
            behandlingsresultat, PeriodeType.LOVVALGSPERIODE);
    }

    @Test
    public void lag_selvstendigVirksomhetManglerLandkode_kasterFeil() throws TekniskException, FunksjonellException {
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(Set.of("uuid"));
        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("land er ikke utfylt for selvstendig virksomhet");
        dataBygger.lag(lagDokumentressurserMedManglendeAdressefelter(false, false, true),
            behandlingsresultat, PeriodeType.LOVVALGSPERIODE);
    }

    @Test
    public void lagArbeidssted_manglerObligatoriskeFelter_blirUnknown() throws TekniskException, FunksjonellException {
        SedDataDto sedData = dataBygger.lag(lagDokumentressurserMedManglendeAdressefelter(false, false, false),
            behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getArbeidssteder())
            .extracting(Arbeidssted::getAdresse)
            .extracting(Adresse::getPoststed)
            .contains(UKJENT);
    }

    @Test
    public void lagVirksomhet_manglerObligatoriskeFelter_blirUnknown() throws TekniskException, FunksjonellException {
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(Set.of("uuid"));
        SedDataDto sedData = dataBygger.lag(lagDokumentressurserMedManglendeAdressefelter(false, false, false),
            behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getArbeidsgivendeVirksomheter())
            .filteredOn(virksomhet -> UKJENT.equals(virksomhet.getOrgnr()))
            .extracting(Virksomhet::getAdresse)
            .extracting(Adresse::getPoststed)
            .contains(UKJENT);
    }

    @Test
    public void lagVirksomhet_harObligatoriskeFelter_blirSatt() throws TekniskException, FunksjonellException {
        SedDataDto sedData = dataBygger.lag(lagDokumentressurser(), behandlingsresultat, PeriodeType.LOVVALGSPERIODE);

        assertThat(sedData.getArbeidsgivendeVirksomheter())
            .extracting(Virksomhet::getOrgnr)
            .contains("orgnr");
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
