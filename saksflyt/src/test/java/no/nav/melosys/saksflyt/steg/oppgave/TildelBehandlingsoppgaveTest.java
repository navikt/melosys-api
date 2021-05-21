package no.nav.melosys.saksflyt.steg.oppgave;

import java.util.Optional;

import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TildelBehandlingsoppgaveTest {
    private static final String SAKSBEHANDLER = "Z998877";
    private static final String SAKSNUMMER = "MEL-1234";
    private static final String OPPGAVE_ID = "123123";

    @Mock
    private OppgaveService oppgaveService;

    private TildelBehandlingsoppgave tildelBehandlingsoppgave;

    private Prosessinstans prosessinstans;

    @BeforeEach
    public void setUp() {
        tildelBehandlingsoppgave = new TildelBehandlingsoppgave(oppgaveService);

        prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, SAKSBEHANDLER);
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER);

        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId(OPPGAVE_ID);
        Oppgave oppgave = oppgaveBuilder.build();

        when(oppgaveService.finnOppgaveMedFagsaksnummer(SAKSNUMMER)).thenReturn(Optional.of(oppgave));
    }

    @Test
    void utfør_finnerOppgave_forventTildelingAvOppgave() {
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, true);
        tildelBehandlingsoppgave.utfør(prosessinstans);

        verify(oppgaveService).tildelOppgave(OPPGAVE_ID, SAKSBEHANDLER);
    }
}
