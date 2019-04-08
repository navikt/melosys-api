package no.nav.melosys.integrasjon.gsak;

import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.oppgave.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.*;

public interface GsakFasade {

    /**
     * Ferdigstiller en opprettet oppgave i GSAK
     */
    void ferdigstillOppgave(String oppgaveId) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException, FunksjonellException;

    /**
     * Finner aktive og utildelte oppgaver som svarer til noen gitt kriterier.
     * GSAK sorterer oppgavene stigende etter frist.
     */
    List<Oppgave> finnUtildelteOppgaverEtterFrist(Oppgavetyper oppgavetype,
                                                  Tema tema,
                                                  List<Sakstyper> sakstyper,
                                                  List<Behandlingstyper> behandlingstyper,
                                                  List<Behandlingstema> behandlingstemaer
    ) throws TekniskException, FunksjonellException;

    /**
     * Finner Oppgaver basert på ansvarlig saksbehandler
     * GSAK sorterer oppgavene stigende etter frist.
     */
    List<Oppgave> finnOppgaveListeMedAnsvarlig(String ansvarligId) throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException, FunksjonellException;

    /**
     * Finner Oppgaver basert på bruker.
     * GSAK sorterer oppgavene stigende etter frist.
     */
    List<Oppgave> finnOppgaveListeMedBruker(String aktørId) throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException, FunksjonellException;

    /**
     * Finner Oppgave med gitt saksnummer.
     */
    Optional<Oppgave> finnOppgaveMedSaksnummer(String saksnummer) throws TekniskException, FunksjonellException;

    /**
     * Finner Behandlingsoppgaver basert på bruker.
     * GSAK sorterer oppgavene stigende etter frist.
     */
    List<Oppgave> finnBehandlingsoppgaverMedBruker(String aktørId) throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException, FunksjonellException;

    /**
     * Hent oppgave fra GSAK på en gitt oppgaveId
     */
    Oppgave hentOppgave(String oppgaveId) throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException, FunksjonellException;

    /**
     * Oppretter en oppgave i GSAK for å få en unik oppgaveId
     */
    String opprettOppgave(Oppgave request) throws SikkerhetsbegrensningException, FunksjonellException, TekniskException;

    /**
     * Legger tilbake en oppgave i GSAK
     */
    void leggTilbakeOppgave(String oppgaveId) throws SikkerhetsbegrensningException, FunksjonellException, TekniskException, IkkeFunnetException;

    /**
     * Oppretter en sak i GSAK
     */
    Long opprettSak(String saksnummer, Behandlingstyper behandlingstype, String aktørId) throws TekniskException, IntegrasjonException, SikkerhetsbegrensningException, FunksjonellException;

    /**
     * Tildeler en oppgaver til en saksbehandler
     */
    void tildelOppgave(String oppgaveId, String saksbehandlerID) throws IkkeFunnetException, SikkerhetsbegrensningException, FunksjonellException, TekniskException;

}