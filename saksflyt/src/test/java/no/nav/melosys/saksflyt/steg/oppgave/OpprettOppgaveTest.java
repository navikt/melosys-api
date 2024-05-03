package no.nav.melosys.saksflyt.steg.oppgave;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakTestFactory;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void utfoerSteg_nySak_sendForvaltningsmelding() {
        final String journalpostID = "142342343";
        final String saksbehandler = "meg!";

        Fagsak fagsak = FagsakTestFactory.builder().medBruker().build();

        Behandling behandling = new Behandling();
        behandling.setId(243L);
        behandling.setInitierendeJournalpostId(journalpostID);
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, true);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);

        opprettOppgave.utfør(prosessinstans);

        verify(oppgaveService).opprettEllerGjenbrukBehandlingsoppgave(behandling, journalpostID, FagsakTestFactory.BRUKER_AKTØR_ID, saksbehandler, null);
    }

    @Test
    void utfoerSteg_nyOppgave_virksomhet() {
        final String journalpostID = "142342343";
        final String saksbehandler = "meg!";

        Fagsak fagsak = FagsakTestFactory.builder().medVirksomhet().build();

        Behandling behandling = new Behandling();
        behandling.setId(243L);
        behandling.setInitierendeJournalpostId(journalpostID);
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, true);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);

        opprettOppgave.utfør(prosessinstans);

        verify(oppgaveService).opprettEllerGjenbrukBehandlingsoppgave(behandling, journalpostID, null, saksbehandler, FagsakTestFactory.ORGNR);
    }
}
