package no.nav.melosys.saksflyt.agent.iv;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.SedService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class IverksettVedtakSendSedTest {

    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private SedService sedService;

    @InjectMocks
    private IverksettVedtakSendSed iverksettVedtakSendSed;

    private Prosessinstans prosessinstans;

    @Before
    public void setUp() throws Exception {
        prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(new Behandling());
        prosessinstans.getBehandling().setId(1L);
        when(behandlingRepository.findWithSaksopplysningerById(anyLong())).thenReturn(prosessinstans.getBehandling());

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.setLovvalgsperioder(Sets.newHashSet(lovvalgsperiode));
        behandlingsresultat.setType(BehandlingsresultatType.FASTSATT_LOVVALGSLAND);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
    }

    @Test
    public void utførSteg_suksessfull_ingenRetur() throws Exception{
        iverksettVedtakSendSed.utfør(prosessinstans);
        verify(sedService).opprettOgSendSed(any(Behandling.class), any(Behandlingsresultat.class));
        assertThat(prosessinstans.getSteg(), is(ProsessSteg.IV_AVSLUTT_BEHANDLING));
    }

    @Test
    public void utførSteg_feilIMetode_settFeiletSteg() throws Exception {
        doThrow(new TekniskException("feil")).when(sedService).opprettOgSendSed(any(Behandling.class), any(Behandlingsresultat.class));
        iverksettVedtakSendSed.utfør(prosessinstans);
        assertThat(prosessinstans.getSteg(), is(ProsessSteg.FEILET_MASKINELT));
    }
}