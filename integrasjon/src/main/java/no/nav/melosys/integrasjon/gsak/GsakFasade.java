package no.nav.melosys.integrasjon.gsak;

import java.util.List;

import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.Oppgavetype;
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
    List<Oppgave> finnUtildelteOppgaverEtterFrist(Oppgavetype oppgavetype,
                                                  Tema tema,
                                                  List<FagsakType> sakstyper,
                                                  List<BehandlingType> behandlingstyper
    ) throws TekniskException;

    /**
     * Finner Oppgaver basert på ansvarlig saksbehandler
     * GSAK sorterer oppgavene stigende etter frist.
     */
    List<Oppgave> finnOppgaveListeMedAnsvarlig(String ansvarligId) throws TekniskException;

    /**
     * Finner Oppgaver basert på bruker.
     * GSAK sorterer oppgavene stigende etter frist.
     */
    List<Oppgave> finnOppgaveListeMedBruker(String aktørId) throws TekniskException;

    /**
     * Hent oppgave fra GSAK på en gitt oppgaveId
     */
    Oppgave hentOppgave(String oppgaveId) throws IkkeFunnetException, TekniskException;

    /**
     * Oppretter en oppgave i GSAK for å få en unik oppgaveId
     */
    String opprettOppgave(Oppgave request) throws SikkerhetsbegrensningException, FunksjonellException;
    /**
     * Legger tilbake en oppgave i GSAK
     */
    void leggTilbakeOppgave(String oppgaveId) throws SikkerhetsbegrensningException, FunksjonellException, TekniskException, IkkeFunnetException;

    /**
     * Oppretter en sak i GSAK
     */
    String opprettSak(String saksnummer, BehandlingType behandlingType, String aktørId) throws TekniskException, IntegrasjonException;

    /**
     * Tildeler en oppgaver til en saksbehandler
     */
    void tildelOppgave(String oppgaveId, String saksbehandlerID) throws IkkeFunnetException, SikkerhetsbegrensningException, FunksjonellException, TekniskException;

}