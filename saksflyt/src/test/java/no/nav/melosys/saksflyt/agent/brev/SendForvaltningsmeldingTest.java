package no.nav.melosys.saksflyt.agent.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SendForvaltningsmeldingTest {

    private SendForvaltningsmelding agent;

    @Mock
    private BrevBestiller brevBestiller;

    @Before
    public void setUp() {
        agent = new SendForvaltningsmelding(brevBestiller);
    }

    @Test
    public void utfoerSteg() throws TekniskException, FunksjonellException {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        p.setBehandling(behandling);
        p.setData(ProsessDataKey.SAKSBEHANDLER, "TEST");

        agent.utførSteg(p);

        verify(brevBestiller).bestill(eq(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID), anyString(), eq(Mottaker.av(Aktoersroller.BRUKER)), any(Behandling.class));

        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FERDIG);
    }
}
