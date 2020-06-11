package no.nav.melosys.saksflyt.steg.iv.ul;


import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Utpekingsperiode;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.iv.ul.OppdaterMedl;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterMedlTest {

    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private MedlPeriodeService medlPeriodeService;

    private OppdaterMedl oppdaterMedl;

    private Prosessinstans prosessinstans;
    private Utpekingsperiode utpekingsperiode;
    private Behandling behandling;

    @Before
    public void settOpp() throws IkkeFunnetException {
        oppdaterMedl = new OppdaterMedl(behandlingsresultatService, medlPeriodeService);

        behandling = new Behandling();
        behandling.setId(0L);

        prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        utpekingsperiode = new Utpekingsperiode();

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.getUtpekingsperioder().add(utpekingsperiode);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
    }

    @Test
    public void utfør() throws MelosysException {
        oppdaterMedl.utfør(prosessinstans);
        verify(medlPeriodeService).opprettPeriodeForeløpig(eq(utpekingsperiode), eq(behandling.getId()), eq(false));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.UL_SEND_ORIENTERINGSBREV);
    }
}