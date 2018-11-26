package no.nav.melosys.saksflyt.agent.iv;

import java.util.Properties;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingstype;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.BrevDataByggerA1;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.ProsessSteg.GSAK_AVSLUTT_OPPGAVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class IverksettVedtakSendBrevTest {

    private IverksettVedtakSendBrev agent;

    @Before
    public void setUp() {
        DokumentSystemService dokumentService = mock(DokumentSystemService.class);
        BrevDataByggerA1 brevDataByggerA1 = mock(BrevDataByggerA1.class);

        agent = new IverksettVedtakSendBrev(dokumentService, brevDataByggerA1);
    }

    @Test
    public void utfoerSteg() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstype.SØKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);
        Properties properties = new Properties();
        p.addData(properties);

        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(GSAK_AVSLUTT_OPPGAVE);
    }
}