package no.nav.melosys.service.dokument.sed.bygger;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.*;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.sed.AbstraktSedData;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class AbstraktSedDataByggerTest {

    private AbstraktSedDataBygger dataBygger;
    private Behandling behandling;

    class AbstraktSedDataByggerStub extends AbstraktSedDataBygger {

        AbstraktSedDataByggerStub(KodeverkService kodeverkService,
                                  RegisterOppslagService registerOppslagService,
                                  LovvalgsperiodeService lovvalgsperiodeService,
                                  AvklartefaktaService avklartefaktaService) {
            super(kodeverkService, registerOppslagService, lovvalgsperiodeService, avklartefaktaService);
        }

        @Override
        public AbstraktSedData lag(Behandling behandling) throws TekniskException, FunksjonellException {
            return null;
        }
    }

    private class SedDataImpl extends AbstraktSedData {}

    @Before
    @SuppressWarnings("unchecked")
    public void setup()
        throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        KodeverkService kodeverkService = mock(KodeverkService.class);
        RegisterOppslagService registerOppslagService = mock(RegisterOppslagService.class);
        LovvalgsperiodeService lovvalgsperiodeService = mock(LovvalgsperiodeService.class);
        AvklartefaktaService avklartefaktaService = mock(AvklartefaktaService.class);

        doReturn(DataByggerStubs.hentOrganisasjonDokumentSetStub()).when(registerOppslagService).hentOrganisasjoner(anySet());

        behandling = DataByggerStubs.hentBehandlingStub();

        dataBygger = new AbstraktSedDataByggerStub(kodeverkService, registerOppslagService, lovvalgsperiodeService, avklartefaktaService);
    }

    @Test
    public void testHentAvklarteSelvstendigeForetak()
        throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AbstraktSedData sedData = dataBygger.lag(behandling, new SedDataImpl());

        assertNotNull(sedData);
        assertNotNull(sedData.getPersonDokument());
        assertNotNull(sedData.getArbeidsgivendeVirksomheter());
        assertNotNull(sedData.getArbeidssteder());
        assertNotNull(sedData.getBostedsadresse());
        assertNotNull(sedData.getSelvstendigeVirksomheter());
        assertNotNull(sedData.getSøknadDokument());
        assertNotNull(sedData.getUtenlandskeVirksomheter());

        assertFalse(sedData.getArbeidsgivendeVirksomheter().isEmpty());
        assertEquals("Land", sedData.getArbeidsgivendeVirksomheter().get(0).adresse.landKode);
    }
}
