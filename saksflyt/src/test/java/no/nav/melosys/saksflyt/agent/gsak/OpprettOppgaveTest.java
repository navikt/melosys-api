package no.nav.melosys.saksflyt.agent.gsak;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
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
import static no.nav.melosys.domain.oppgave.Behandlingstema.ARB_EØS;
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
    public void utfoerSteg_nySak_sendForvaltningsmelding() throws FunksjonellException, TekniskException {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "MEL-TESTx";
        fagsak.setSaksnummer(saksnummer);
        fagsak.setType(Sakstyper.EU_EOS);
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setFagsak(fagsak);

        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        p.getBehandling().setType(Behandlingstyper.SOEKNAD);
        p.setType(ProsessType.JFR_NY_SAK);

        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        agent.utførSteg(p);

        verify(gsakFasade, times(1)).opprettOppgave(oppgave.capture());

        assertThat(oppgave.getValue().getSaksnummer()).isEqualTo(saksnummer);
        assertThat(oppgave.getValue().getBehandlingstema()).isEqualTo(ARB_EØS);
        assertThat(p.getSteg()).isEqualTo(SEND_FORVALTNINGSMELDING);
    }

    @Test
    public void utfoerSteg_endretPeriode_ikkeSendForvaltningsmelding() throws FunksjonellException, TekniskException {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "MEL-TESTx";
        fagsak.setSaksnummer(saksnummer);
        fagsak.setType(Sakstyper.EU_EOS);
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setType(Behandlingstyper.ENDRET_PERIODE);
        behandling.setFagsak(fagsak);

        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        p.getBehandling().setType(Behandlingstyper.ENDRET_PERIODE);
        p.setType(ProsessType.JFR_NY_BEHANDLING);

        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        agent.utførSteg(p);

        verify(gsakFasade, times(1)).opprettOppgave(oppgave.capture());

        assertThat(oppgave.getValue().getSaksnummer()).isEqualTo(saksnummer);
        assertThat(oppgave.getValue().getBehandlingstema()).isEqualTo(ARB_EØS);
        assertThat(p.getSteg()).isEqualTo(null);
    }

    @Test
    public void utfoerSteg_nyBehandling_tilNull() throws FunksjonellException, TekniskException {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "MEL-TESTx";
        fagsak.setSaksnummer(saksnummer);
        fagsak.setType(Sakstyper.EU_EOS);
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setFagsak(fagsak);

        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);
        p.setType(ProsessType.JFR_NY_BEHANDLING);

        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        agent.utførSteg(p);

        verify(gsakFasade, times(1)).opprettOppgave(oppgave.capture());

        assertThat(oppgave.getValue().getSaksnummer()).isEqualTo(saksnummer);
        assertThat(oppgave.getValue().getBehandlingstema()).isEqualTo(ARB_EØS);
        assertThat(p.getSteg()).isNull();
    }

    @Test
    public void utfoerSteg_feilSakstype_feiler() {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "MEL-TESTx";
        fagsak.setSaksnummer(saksnummer);
        fagsak.setType(Sakstyper.FTRL);
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);

        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(FEILET_MASKINELT);
    }

    @Test
    public void utfoerSteg_feilBehandlingstype_feiler() {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "MEL-TESTx";
        fagsak.setSaksnummer(saksnummer);
        fagsak.setType(Sakstyper.EU_EOS);
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setType(Behandlingstyper.NORGE_UTPEKT);
        behandling.setFagsak(fagsak);

        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(FEILET_MASKINELT);
    }

    @Test
    public void utfoerSteg_feilProsessType_feiler() {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "MEL-TESTx";
        fagsak.setSaksnummer(saksnummer);
        fagsak.setType(Sakstyper.EU_EOS);
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setFagsak(fagsak);

        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.MANGELBREV);
        p.setBehandling(behandling);

        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(FEILET_MASKINELT);
    }
}