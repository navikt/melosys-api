package no.nav.melosys.service.dokument.sed.bygger;

import java.util.Collection;
import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.exception.*;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.sed.A009Data;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class A009DataByggerTest {

    private A009DataBygger a009DataBygger;
    private Behandling behandling;

    @Before
    public void setup() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        KodeverkService kodeverkService = mock(KodeverkService.class);
        RegisterOppslagService registerOppslagService = mock(RegisterOppslagService.class);
        LovvalgsperiodeService lovvalgsperiodeService = mock(LovvalgsperiodeService.class);
        AvklartefaktaService avklartefaktaService = mock(AvklartefaktaService.class);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1);
        Collection<Lovvalgsperiode> lovvalgsperioder = Collections.singletonList(lovvalgsperiode);
        doReturn(lovvalgsperioder).when(lovvalgsperiodeService).hentLovvalgsperioder(anyLong());
        doReturn(DataByggerStubs.hentOrganisasjonDokumentSetStub()).when(registerOppslagService).hentOrganisasjoner(anySet());

        behandling = DataByggerStubs.hentBehandlingStub();

        a009DataBygger = new A009DataBygger(kodeverkService, registerOppslagService, lovvalgsperiodeService, avklartefaktaService);
    }

    @Test
    public void lagA009DataObjekt_forventLovvalgBestemmelse12_1() throws FunksjonellException, TekniskException {
        A009Data a009Data = (A009Data) a009DataBygger.lag(behandling);
        assertNotNull(a009Data.getLovvalgsperiode());
        assertEquals(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1, a009Data.getLovvalgsperiode().getBestemmelse());

    }


}
