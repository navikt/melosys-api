package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.*;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HentPersonopplysningerTest {

    @Mock
    private TpsFasade tpsFasade;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private SaksopplysningRepository saksopplysningRepository;

    private HentPersonopplysninger hentPersonopplysninger;

    @Before
    public void setUp() throws Exception {
        hentPersonopplysninger = new HentPersonopplysninger(tpsFasade, fagsakService, saksopplysningRepository);
        when(tpsFasade.hentIdentForAktørId(anyString())).thenReturn("432234");
        when(tpsFasade.hentPerson(anyString())).thenReturn(new Saksopplysning());
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
        verify(saksopplysningRepository).save(any(Saksopplysning.class));
    }
}