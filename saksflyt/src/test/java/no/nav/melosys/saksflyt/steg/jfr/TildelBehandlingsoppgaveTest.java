package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TildelBehandlingsoppgaveTest {

    private static final String SAKSBEHANDLER = "Z998877";

    private static final String SAKSNUMMER = "MEL-1234";

    private static final String OPPGAVE_ID = "123123";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private GsakFasade gsakFasade;

    @InjectMocks
    private TildelBehandlingsoppgave tildelBehandlingsoppgave;

    private Prosessinstans prosessinstans;

    @Before
    public void setUp() throws FunksjonellException, TekniskException {
        prosessinstans = new Prosessinstans();
        prosessinstans.setSteg(ProsessSteg.JFR_TILDEL_BEHANDLINGSOPPGAVE);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, SAKSBEHANDLER);
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER);

        Oppgave oppgave = new Oppgave();
        oppgave.setOppgaveId(OPPGAVE_ID);

        when(gsakFasade.finnOppgaveMedSaksnummer(eq(SAKSNUMMER))).thenReturn(oppgave);
    }

    @Test
    public void utførSteg_finnerOppgave_forventTildelingAvOppgave() throws FunksjonellException, TekniskException {
        tildelBehandlingsoppgave.utførSteg(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);
        verify(gsakFasade).finnOppgaveMedSaksnummer(eq(SAKSNUMMER));
        verify(gsakFasade).tildelOppgave(eq(OPPGAVE_ID), eq(SAKSBEHANDLER));
    }

    @Test
    public void utførSteg_finnerIngenOppgave_feiler() throws FunksjonellException, TekniskException {
        when(gsakFasade.finnOppgaveMedSaksnummer(anyString())).thenThrow(new TekniskException(""));

        tildelBehandlingsoppgave.utførSteg(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
        verify(gsakFasade).finnOppgaveMedSaksnummer(anyString());
        verify(gsakFasade, never()).tildelOppgave(anyString(), anyString());
    }

    @Test
    public void utfør_finnerIngenOppgave_forventException() throws FunksjonellException, TekniskException {
        when(gsakFasade.finnOppgaveMedSaksnummer(anyString())).thenThrow(new TekniskException(""));

        expectedException.expect(TekniskException.class);
        tildelBehandlingsoppgave.utfør(prosessinstans);

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
        verify(gsakFasade).finnOppgaveMedSaksnummer(anyString());
        verify(gsakFasade, never()).tildelOppgave(anyString(), anyString());
    }
}