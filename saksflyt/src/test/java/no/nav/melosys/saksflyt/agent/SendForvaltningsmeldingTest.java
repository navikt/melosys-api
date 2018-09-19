package no.nav.melosys.saksflyt.agent;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.DokumentService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SendForvaltningsmeldingTest {

    private SendForvaltningsmelding agent;

    private DokumentService dokumentService;

    @Before
    public void setUp() throws Exception {
        dokumentService = mock(DokumentService.class);
        agent = new SendForvaltningsmelding(dokumentService);
    }

    @Test
    public void utfoerSteg() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.setData(ProsessDataKey.SAKSBEHANDLER, "TEST");

        agent.utførSteg(p);

        verify(dokumentService, times(1)).produserDokument(anyLong(), anyString(), anyString());

        assertThat(p.getSteg()).isNull();
    }
}
