package no.nav.melosys.saksflyt.agent.aou;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.SedService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SendSedTest {

    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private SedService sedService;

    @InjectMocks
    private SendSed sendSed;

    private Prosessinstans prosessinstans;

    @Before
    public void setUp() {
        prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(new Behandling());
        prosessinstans.getBehandling().setId(1L);
        when(behandlingRepository.findWithSaksopplysningerById(anyLong())).thenReturn(prosessinstans.getBehandling());
    }

    @Test
    public void utføer_artikkel16_verifiserStegFerdig() throws Exception {
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(eq(1L))).thenReturn(behandlingsresultat);

        sendSed.utfør(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);
        verify(sedService).opprettOgSendSed(any(Behandling.class), any(Behandlingsresultat.class));
    }

    @Test
    public void utfør_artikkel12_verifiserSedIkkeSendt() throws Exception {
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat();
        behandlingsresultat.getLovvalgsperioder().iterator().next().setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        when(behandlingsresultatService.hentBehandlingsresultat(eq(2L))).thenReturn(behandlingsresultat);
        prosessinstans.getBehandling().setId(2L);

        sendSed.utfør(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);
        verify(sedService, never()).opprettOgSendSed(any(), any());
    }

    private static Behandlingsresultat hentBehandlingsresultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.setLovvalgsperioder(Sets.newHashSet(lovvalgsperiode));
        behandlingsresultat.setType(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
        return behandlingsresultat;
    }
}