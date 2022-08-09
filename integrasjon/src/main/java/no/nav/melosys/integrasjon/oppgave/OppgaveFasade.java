package no.nav.melosys.integrasjon.oppgave;

import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.oppgave.Oppgave;

public interface OppgaveFasade {

    void feilregistrerOppgaver(Set<String> oppgaveIdSet);

    void ferdigstillOppgave(String oppgaveId);

    List<Oppgave> finnUtildelteOppgaverEtterFrist(String behandlingstype, String behandlingstema);

    void oppdaterOppgave(String oppgaveID, OppgaveOppdatering oppgaveOppdatering);

    Set<Oppgave> finnOppgaverMedAnsvarlig(String ansvarligSaksbehandlerID);

    List<Oppgave> finnOppgaverMedAktørId(String aktørID);

    List<Oppgave> finnOppgaverMedOrgnr(String orgnr);

    List<Oppgave> finnÅpneOppgaverMedJournalpostID(String journalpostID);

    List<Oppgave> finnÅpneOppgaverMedSaksnummer(String saksnummer);

    List<Oppgave> finnAvsluttetOppgaverMedSaksnummer(String saksnummer);

    Oppgave hentOppgave(String oppgaveId);

    String opprettOppgave(Oppgave oppgave);

    String opprettSensitivOppgave(Oppgave oppgave);

    void leggTilbakeOppgave(String oppgaveId);
}
