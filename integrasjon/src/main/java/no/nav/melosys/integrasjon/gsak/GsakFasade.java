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
     *
     * @param oppgaveId GSAK-oppgave som skal ferdigstilles
     */
    void ferdigstillOppgave(String oppgaveId) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException, FunksjonellException;

    /**
     * Finner aktive og utildelte oppgaver som svarer til noen gitt kriterier.
     * GSAK sorterer oppgavene stigende etter frist.
     *
     * @param oppgavetype
     * @param tema
     * @param sakstyper
     * @param behandlingstyper
     * @return List<Oppgave>
     */
    List<Oppgave> finnUtildelteOppgaverEtterFrist(Oppgavetype oppgavetype,
                                                  Tema tema,
                                                  List<FagsakType> sakstyper,
                                                  List<BehandlingType> behandlingstyper
    ) throws TekniskException;

    /**
     * Finner Oppgaver basert på ansvarlig saksbehandler
     * GSAK sorterer oppgavene stigende etter frist.
     *
     * @param ansvarligId
     * @return List<Oppgave>
     */
    List<Oppgave> finnOppgaveListeMedAnsvarlig(String ansvarligId) throws TekniskException;

    /**
     * Finner Oppgaver basert på bruker.
     * GSAK sorterer oppgavene stigende etter frist.
     *
     * @param aktørId
     * @return List<Oppgave>
     */
    List<Oppgave> finnOppgaveListeMedBruker(String aktørId) throws TekniskException;

    /**
     * Hent oppgave fra GSAK på en gitt oppgaveId
     *
     * @param oppgaveId
     * @return oppgave informasjon
     */
    Oppgave hentOppgave(String oppgaveId) throws IkkeFunnetException, TekniskException;

    /**
     * Oppretter en oppgave i GSAK for å få en unik oppgaveId
     *
     * @param request Intern representasjon av oppgaven som skal opprettes i GSAK
     * @return OppgaveId
     */
    String opprettOppgave(Oppgave request) throws SikkerhetsbegrensningException, FunksjonellException;
    /**
     * Legger tilbake en oppgave i GSAK
     *
     * @param oppgaveId
     */
    void leggTilbakeOppgave(String oppgaveId) throws SikkerhetsbegrensningException, FunksjonellException, TekniskException, IkkeFunnetException;

    /**
     * Oppretter en sak i GSAK
     *
     * @param saksnummer fra Melosys
     * @param behandlingType brukes til å avgjøre tema
     * @param aktørId AktørId
     * @return Saksnummer fra GSAK
     */
    //Fixme: Anbefaler å bruke TekniskException isteden av IntegrasjonException
    String opprettSak(String saksnummer, BehandlingType behandlingType, String aktørId) throws TekniskException, IntegrasjonException;

    /**
     * Tildeler en oppgaver til en saksbehandler
     *
     * @param oppgaveId
     * @param saksbehandlerID
     */
    void tildelOppgave(String oppgaveId, String saksbehandlerID) throws IkkeFunnetException, SikkerhetsbegrensningException, FunksjonellException, TekniskException;

    // FIXME For å teste jfr
    String opprettOppgave(String ansvarligID, String oppgavetype, String brukerID, String journalpostID, String saksnummer);
}