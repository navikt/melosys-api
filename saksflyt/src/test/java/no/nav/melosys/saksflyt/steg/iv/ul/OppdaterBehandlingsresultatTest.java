package no.nav.melosys.saksflyt.steg.iv.ul;


import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Utpekingsperiode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.iv.ul.OppdaterBehandlingsresultat;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.utpeking.UtpekingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterBehandlingsresultatTest {

    @Mock
    private UtpekingService utpekingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private OppdaterBehandlingsresultat oppdaterBehandlingsresultat;

    @Before
    public void setup() {
        oppdaterBehandlingsresultat = new OppdaterBehandlingsresultat(utpekingService, behandlingsresultatService);
    }

    @Test
    public void utfør() throws MelosysException {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        Utpekingsperiode utpekingsperiode = new Utpekingsperiode();
        utpekingsperiode.setMedlPeriodeID(123L);
        behandlingsresultat.getUtpekingsperioder().add(utpekingsperiode);

        Behandling behandling = new Behandling();
        behandling.setId(123L);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandling.getId()))).thenReturn(behandlingsresultat);

        oppdaterBehandlingsresultat.utfør(prosessinstans);

        verify(utpekingService).oppdaterSendtUtland(eq(utpekingsperiode));
        verify(behandlingsresultatService).lagre(eq(behandlingsresultat));

        assertThat(behandlingsresultat.getType()).isEqualTo(Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND);
        assertThat(behandlingsresultat.getFastsattAvLand()).isEqualTo(Landkoder.NO);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);

    }
}