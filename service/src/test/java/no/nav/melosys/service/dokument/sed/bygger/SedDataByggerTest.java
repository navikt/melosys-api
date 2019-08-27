package no.nav.melosys.service.dokument.sed.bygger;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.ressurser.Brevressurser;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SedDataByggerTest {
    @Mock
    KodeverkService kodeverkService;
    @Mock
    RegisterOppslagService registerOppslagService;
    @Mock
    LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    AvklartefaktaService avklartefaktaService;

    private SedDataBygger dataBygger;
    private Behandling behandling;
    private Behandlingsresultat behandlingsresultat;

    @Before
    @SuppressWarnings("unchecked")
    public void setup()
        throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {

        doReturn(DataByggerStubs.hentOrganisasjonDokumentSetStub()).when(registerOppslagService).hentOrganisasjoner(anySet());

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
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

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now().plusYears(2), Landkoder.NO, Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, Trygdedekninger.FULL_DEKNING_EOSFO);
        behandlingsresultat.setAnmodningsperioder(Collections.singleton(anmodningsperiode));

        behandling = DataByggerStubs.hentBehandlingStub();

        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService);
        Brevressurser brevressurser = new Brevressurser(behandling, kodeverkService, null, avklarteVirksomheterService, avklartefaktaService, lovvalgsperiodeService);
        dataBygger = new SedDataBygger(brevressurser, lovvalgsperiodeService);
    }

    @Test
    public void testHentAvklarteSelvstendigeForetak()
        throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lag(behandlingsresultat);

        assertThat(sedData).isNotNull();
        assertThat(sedData.getArbeidsgivendeVirksomheter()).isNotNull();
        assertThat(sedData.getArbeidssteder()).isNotNull();
        assertThat(sedData.getBruker()).isNotNull();
        assertThat(sedData.getBostedsadresse()).isNotNull();
        assertThat(sedData.getFamilieMedlem()).isNotNull();
        assertThat(sedData.getLovvalgsperioder()).isNotNull();
        assertThat(sedData.getSelvstendigeVirksomheter()).isNotNull();
        assertThat(sedData.getUtenlandskeVirksomheter()).isNotNull();
        assertThat(sedData.getUtenlandskIdent()).isNotNull();
        assertThat(sedData.getMottakerLand()).isEqualTo("SE");

        assertThat(sedData.getArbeidsgivendeVirksomheter().isEmpty()).isFalse();
    }

    @Test
    public void lagUtkast_forventFelt_utenLovvalgsperioder()
        throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lagUtkast();

        lagUtkastAssertions(sedData);
        assertThat(sedData.getLovvalgsperioder().isEmpty()).isTrue();
    }

    @Test
    public void lagUtkast_forventFelt_medLovvalgsperioder()
        throws FunksjonellException, TekniskException {
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(lagLovvalgsperioder());
        SedDataDto sedData = dataBygger.lagUtkast();

        lagUtkastAssertions(sedData);
        assertThat(sedData.getLovvalgsperioder().isEmpty()).isFalse();
    }

    private void lagUtkastAssertions(SedDataDto sedData) {
        assertThat(sedData).isNotNull();
        assertThat(sedData.getArbeidsgivendeVirksomheter()).isNotNull();
        assertThat(sedData.getArbeidssteder()).isNotNull();
        assertThat(sedData.getBruker()).isNotNull();
        assertThat(sedData.getBostedsadresse()).isNotNull();
        assertThat(sedData.getFamilieMedlem()).isNotNull();
        assertThat(sedData.getUtenlandskIdent()).isNotNull();
        assertThat(sedData.getSelvstendigeVirksomheter()).isNotNull();
        assertThat(sedData.getUtenlandskeVirksomheter()).isNotNull();
        assertThat(sedData.getTidligereLovvalgsperioder()).isNull();
        assertThat(sedData.getMottakerLand()).isNull();
        assertThat(sedData.getArbeidsgivendeVirksomheter().isEmpty()).isFalse();
    }

    private List<Lovvalgsperiode> lagLovvalgsperioder() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1);
        return Collections.singletonList(lovvalgsperiode);
    }
}
