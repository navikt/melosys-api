package no.nav.melosys.service.dokument.sed.bygger;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doReturn;

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
        throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {

        doReturn(DataByggerStubs.hentOrganisasjonDokumentSetStub()).when(registerOppslagService).hentOrganisasjoner(anySet());

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1L));
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);

        behandlingsresultat = new Behandlingsresultat();
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        VilkaarBegrunnelse vilkaarBegrunnelse = new VilkaarBegrunnelse();
        vilkaarBegrunnelse.setKode("SOEKT_FOR_SENT");
        vilkaarsresultat.setBegrunnelser(new HashSet<>(Collections.singletonList(vilkaarBegrunnelse)));
        behandlingsresultat.setVilkaarsresultater(Collections.singleton(vilkaarsresultat));
        lovvalgsperiode.setBehandlingsresultat(behandlingsresultat);

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now().plusYears(2), Landkoder.NO, LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1,
            null, Landkoder.SE, LovvalgsBestemmelser_883_2004.FO_883_2004_ART13_1A, Trygdedekninger.FULL_DEKNING_EOSFO);
        behandlingsresultat.setAnmodningsperioder(Collections.singleton(anmodningsperiode));

        behandling = DataByggerStubs.hentBehandlingStub();

        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService);
        dataBygger = new SedDataBygger(kodeverkService, lovvalgsperiodeService, avklartefaktaService, avklarteVirksomheterService);
    }

    @Test
    public void testHentAvklarteSelvstendigeForetak()
        throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lag(behandling, behandlingsresultat);

        assertNotNull(sedData);
        assertNotNull(sedData.getArbeidsgivendeVirksomheter());
        assertNotNull(sedData.getArbeidssteder());
        assertNotNull(sedData.getBruker());
        assertNotNull(sedData.getBostedsadresse());
        assertNotNull(sedData.getFamilieMedlem());
        assertNotNull(sedData.getLovvalgsperioder());
        assertNotNull(sedData.getSelvstendigeVirksomheter());
        assertNotNull(sedData.getUtenlandskeVirksomheter());
        assertNotNull(sedData.getUtenlandskIdent());
        assertEquals("SE", sedData.getMottakerLand());

        assertFalse(sedData.getArbeidsgivendeVirksomheter().isEmpty());
    }

    @Test
    public void lagUtkast_forventFelt()
        throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lagUtkast(behandling);

        assertNotNull(sedData);
        assertNotNull(sedData.getArbeidsgivendeVirksomheter());
        assertNotNull(sedData.getArbeidssteder());
        assertNotNull(sedData.getBruker());
        assertNotNull(sedData.getBostedsadresse());
        assertNotNull(sedData.getFamilieMedlem());
        assertNotNull(sedData.getUtenlandskIdent());
        assertNotNull(sedData.getSelvstendigeVirksomheter());
        assertNotNull(sedData.getUtenlandskeVirksomheter());
        assertNull(sedData.getLovvalgsperioder());
        assertNull(sedData.getTidligereLovvalgsperioder());
        assertNull(sedData.getMottakerLand());

        assertFalse(sedData.getArbeidsgivendeVirksomheter().isEmpty());
    }
}
