package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.saksflyt.felles.HentOpplysningerFelles;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HentPersonopplysningerTest {

    @Mock
    private HentOpplysningerFelles hentOpplysningerFelles;

    private HentPersonopplysninger hentPersonopplysninger;

    @Before
    public void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        hentPersonopplysninger = new HentPersonopplysninger(hentOpplysningerFelles);
        when(hentOpplysningerFelles.hentOgLagrePersonopplysninger(anyString(), any())).thenReturn("321");
    }

    @Test
    public void utfør_verifiserIdentOgPersonHentet() throws Exception {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "123321");
        hentPersonopplysninger.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_HENT_MEDLEMSKAP);
        verify(hentOpplysningerFelles).hentOgLagrePersonopplysninger(eq("123321"), eq(behandling));
    }
}