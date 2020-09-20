package no.nav.melosys.saksflyt.steg.aou.inn;

import java.util.Set;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpprettPeriodeIMedlTest {

    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private MedlPeriodeService medlPeriodeService;

    private OpprettPeriodeIMedl opprettPeriodeIMedl;

    @Before
    public void setup() {
        opprettPeriodeIMedl = new OpprettPeriodeIMedl(behandlingsresultatService, medlPeriodeService);
    }

    @Test
    public void utfør() throws FunksjonellException, TekniskException {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        prosessinstans.setBehandling(behandling);

        opprettPeriodeIMedl.utfør(prosessinstans);

        verify(behandlingsresultatService).hentBehandlingsresultat(eq(1L));
        verify(medlPeriodeService).opprettPeriodeUnderAvklaring(eq(anmodningsperiode), anyLong(), eq(true));
        //assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_OPPRETT_OPPGAVE);
    }
}