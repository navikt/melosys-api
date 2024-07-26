package no.nav.melosys.integrasjon.oppgave;

import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.oppgave.Oppgave;

public interface OppgaveFasade {

    void ferdigstillOppgave(String oppgaveId);

    List<Oppgave> finnUtildelteOppgaverEtterFrist(String behandlingstema);

    void oppdaterOppgave(String oppgaveID, OppgaveOppdatering oppgaveOppdatering);

    Set<Oppgave> finnOppgaverMedAnsvarlig(String ansvarligSaksbehandlerID);

    List<Oppgave> finnOppgaverMedAktørId(String aktørID, String[] oppgavetyper);

    List<Oppgave> finnOppgaverMedOrgnr(String orgnr, String[] oppgavetyper);

    List<Oppgave> finnÅpneBehandlingsoppgaverMedSaksnummer(String saksnummer);

    List<Oppgave> finnÅpneBehandlingsoppgaver();

    List<Oppgave> finnAvsluttetBehandlingsoppgaverMedSaksnummer(String saksnummer);

    Oppgave hentOppgave(String oppgaveId);

    String opprettOppgave(Oppgave oppgave);

    String opprettSensitivOppgave(Oppgave oppgave);

    void leggTilbakeOppgave(String oppgaveId);
}
