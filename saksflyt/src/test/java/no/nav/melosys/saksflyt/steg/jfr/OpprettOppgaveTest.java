package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.FERDIG;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.SEND_FORVALTNINGSMELDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OpprettOppgaveTest {
    @Mock
    private OppgaveService oppgaveService;

    private OpprettOppgave agent;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private Prosessinstans prosessinstans;
    private Behandling behandling;

    private final String aktørID = "22222";
    private final String journalpostID = "43235";

    @Before
    public void setUp() {
        agent = new OpprettOppgave(oppgaveService);

        prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostID);
        behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);

        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);

        behandling.setFagsak(fagsak);
        prosessinstans.setBehandling(behandling);
    }

    @Test
    public void utfoerSteg_nySak_sendForvaltningsmelding() throws FunksjonellException, TekniskException {
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, true);
        prosessinstans.setType(ProsessType.JFR_NY_SAK);

        agent.utfør(prosessinstans);

        verify(oppgaveService).opprettBehandlingsoppgave(eq(behandling), eq(journalpostID), eq(aktørID), isNull());
        assertThat(prosessinstans.getSteg()).isEqualTo(SEND_FORVALTNINGSMELDING);
    }

    @Test
    public void utfoerSteg_skalSendesForvaltningsmeldingFalse_ikkeSendForvaltningsmelding() throws FunksjonellException, TekniskException {
        prosessinstans.setType(ProsessType.JFR_NY_SAK);
        prosessinstans.setData(ProsessDataKey.SKAL_SENDES_FORVALTNINGSMELDING, false);

        agent.utfør(prosessinstans);

        verify(oppgaveService).opprettBehandlingsoppgave(eq(behandling), eq(journalpostID), eq(aktørID), isNull());
        assertThat(prosessinstans.getSteg()).isEqualTo(FERDIG);
    }

    @Test
    public void utfoerSteg_endretPeriode_ikkeSendForvaltningsmelding() throws FunksjonellException, TekniskException {

        behandling.setType(Behandlingstyper.ENDRET_PERIODE);
        prosessinstans.setType(ProsessType.JFR_NY_BEHANDLING);

        agent.utfør(prosessinstans);

        verify(oppgaveService).opprettBehandlingsoppgave(eq(behandling), eq(journalpostID), eq(aktørID), isNull());
        assertThat(prosessinstans.getSteg()).isEqualTo(FERDIG);
    }


    @Test
    public void utfoerSteg_skalTilordnes_setterTilordnetRessurs() throws FunksjonellException, TekniskException {
        prosessinstans.setType(ProsessType.JFR_NY_SAK);

        String saksbehandler = "bruker";
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, true);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);

        agent.utfør(prosessinstans);

        verify(oppgaveService).opprettBehandlingsoppgave(eq(behandling), eq(journalpostID), eq(aktørID), eq(saksbehandler));
    }

    @Test
    public void utfoerSteg_nyBehandling_tilFerdig() throws FunksjonellException, TekniskException {
        prosessinstans.setType(ProsessType.JFR_NY_BEHANDLING);

        agent.utfør(prosessinstans);

        verify(oppgaveService).opprettBehandlingsoppgave(eq(behandling), eq(journalpostID), eq(aktørID), isNull());
        assertThat(prosessinstans.getSteg()).isEqualTo(FERDIG);
    }

    @Test
    public void utfoerSteg_feilSakstype_feiler() throws FunksjonellException, TekniskException {
        behandling.getFagsak().setType(Sakstyper.FTRL);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage(Sakstyper.FTRL + " er ikke støttet");
        agent.utfør(prosessinstans);
    }

    @Test
    public void utfoerSteg_feilProsessType_feiler() throws FunksjonellException, TekniskException {
        prosessinstans.setType(ProsessType.MANGELBREV);

        expectedException.expect(TekniskException.class);
        expectedException.expectMessage(ProsessType.MANGELBREV + " er ikke støttet");
        agent.utfør(prosessinstans);
    }
}