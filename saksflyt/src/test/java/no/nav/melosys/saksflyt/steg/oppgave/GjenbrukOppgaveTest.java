package no.nav.melosys.saksflyt.steg.oppgave;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GjenbrukOppgaveTest {

    @Mock
    private OppgaveService oppgaveService;
    @Captor
    private ArgumentCaptor<Oppgave> oppgaveCaptor;

    private GjenbrukOppgave gjenbrukOppgave;

    @BeforeEach
    public void setUp() {
        gjenbrukOppgave = new GjenbrukOppgave(oppgaveService);
    }

    @Test
    void gjenbrukOppgave_utfør_oppdatererOppgave() throws FunksjonellException, TekniskException {
        final String oppgaveID = "1234";
        final String saksnummer = "MEL-123";
        final String oppgaveBeskrivelse = "jeg beskriver oppgave";

        Oppgave eksisterendeOppgave = new Oppgave.Builder().setBeskrivelse(oppgaveBeskrivelse).build();
        when(oppgaveService.hentOppgaveMedOppgaveID(eq(oppgaveID))).thenReturn(eksisterendeOppgave);

        gjenbrukOppgave.utfør(lagProsessinstans(oppgaveID, saksnummer));
        verify(oppgaveService).opprettOppgave(oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue())
            .hasFieldOrPropertyWithValue("saksnummer", saksnummer)
            .hasFieldOrPropertyWithValue("behandlesAvApplikasjon", Fagsystem.MELOSYS)
            .hasFieldOrPropertyWithValue("oppgavetype", Oppgavetyper.BEH_SAK_MK)
            .hasFieldOrPropertyWithValue("behandlingstema", "ab0424")
            .hasFieldOrPropertyWithValue("behandlingstype", "ae0034")
            .hasFieldOrPropertyWithValue("tilordnetRessurs", "Deg321")
            .hasFieldOrPropertyWithValue("aktørId", "123321");
    }

    private static Prosessinstans lagProsessinstans(String oppgaveID, String saksnummer) {

        Aktoer bruker = new Aktoer();
        bruker.setAktørId("123321");
        bruker.setRolle(Aktoersroller.BRUKER);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(saksnummer);
        fagsak.getAktører().add(bruker);

        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setFagsak(fagsak);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, oppgaveID);
        prosessinstans.setData(ProsessDataKey.SKAL_TILORDNES, true);
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, "Deg321");
        return prosessinstans;
    }
}