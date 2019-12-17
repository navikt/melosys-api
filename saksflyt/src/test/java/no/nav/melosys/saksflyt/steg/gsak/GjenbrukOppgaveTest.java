package no.nav.melosys.saksflyt.steg.gsak;

import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GjenbrukOppgaveTest {
    @Mock
    private GsakFasade gsakFasade;
    @Captor
    private ArgumentCaptor<Oppgave> oppgaveCaptor;

    private GjenbrukOppgave gjenbrukOppgave;

    @Before
    public void setUp() {
        gjenbrukOppgave = new GjenbrukOppgave(gsakFasade);
    }

    @Test
    public void gjenbrukOppgave_utfør_oppdatererOppgave() throws FunksjonellException, TekniskException {
        final String oppgaveID = "1234";
        final String saksnummer = "MEL-123";
        when(gsakFasade.hentOppgave(eq(oppgaveID))).thenReturn(lagOppgave(oppgaveID));
        gjenbrukOppgave.utfør(lagProsessinstans(oppgaveID, saksnummer));
        verify(gsakFasade).oppdaterOppgave(oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue()).hasFieldOrPropertyWithValue("saksnummer", saksnummer)
            .hasFieldOrPropertyWithValue("behandlesAvApplikasjon", Fagsystem.MELOSYS)
            .hasFieldOrPropertyWithValue("oppgavetype", Oppgavetyper.BEH_SAK_MK);
    }

    private static Oppgave lagOppgave(String oppgaveID) {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId(oppgaveID);
        return oppgaveBuilder.build();
    }

    private static Prosessinstans lagProsessinstans(String oppgaveID, String saksnummer) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, oppgaveID);
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, saksnummer);
        return prosessinstans;
    }
}