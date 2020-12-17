package no.nav.melosys.integrasjon.oppgave;

import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;

public interface OppgaveFasade {

    /**
     * Ferdigstiller en opprettet oppgave i Oppgave
     */
    void ferdigstillOppgave(String oppgaveId) throws TekniskException, FunksjonellException;

    /**
     * Finner aktive og utildelte oppgaver som svarer til noen gitt kriterier.
     * Oppgave sorterer oppgavene stigende etter frist.
     */
    List<Oppgave> finnUtildelteOppgaverEtterFrist(Behandlingstema behandlingstema) throws TekniskException, FunksjonellException;

    void oppdaterOppgave(String oppgaveID, OppgaveOppdatering oppgaveOppdatering) throws FunksjonellException, TekniskException;

    /**
     * Finner Oppgaver basert på ansvarlig saksbehandler
     * Oppgave sorterer oppgavene stigende etter frist.
     */
    Set<Oppgave> finnOppgaverMedAnsvarlig(String ansvarligId) throws TekniskException, FunksjonellException;

    /**
     * Finner oppgaver relatert til en bruker.
     * Oppgaver sorteres stigende etter frist.
     */
    List<Oppgave> finnOppgaverMedBrukerID(String aktørID) throws FunksjonellException, TekniskException;

    /**
     * Finner alle oppgaver med gitt saksnummer.
     */
    List<Oppgave> finnOppgaverMedSaksnummer(String saksnummer) throws FunksjonellException, TekniskException;

    /**
     * Hent oppgave fra Oppgave på en gitt oppgaveId
     */
    Oppgave hentOppgave(String oppgaveId) throws TekniskException, FunksjonellException;

    /**
     * Oppretter en oppgave for Melosys i Oppgave og returnerer en unik oppgaveId
     */
    String opprettOppgave(Oppgave oppgave) throws FunksjonellException, TekniskException;

    /**
     * Oppretter en oppgave for NAV Viken i Oppgave og returnerer en unik oppgaveId
     */
    String opprettSensitivOppgave(Oppgave oppgave) throws FunksjonellException, TekniskException;

    /**
     * Legger tilbake en oppgave i Oppgave
     */
    void leggTilbakeOppgave(String oppgaveId) throws FunksjonellException, TekniskException;
}