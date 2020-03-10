package no.nav.melosys.saksflyt.steg.aou.inn;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.service.medl.MedlPeriodeService;
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
public class OpprettPeriodeIMedlTest {

    @Mock
    private MedlFasade medlFasade;
    @Mock
    private MedlPeriodeService medlPeriodeService;

    private OpprettPeriodeIMedl opprettPeriodeIMedl;

    @Before
    public void setup() {
        opprettPeriodeIMedl = new OpprettPeriodeIMedl(medlFasade, medlPeriodeService);
    }

    @Test
    public void utfør() throws FunksjonellException, TekniskException {
        when(medlPeriodeService.hentAnmodningsperiode(any())).thenReturn(new Anmodningsperiode());
        when(medlPeriodeService.hentFnr(any())).thenReturn("123");
        when(medlFasade.opprettPeriodeUnderAvklaring(anyString(), any(), any())).thenReturn(987L);

        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        prosessinstans.setBehandling(behandling);

        opprettPeriodeIMedl.utfør(prosessinstans);

        verify(medlPeriodeService).hentAnmodningsperiode(any());
        verify(medlPeriodeService).hentFnr(any());
        verify(medlFasade).opprettPeriodeUnderAvklaring(anyString(), any(), any());
        verify(medlPeriodeService).lagreMedlPeriodeId(anyLong(), any(Anmodningsperiode.class), anyLong());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_OPPRETT_OPPGAVE);
    }
}