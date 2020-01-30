package no.nav.melosys.saksflyt.steg.gsak;

import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.gsak.OppgaveOppdatering;
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

@RunWith(MockitoJUnitRunner.class)
public class GjenbrukOppgaveTest {
    @Mock
    private GsakFasade gsakFasade;
    @Captor
    private ArgumentCaptor<OppgaveOppdatering> oppgaveCaptor;

    private GjenbrukOppgave gjenbrukOppgave;

    @Before
    public void setUp() {
        gjenbrukOppgave = new GjenbrukOppgave(gsakFasade);
    }

    @Test
    public void gjenbrukOppgave_utfør_oppdatererOppgave() throws FunksjonellException, TekniskException {
        final String oppgaveID = "1234";
        final String saksnummer = "MEL-123";
        gjenbrukOppgave.utfør(lagProsessinstans(oppgaveID, saksnummer));
        verify(gsakFasade).oppdaterOppgave(eq(oppgaveID), oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue()).hasFieldOrPropertyWithValue("saksnummer", saksnummer)
            .hasFieldOrPropertyWithValue("behandlesAvApplikasjon", Fagsystem.MELOSYS)
            .hasFieldOrPropertyWithValue("oppgavetype", Oppgavetyper.BEH_SAK_MK)
            .hasFieldOrPropertyWithValue("behandlingstype", Behandlingstyper.SOEKNAD_IKKE_YRKESAKTIV)
            .hasFieldOrPropertyWithValue("tilordnetRessurs", "Deg321");
    }

    private static Prosessinstans lagProsessinstans(String oppgaveID, String saksnummer) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, oppgaveID);
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, saksnummer);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD_IKKE_YRKESAKTIV);
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, true);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, "Deg321");
        return prosessinstans;
    }
}