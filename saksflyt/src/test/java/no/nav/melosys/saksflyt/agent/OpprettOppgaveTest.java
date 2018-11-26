package no.nav.melosys.saksflyt.agent;

import java.util.Properties;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.oppgave.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OpprettOppgaveTest {

    @Mock
    private GsakFasade gsakFasade;

    private OpprettOppgave agent;

    @Captor
    private ArgumentCaptor<Oppgave> oppgave;

    @Before
    public void setUp() {
        agent = new OpprettOppgave(gsakFasade);
    }

    @Test
    public void utfoerSteg() throws FunksjonellException, TekniskException {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "MEL-TESTx";
        fagsak.setSaksnummer(saksnummer);
        fagsak.setType(Fagsakstype.EU_EØS);
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);

        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);
        p.getBehandling().setType(Behandlingstype.SØKNAD);
        p.setType(ProsessType.JFR_NY_SAK);
        Properties properties = new Properties();
        p.addData(properties);

        agent.utførSteg(p);

        verify(gsakFasade, times(1)).opprettOppgave(oppgave.capture());

        assertThat(oppgave.getValue().getSaksnummer()).isEqualTo(saksnummer);
        assertThat(oppgave.getValue().getBehandlingstema()).isEqualTo(Behandlingstema.ARB_EØS);
        assertThat(p.getSteg()).isEqualTo(SEND_FORVALTNINGSMELDING);
    }

    @Test
    public void utfoerSteg_feilSakstype() {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "MEL-TESTx";
        fagsak.setSaksnummer(saksnummer);
        fagsak.setType(Fagsakstype.FOLKETRYGD);
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);

        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

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
        behandling.setType(Behandlingstype.NORGE_UTPEKT);
        behandling.setFagsak(fagsak);

        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(FEILET_MASKINELT);
    }
}