package no.nav.melosys.integrasjon.gsak;

import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;

public interface GsakFasade {

    /**
     * Ferdigstiller en opprettet oppgave i Oppgave
     */
    void ferdigstillOppgave(String oppgaveId) throws TekniskException, FunksjonellException;

    /**
     * Finner aktive og utildelte oppgaver som svarer til noen gitt kriterier.
     * Oppgave sorterer oppgavene stigende etter frist.
     */
    List<Oppgave> finnUtildelteOppgaverEtterFrist(Set<Oppgavetyper> oppgavetype,
                                                  Set<Behandlingstyper> behandlingstyper,
                                                  Set<Behandlingstema> behandlingstemaer
    ) throws TekniskException, FunksjonellException;

    void oppdaterOppgavePrioritet(String oppgaveId, PrioritetType prioritet) throws FunksjonellException, TekniskException;

    /**
     * Finner Oppgaver basert på ansvarlig saksbehandler
     * Oppgave sorterer oppgavene stigende etter frist.
     */
    List<Oppgave> finnOppgaveListeMedAnsvarlig(String ansvarligId) throws TekniskException, FunksjonellException;

    /**
     * Finner Oppgave med gitt saksnummer.
     */
    Oppgave hentOppgaveMedSaksnummer(String saksnummer) throws TekniskException, FunksjonellException;

    /**
     * Henter tema for aktuell saksnummer
     */
    Tema hentTemaFraSak(Long gsakSaksnummer) throws TekniskException, FunksjonellException;

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
     * Legger tilbake en oppgave i Oppgave
     */
    void leggTilbakeOppgave(String oppgaveId) throws FunksjonellException, TekniskException;

    /**
     * Oppretter en sak i Oppgave
     */
    Long opprettSak(String saksnummer, Behandlingstyper behandlingstype, String aktørId) throws TekniskException, FunksjonellException;

    /**
     * Tildeler en oppgaver til en saksbehandler
     */
    void tildelOppgave(String oppgaveId, String saksbehandlerID) throws FunksjonellException, TekniskException;

    /**
     * Oppdaterer en eksisterende oppgave.
     */
    void oppdaterOppgave(Oppgave oppgave) throws FunksjonellException, TekniskException;
}