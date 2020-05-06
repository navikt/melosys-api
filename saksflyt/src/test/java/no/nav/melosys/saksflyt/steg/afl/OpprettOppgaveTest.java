package no.nav.melosys.saksflyt.steg.afl;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OpprettOppgaveTest {

    private OpprettOppgave opprettOppgave;

    @Mock
    private OppgaveService oppgaveService;

    @Before
    public void setup() {
        opprettOppgave = new OpprettOppgave(oppgaveService);
    }

    @Test
    public void utfør() throws MelosysException {
        final String aktørID = "32313";
        final String journalpostID = "412423";

        Behandling behandling = new Behandling();

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, aktørID);
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostID);

        opprettOppgave.utfør(prosessinstans);

        verify(oppgaveService).opprettEllerGjenbrukBehandlingsoppgave(eq(behandling), eq(journalpostID), eq(aktørID), isNull());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);
    }
}