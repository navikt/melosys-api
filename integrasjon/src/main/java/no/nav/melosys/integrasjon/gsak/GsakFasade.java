package no.nav.melosys.integrasjon.gsak;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.oppgave.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;

public interface GsakFasade {

    /**
     * Ferdigstiller en opprettet oppgave i GSAK
     */
    void ferdigstillOppgave(String oppgaveId) throws TekniskException, FunksjonellException;

    /**
     * Finner aktive og utildelte oppgaver som svarer til noen gitt kriterier.
     * GSAK sorterer oppgavene stigende etter frist.
     */
    List<Oppgave> finnUtildelteOppgaverEtterFrist(Set<Oppgavetyper> oppgavetype,
                                                  Set<Sakstyper> sakstyper,
                                                  Set<Behandlingstyper> behandlingstyper,
                                                  Set<Behandlingstema> behandlingstemaer
    ) throws TekniskException, FunksjonellException;

    /**
     * Finner Oppgaver basert på ansvarlig saksbehandler
     * GSAK sorterer oppgavene stigende etter frist.
     */
    List<Oppgave> finnOppgaveListeMedAnsvarlig(String ansvarligId) throws TekniskException, FunksjonellException;

    /**
     * Finner Oppgave med gitt saksnummer.
     */
    Optional<Oppgave> finnOppgaveMedSaksnummer(String saksnummer) throws TekniskException, FunksjonellException;

    /**
     * Finner Behandlingsoppgaver basert på bruker.
     * GSAK sorterer oppgavene stigende etter frist.
     */
    List<Oppgave> finnBehandlingsoppgaverMedBruker(String aktørId) throws TekniskException, FunksjonellException;

    /**
     * Hent oppgave fra GSAK på en gitt oppgaveId
     */
    Oppgave hentOppgave(String oppgaveId) throws TekniskException, FunksjonellException;

    /**
     * Oppretter en oppgave i GSAK for å få en unik oppgaveId
     */
    String opprettOppgave(Oppgave request) throws FunksjonellException, TekniskException;

    /**
     * Legger tilbake en oppgave i GSAK
     */
    void leggTilbakeOppgave(String oppgaveId) throws FunksjonellException, TekniskException;

    /**
     * Oppretter en sak i GSAK
     */
    Long opprettSak(String saksnummer, Behandlingstyper behandlingstype, String aktørId) throws TekniskException, FunksjonellException;

    /**
     * Tildeler en oppgaver til en saksbehandler
     */
    void tildelOppgave(String oppgaveId, String saksbehandlerID) throws FunksjonellException, TekniskException;

}