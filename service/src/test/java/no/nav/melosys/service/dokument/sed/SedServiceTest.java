package no.nav.melosys.service.dokument.sed;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Ignore
@RunWith(MockitoJUnitRunner.class)
public class SedServiceTest {

    @Mock
    private FagsakRepository fagsakRepository;
    @Mock
    private SedDataByggerVelger sedDataByggerVelger;

    @InjectMocks
    private SedService sedService;

    private Behandling behandling;
    private Behandlingsresultat behandlingsresultat;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() throws Exception {

        behandling = new Behandling();
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().setSaksnummer("123");

        behandlingsresultat = new Behandlingsresultat();

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setLovvalgsland(Landkoder.SK);
        behandlingsresultat.setLovvalgsperioder(Sets.newHashSet(lovvalgsperiode));

        SedDataBygger dataBygger = Mockito.mock(SedDataBygger.class);
        when(sedDataByggerVelger.hent(any(LovvalgBestemmelse.class))).thenReturn(dataBygger);

        //A009Data a009Data = SedDataStub.hent(new A009Data());
        //a009Data.setLovvalgsperioder(new Lovvalgsperiode());
        //a009Data.getLovvalgsperiode().setLovvalgsland(Landkoder.SK);
        //a009Data.getLovvalgsperiode().setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1);
        //a009Data.getLovvalgsperiode().setFom(LocalDate.now());
        //a009Data.getLovvalgsperiode().setTom(LocalDate.now());
        //when(dataBygger.lag(any(Behandling.class))).thenReturn(a009Data);


        Map<String, String> rinaInfo = Maps.newHashMap();
        rinaInfo.put("caseId","3333");
        rinaInfo.put("documentId", "aaa");
    }

    @Test
    public void testPlaceholder() throws Exception {

    }
/*
    @Test
    public void opprettOgSendSed_verifiserKorrektSedType() throws Exception {
        sedService.opprettOgSendSed(behandling, behandlingsresultat);
    }

    @Test
    public void opprettOgSendSed_ikkeStøttetBestemmelse_forventException() throws Exception {
        expectedException.expect(RuntimeException.class);
        behandlingsresultat.getLovvalgsperioder().iterator().next().setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART11_3E);
        sedService.opprettOgSendSed(behandling, behandlingsresultat);
    }

    @Test
    public void opprettOgSendSed_ingenLovvalgsperiode_forventException() throws Exception {
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("Finner ingen lovvalgsperiode!");
        behandlingsresultat.setLovvalgsperioder(Sets.newHashSet());
        sedService.opprettOgSendSed(behandling, behandlingsresultat);
    }*/
}