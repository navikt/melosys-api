package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.service.behandling.BehandlingService;
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
public class SendForvaltningsmeldingTest {

    private SendForvaltningsmelding agent;

    @Mock
    private BrevBestiller brevBestiller;

    @Mock
    private BehandlingService behandlingService;

    @Before
    public void setUp() {
        agent = new SendForvaltningsmelding(brevBestiller, behandlingService);
    }

    @Test
    public void utfoerSteg() throws TekniskException, FunksjonellException {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        p.setBehandling(behandling);
        p.setData(ProsessDataKey.SAKSBEHANDLER, "TEST");

        agent.utførSteg(p);

        verify(behandlingService).hentBehandling(1L);
        verify(brevBestiller).bestill(eq(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID), anyString(), eq(Mottaker.av(Aktoersroller.BRUKER)), any(Behandling.class));

        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FERDIG);
    }
}
