package no.nav.melosys.service.dokument.sed.bygger;

import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Landkoder;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.exception.*;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.sed.dto.SedDataDto;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
        lovvalgsperiode.setLovvalgsland(Landkoder.SE);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1L));
        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1);
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Collections.singletonList(lovvalgsperiode));

        behandling = DataByggerStubs.hentBehandlingStub();

        dataBygger = new SedDataBygger(kodeverkService, registerOppslagService, lovvalgsperiodeService, avklartefaktaService);
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

        assertFalse(sedData.getArbeidsgivendeVirksomheter().isEmpty());
    }
}
