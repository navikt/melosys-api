package no.nav.melosys.service.dokument.sed.bygger;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
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
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SedDataByggerTest {

    private SedDataBygger dataBygger;
    private Behandling behandling;

    @Before
    @SuppressWarnings("unchecked")
    public void setup()
        throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        KodeverkService kodeverkService = mock(KodeverkService.class);
        RegisterOppslagService registerOppslagService = mock(RegisterOppslagService.class);
        LovvalgsperiodeService lovvalgsperiodeService = mock(LovvalgsperiodeService.class);
        AvklartefaktaService avklartefaktaService = mock(AvklartefaktaService.class);

        doReturn(DataByggerStubs.hentOrganisasjonDokumentSetStub()).when(registerOppslagService).hentOrganisasjoner(anySet());

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setUnntakFraLovvalgsland(Landkoder.SE);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1L));
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setUnntakFraBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1);
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Collections.singletonList(lovvalgsperiode));

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        VilkaarBegrunnelse vilkaarBegrunnelse = new VilkaarBegrunnelse();
        vilkaarBegrunnelse.setKode("SOEKT_FOR_SENT");
        vilkaarsresultat.setBegrunnelser(new HashSet<>(Collections.singletonList(vilkaarBegrunnelse)));
        behandlingsresultat.setVilkaarsresultater(Collections.singleton(vilkaarsresultat));
        lovvalgsperiode.setBehandlingsresultat(behandlingsresultat);

        behandling = DataByggerStubs.hentBehandlingStub();

        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService);
        dataBygger = new SedDataBygger(kodeverkService, lovvalgsperiodeService, avklartefaktaService, avklarteVirksomheterService);
    }

    @Test
    public void testHentAvklarteSelvstendigeForetak()
        throws FunksjonellException, TekniskException {
        SedDataDto sedData = dataBygger.lag(behandling);

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
}
