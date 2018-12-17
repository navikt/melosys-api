package no.nav.melosys.saksflyt.agent.brev;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProduserbartDokument;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.BrevData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SendForvaltningsmeldingTest {

    private SendForvaltningsmelding agent;

    private DokumentSystemService dokumentService;

    @Before
    public void setUp() throws Exception {
        dokumentService = mock(DokumentSystemService.class);
        agent = new SendForvaltningsmelding(dokumentService);
    }

    @Test
    public void utfoerSteg() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException, FunksjonellException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.setData(ProsessDataKey.SAKSBEHANDLER, "TEST");

        agent.utførSteg(p);

        verify(dokumentService, times(1)).produserDokument(anyLong(), any(ProduserbartDokument.class), any(BrevData.class));

        assertThat(p.getSteg()).isNull();
    }
}
