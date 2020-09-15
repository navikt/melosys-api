package no.nav.melosys.saksflyt.steg.jfr;

import java.util.Optional;

import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TildelBehandlingsoppgaveTest {
    private static final String SAKSBEHANDLER = "Z998877";
    private static final String SAKSNUMMER = "MEL-1234";
    private static final String OPPGAVE_ID = "123123";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private OppgaveService oppgaveService;

    private TildelBehandlingsoppgave tildelBehandlingsoppgave;

    private Prosessinstans prosessinstans;

    @Before
    public void setUp() throws FunksjonellException, TekniskException {
        tildelBehandlingsoppgave = new TildelBehandlingsoppgave(oppgaveService);

        prosessinstans = new Prosessinstans();
        prosessinstans.setSteg(ProsessSteg.JFR_TILDEL_BEHANDLINGSOPPGAVE);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, SAKSBEHANDLER);
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER);

        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId(OPPGAVE_ID);
        Oppgave oppgave = oppgaveBuilder.build();

        when(oppgaveService.finnOppgaveMedFagsaksnummer(eq(SAKSNUMMER))).thenReturn(Optional.of(oppgave));
    }

    @Test
    public void utfør_finnerOppgave_forventTildelingAvOppgave() throws FunksjonellException, TekniskException {
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, true);
        tildelBehandlingsoppgave.utfør(prosessinstans);

        verify(oppgaveService).tildelOppgave(eq(OPPGAVE_ID), eq(SAKSBEHANDLER));
    }
}