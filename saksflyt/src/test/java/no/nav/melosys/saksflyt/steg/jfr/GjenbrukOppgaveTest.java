package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.oppgave.OppgaveService;
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
    private OppgaveService oppgaveService;
    @Captor
    private ArgumentCaptor<Oppgave> oppgaveCaptor;

    private GjenbrukOppgave gjenbrukOppgave;

    @Before
    public void setUp() {
        gjenbrukOppgave = new GjenbrukOppgave(oppgaveService);
    }

    @Test
    public void gjenbrukOppgave_utfør_oppdatererOppgave() throws FunksjonellException, TekniskException {
        final String oppgaveID = "1234";
        final String saksnummer = "MEL-123";
        final String oppgaveBeskrivelse = "jeg beskriver oppgave";

        Oppgave eksisterendeOppgave = new Oppgave.Builder().setBeskrivelse(oppgaveBeskrivelse).build();
        when(oppgaveService.hentOppgaveMedOppgaveID(eq(oppgaveID))).thenReturn(eksisterendeOppgave);

        gjenbrukOppgave.utfør(lagProsessinstans(oppgaveID, saksnummer));
        verify(oppgaveService).opprettOppgave(oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue()).hasFieldOrPropertyWithValue("saksnummer", saksnummer)
            .hasFieldOrPropertyWithValue("behandlesAvApplikasjon", Fagsystem.MELOSYS)
            .hasFieldOrPropertyWithValue("oppgavetype", Oppgavetyper.BEH_SAK_MK)
            .hasFieldOrPropertyWithValue("behandlingstype", Behandlingstyper.SOEKNAD_IKKE_YRKESAKTIV)
            .hasFieldOrPropertyWithValue("tilordnetRessurs", "Deg321")
            .hasFieldOrPropertyWithValue("aktørId", "123321");
    }

    private static Prosessinstans lagProsessinstans(String oppgaveID, String saksnummer) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, oppgaveID);
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, saksnummer);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD_IKKE_YRKESAKTIV);
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, true);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, "Deg321");
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "123321");
        return prosessinstans;
    }
}