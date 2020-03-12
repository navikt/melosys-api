package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
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
    private OppgaveFasade oppgaveFasade;
    @Mock
    private OppgaveService oppgaveService;

    private TildelBehandlingsoppgave tildelBehandlingsoppgave;

    private Prosessinstans prosessinstans;

    @Before
    public void setUp() throws FunksjonellException, TekniskException {
        tildelBehandlingsoppgave = new TildelBehandlingsoppgave(oppgaveFasade, oppgaveService);

        prosessinstans = new Prosessinstans();
        prosessinstans.setSteg(ProsessSteg.JFR_TILDEL_BEHANDLINGSOPPGAVE);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, SAKSBEHANDLER);
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER);

        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId(OPPGAVE_ID);

        when(oppgaveService.hentOppgaveMedFagsaksnummer(eq(SAKSNUMMER))).thenReturn(oppgaveBuilder.build());
    }

    @Test
    public void utførSteg_finnerOppgave_forventTildelingAvOppgave() throws FunksjonellException, TekniskException {
        tildelBehandlingsoppgave.utførSteg(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);
        verify(oppgaveService).hentOppgaveMedFagsaksnummer(eq(SAKSNUMMER));
        verify(oppgaveFasade).tildelOppgave(eq(OPPGAVE_ID), eq(SAKSBEHANDLER));
    }
}