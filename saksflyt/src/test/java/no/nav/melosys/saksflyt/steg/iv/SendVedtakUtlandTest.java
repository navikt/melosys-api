package no.nav.melosys.saksflyt.steg.iv;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SendVedtakUtlandTest {
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private EessiService eessiService;

    private SendVedtakUtland sendVedtakUtland;

    private Prosessinstans prosessinstans;
    private Lovvalgsperiode lovvalgsperiode;

    @Before
    public void setUp() throws Exception {
        prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(new Behandling());
        prosessinstans.getBehandling().setId(1L);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(prosessinstans.getBehandling());

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.setLovvalgsperioder(Sets.newHashSet(lovvalgsperiode));
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        sendVedtakUtland = new SendVedtakUtland(behandlingService, eessiService, behandlingsresultatService);
    }

    @Test
    public void utførSteg_suksessfull_statusErAvgiftsoppgave() throws Exception{
        sendVedtakUtland.utfør(prosessinstans);
        verify(eessiService).opprettOgSendSed(any(Behandling.class), any(Behandlingsresultat.class));
        assertThat(prosessinstans.getSteg(), is(ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE));
    }

    @Test
    public void utførStegForArtikkel11_suksessfull_statusErAvsluttBehandling() throws Exception {
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        sendVedtakUtland.utfør(prosessinstans);
        verify(eessiService).opprettOgSendSed(any(Behandling.class), any(Behandlingsresultat.class));
        assertThat(prosessinstans.getSteg(), is(ProsessSteg.IV_AVSLUTT_BEHANDLING));
    }
}