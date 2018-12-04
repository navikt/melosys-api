package no.nav.melosys.saksflyt.agent.iv;

import java.util.Properties;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingstype;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataByggerA1;
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.ProsessSteg.GSAK_AVSLUTT_OPPGAVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IverksettVedtakSendBrevTest {

    private IverksettVedtakSendBrev agent;

    @Before
    public void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        DokumentSystemService dokumentService = mock(DokumentSystemService.class);

        BrevData brevdata = new BrevDataA1("Z123456");
        BrevDataByggerA1 brevDataByggerA1 = mock(BrevDataByggerA1.class);
        when(brevDataByggerA1.lag(any(), any())).thenReturn(brevdata);

        BrevDataByggerVelger byggerVelger = mock(BrevDataByggerVelger.class);
        when(byggerVelger.hent(any())).thenReturn(brevDataByggerA1);

        BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
        when(behandlingRepository.findOneWithSaksopplysningerById(any())).thenReturn(new Behandling());
        agent = new IverksettVedtakSendBrev(dokumentService, byggerVelger, behandlingRepository);
    }

    @Test
    public void utfoerSteg() {
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