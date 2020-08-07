package no.nav.melosys.saksflyt.steg.msa;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OpprettOppgaveTest {

    @Mock
    private OppgaveService oppgaveService;

    private OpprettOppgave opprettOppgave;

    private final String aktørID = "123132123";
    private final Behandling behandling = new Behandling();
    private final Prosessinstans prosessinstans = new Prosessinstans();

    @Before
    public void setup() {
        opprettOppgave = new OpprettOppgave(oppgaveService);

        behandling.setId(123L);
        behandling.setInitierendeDokumentId("jpid123");

        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId(aktørID);
        Fagsak fagsak = new Fagsak();
        fagsak.getAktører().add(bruker);
        behandling.setFagsak(fagsak);

        prosessinstans.setBehandling(behandling);
    }

    @Test
    public void utfør() throws MelosysException {
        opprettOppgave.utfør(prosessinstans);

        verify(oppgaveService).opprettEllerGjenbrukBehandlingsoppgave(
            eq(behandling),
            eq(behandling.getInitierendeJournalpostId()),
            eq(aktørID),
            isNull()
        );
    }
}