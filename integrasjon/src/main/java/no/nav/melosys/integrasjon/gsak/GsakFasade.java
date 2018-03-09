package no.nav.melosys.integrasjon.gsak;

import java.util.List;

import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.gsak.dto.OppgaveDTO;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.felles.exception.TekniskException;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSLagreOppgaveRequest;
import no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave.OpprettOppgaveRequest;

public interface GsakFasade {

    /**
     * Oppretter en sak i GSAK for å få et unikt saksnummer.
     *
     * @param fagsakId fagsakId fra Melosys
     * @param fnr      Fødselsnummer
     * @return Saksnummer fra GSAK
     * @throws IntegrasjonException
     */
    String opprettSak(Long fagsakId, String fnr) throws IntegrasjonException;

    /**
     * Hent OppgaveListe
     *
     * @param ansvarligEnhetId        fagsakId fra Melosys
     * @param brukerID                Fødselsnummer
     * @param sorteringselementKode   OPPRETTET_DATO ellers FRIST_DATO
     * @param sorteringKode           STIGENDE ellers SYNKENDE
     * @param ikkeTidligereFordeltTil Saksbehandlerident
     * @return ArrayList av OppgaveDTO som har oppgaveID
     * @throws IntegrasjonException
     */
    List<OppgaveDTO> finnOppgaveListe(String ansvarligEnhetId,
                                      String brukerID,
                                      String sorteringselementKode,
                                      String sorteringKode, //STIGENDE ellers SYNKENDE
                                      String ikkeTidligereFordeltTil
    ) throws IntegrasjonException;

    // FIXME: Ikke eksponer eksterne avhengigheter
    void lagreOppgave(WSLagreOppgaveRequest request) throws IntegrasjonException, SikkerhetsbegrensningException, TekniskException;

    void ferdigstillOppgave(String oppgaveId) throws SikkerhetsbegrensningException, TekniskException;

    String opprettOppgave(OpprettOppgaveRequest request) throws SikkerhetsbegrensningException;
}
