package no.nav.melosys.saksflyt.steg.iv;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class IverksettVedtakSendSedTest {

    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private EessiService eessiService;

    @InjectMocks
    private IverksettVedtakSendSed iverksettVedtakSendSed;

    private Prosessinstans prosessinstans;
    
    private Lovvalgsperiode lovvalgsperiode;

    @Before
    public void setUp() throws Exception {
        prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(new Behandling());
        prosessinstans.getBehandling().setId(1L);
        when(behandlingRepository.findWithSaksopplysningerById(anyLong())).thenReturn(prosessinstans.getBehandling());

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.setLovvalgsperioder(Sets.newHashSet(lovvalgsperiode));
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
    }

    @Test
    public void utførSteg_suksessfull_statusErAvgiftsoppgave() throws Exception{
        iverksettVedtakSendSed.utfør(prosessinstans);
        verify(eessiService).opprettOgSendSed(any(Behandling.class), any(Behandlingsresultat.class), any(BucType.class));
        assertThat(prosessinstans.getSteg(), is(ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE));
    }

    @Test
    public void utførStegForArtikkel11_suksessfull_statusErAvsluttBehandling() throws Exception {
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        iverksettVedtakSendSed.utfør(prosessinstans);
        verify(eessiService).opprettOgSendSed(any(Behandling.class), any(Behandlingsresultat.class), any(BucType.class));
        assertThat(prosessinstans.getSteg(), is(ProsessSteg.IV_AVSLUTT_BEHANDLING));
    }
}