package no.nav.melosys.saksflyt.steg.gsak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.service.BehandlingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.oppgave.Behandlingstema.EU_EOS;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OpprettOppgaveTest {
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private GsakFasade gsakFasade;

    private OpprettOppgave agent;

    @Captor
    private ArgumentCaptor<Oppgave> oppgave;

    @Before
    public void setUp() {
        agent = new OpprettOppgave(behandlingService, gsakFasade);
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

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        agent.utførSteg(p);

        verify(gsakFasade).opprettOppgave(oppgave.capture());

        assertThat(oppgave.getValue().getSaksnummer()).isEqualTo(saksnummer);
        assertThat(oppgave.getValue().getBehandlingstema()).isEqualTo(EU_EOS);
        assertThat(p.getSteg()).isEqualTo(SEND_FORVALTNINGSMELDING);
    }

    @Test
    public void utfoerSteg_skalSendesForvaltningsmeldingFalse_ikkeSendForvaltningsmelding() throws FunksjonellException, TekniskException {
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
        p.setData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, false);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        agent.utførSteg(p);

        verify(gsakFasade).opprettOppgave(oppgave.capture());

        assertThat(oppgave.getValue().getSaksnummer()).isEqualTo(saksnummer);
        assertThat(oppgave.getValue().getBehandlingstema()).isEqualTo(EU_EOS);
        assertThat(p.getSteg()).isEqualTo(FERDIG);
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

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        agent.utførSteg(p);

        verify(gsakFasade).opprettOppgave(oppgave.capture());

        assertThat(oppgave.getValue().getSaksnummer()).isEqualTo(saksnummer);
        assertThat(oppgave.getValue().getBehandlingstema()).isEqualTo(EU_EOS);
        assertThat(p.getSteg()).isEqualTo(FERDIG);
    }


    @Test
    public void utfoerSteg_skalTilordnes_setterTilordnetRessurs() throws FunksjonellException, TekniskException {
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

        String bruker = "bruker";
        p.setData(ProsessDataKey.SKAL_TILORDNES, true);
        p.setData(ProsessDataKey.SAKSBEHANDLER, bruker);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        agent.utførSteg(p);

        verify(gsakFasade).opprettOppgave(oppgave.capture());

        assertThat(oppgave.getValue().getTilordnetRessurs()).isEqualTo(bruker);
    }

    @Test
    public void utfoerSteg_nyBehandling_tilFerdig() throws FunksjonellException, TekniskException {
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

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        agent.utførSteg(p);

        verify(gsakFasade).opprettOppgave(oppgave.capture());

        assertThat(oppgave.getValue().getSaksnummer()).isEqualTo(saksnummer);
        assertThat(oppgave.getValue().getBehandlingstema()).isEqualTo(EU_EOS);
        assertThat(p.getSteg()).isEqualTo(FERDIG);
    }

    @Test
    public void utfoerSteg_feilSakstype_feiler() throws IkkeFunnetException {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "MEL-TESTx";
        fagsak.setSaksnummer(saksnummer);
        fagsak.setType(Sakstyper.FTRL);
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);

        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(FEILET_MASKINELT);
    }

    @Test
    public void utfoerSteg_feilBehandlingstype_feiler() throws IkkeFunnetException {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "MEL-TESTx";
        fagsak.setSaksnummer(saksnummer);
        fagsak.setType(Sakstyper.EU_EOS);
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setType(Behandlingstyper.BESLUTNING_LOVVALG_NORGE);
        behandling.setFagsak(fagsak);

        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(FEILET_MASKINELT);
    }

    @Test
    public void utfoerSteg_feilProsessType_feiler() throws IkkeFunnetException {
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

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(FEILET_MASKINELT);
    }
}