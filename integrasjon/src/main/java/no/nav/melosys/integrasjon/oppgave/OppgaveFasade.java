package no.nav.melosys.integrasjon.oppgave;

import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.oppgave.Oppgave;

public interface OppgaveFasade {

    /**
     * Ferdigstiller en opprettet oppgave i Oppgave
     */
    void ferdigstillOppgave(String oppgaveId);

    /**
     * Finner aktive og utildelte oppgaver som svarer til noen gitt kriterier.
     * Oppgave sorterer oppgavene stigende etter frist.
     */
    List<Oppgave> finnUtildelteOppgaverEtterFrist(String behandlingstype, String behandlingstema);

    void oppdaterOppgave(String oppgaveID, OppgaveOppdatering oppgaveOppdatering);

    /**
     * Finner Oppgaver basert på ansvarlig saksbehandler
     * Oppgave sorterer oppgavene stigende etter frist.
     */
    Set<Oppgave> finnOppgaverMedAnsvarlig(String ansvarligId);

    /**
     * Finner oppgaver relatert til en bruker.
     * Oppgaver sorteres stigende etter frist.
     */
    List<Oppgave> finnOppgaverMedBrukerID(String aktørID);

    /**
     * Finner oppgaver relatert til en virksomhet.
     * Oppgaver sorteres stigende etter frist.
     */
    List<Oppgave> finnOppgaverMedOrgnr(String orgnr);

    /**
     * Finner alle åpne oppgaver med gitt saksnummer.
     */
    List<Oppgave> finnÅpneOppgaverMedSaksnummer(String saksnummer);

    /**
     * Finner alle oppgaver med gitt saksnummer.
     */
    List<Oppgave> finnAvsluttetOppgaverMedSaksnummer(String saksnummer);

    /**
     * Hent oppgave fra Oppgave på en gitt oppgaveId
     */
    Oppgave hentOppgave(String oppgaveId);

    /**
     * Oppretter en oppgave for Melosys i Oppgave og returnerer en unik oppgaveId
     */
    String opprettOppgave(Oppgave oppgave);

    /**
     * Oppretter en oppgave for NAV Viken i Oppgave og returnerer en unik oppgaveId
     */
    String opprettSensitivOppgave(Oppgave oppgave);

    /**
     * Legger tilbake en oppgave i Oppgave
     */
    void leggTilbakeOppgave(String oppgaveId);
}
