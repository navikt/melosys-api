package no.nav.melosys.integrasjon.gsak;

import java.util.List;

import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.felles.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave.OpprettOppgaveRequest;

public interface GsakFasade {

    /**
     * Ferdigstiller en opprettet oppgave i GSAK
     *
     * @param oppgaveId                 GSAK-oppgave som skal ferdigstilles
     * @throws                          SikkerhetsbegrensningException
     * @throws                          TekniskException
     */
    void ferdigstillOppgave(String oppgaveId) throws SikkerhetsbegrensningException, TekniskException;

    /**
     * Finner aktive og utildelte oppgaver som svarer til noen gitt kriterier.
     * GSAK sorterer oppgavene stigende etter frist.
     *
     * @param fagområdeKodeListe
     * @param underkategori
     * @param oppgavetypeKodeListe
     * @return
     * @throws IntegrasjonException
     */
    List<Oppgave> finnUtildelteOppgaverEtterFrist(List<String> fagområdeKodeListe,
                                                  String underkategori,
                                                  List<String> oppgavetypeKodeListe
    ) throws IntegrasjonException;

    /**
     * Oppretter en oppgave i GSAK for å få en unik oppgaveId
     *
     * @param request                   Intern representasjon av oppgaven som skal opprettes i GSAK
     * @return                          GSAK oppgaveId til den opprettede oppgaven
     * @throws                          SikkerhetsbegrensningException
     */
    String opprettOppgave(OpprettOppgaveRequest request) throws SikkerhetsbegrensningException;

    /**
     * Oppretter en sak i GSAK
     *
     * @param fagsakId                  fagsakId fra Melosys
     * @param fnr                       Fødselsnummer
     * @return                          Saksnummer fra GSAK
     * @throws                          IntegrasjonException
     */
    String opprettSak(Long fagsakId, String fnr) throws IntegrasjonException;

    /**
     * Tildeler en oppgaver til en saksbehandler
     * @param oppgaveId
     * @param saksbehandlerID
     */
    void tildelOppgave(String oppgaveId, String saksbehandlerID);
}
