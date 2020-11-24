package no.nav.melosys.saksflyt.steg.oppgave;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OpprettOppgaveTest {
    @Mock
    private OppgaveService oppgaveService;

    private OpprettOppgave opprettOppgave;

    @BeforeEach
    public void setUp() {
        opprettOppgave = new OpprettOppgave(oppgaveService);
    }

    @Test
    void utfoerSteg_nySak_sendForvaltningsmelding() throws FunksjonellException, TekniskException {
        final String journalpostID = "142342343";
        final String aktørID = "1242142";
        final String saksbehandler = "meg!";

        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId(aktørID);

        Fagsak fagsak = new Fagsak();
        fagsak.getAktører().add(bruker);

        Behandling behandling = new Behandling();
        behandling.setId(243L);
        behandling.setInitierendeJournalpostId(journalpostID);
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, true);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);

        opprettOppgave.utfør(prosessinstans);

        verify(oppgaveService).opprettEllerGjenbrukBehandlingsoppgave(eq(behandling), eq(journalpostID), eq(aktørID), eq(saksbehandler), eq(null));
    }
}