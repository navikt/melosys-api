package no.nav.melosys.saksflyt.agent.gsak;

import java.util.Properties;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.BehandlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.ProsessSteg.FEILET_MASKINELT;
import static no.nav.melosys.domain.ProsessSteg.SEND_FORVALTNINGSMELDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OpprettOppgaveTest {

    @Mock
    private BehandlingRepository behandlingRepository;

    @Mock
    private GsakFasade gsakFasade;

    private OpprettOppgave agent;

    @Captor
    private ArgumentCaptor<Oppgave> oppgave;

    @Before
    public void setUp() {
        agent = new OpprettOppgave(behandlingRepository, gsakFasade);
    }

    @Test
    public void utfoerSteg() throws FunksjonellException, TekniskException {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "MEL-TESTx";
        fagsak.setSaksnummer(saksnummer);
        fagsak.setType(Fagsakstype.EU_EØS);
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);

        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);
        p.getBehandling().setType(Behandlingstype.SØKNAD);
        p.setType(ProsessType.JFR_NY_SAK);
        Properties properties = new Properties();
        p.addData(properties);

        when(behandlingRepository.findOneWithSaksopplysningerById(anyLong())).thenReturn(behandling);

        agent.utførSteg(p);

        verify(gsakFasade, times(1)).opprettOppgave(oppgave.capture());

        assertThat(oppgave.getValue().getSaksnummer()).isEqualTo(saksnummer);
        assertThat(oppgave.getValue().getBehandlingstema()).isEqualTo(null);
        assertThat(p.getSteg()).isEqualTo(SEND_FORVALTNINGSMELDING);
    }

    @Test
    public void utfoerSteg_feilSakstype() {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "MEL-TESTx";
        fagsak.setSaksnummer(saksnummer);
        fagsak.setType(Fagsakstype.FOLKETRYGD);
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);

        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        when(behandlingRepository.findOneWithSaksopplysningerById(anyLong())).thenReturn(behandling);

        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(FEILET_MASKINELT);
    }

    @Test
    public void utfoerSteg_feilBehandlingstype() {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "MEL-TESTx";
        fagsak.setSaksnummer(saksnummer);
        fagsak.setType(Fagsakstype.EU_EØS);
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setType(Behandlingstype.NORGE_UTPEKT);
        behandling.setFagsak(fagsak);

        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        when(behandlingRepository.findOneWithSaksopplysningerById(anyLong())).thenReturn(behandling);

        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(FEILET_MASKINELT);
    }
}