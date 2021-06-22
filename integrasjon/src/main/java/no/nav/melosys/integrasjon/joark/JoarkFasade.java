package no.nav.melosys.integrasjon.joark;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;

public interface JoarkFasade {

    /**
     * Ferdigstiller journalføring
     */
    void ferdigstillJournalføring(String journalpostId);

    /**
     * Henter et dokument fra Joark
     */
    byte[] hentDokument(String journalPostID, String dokumentID);

    /**
     * Henter en journalpost fra Joark
     */
    Journalpost hentJournalpost(String journalpostID);

    /**
     * Henter en liste med journalposter knyttet til en sak.
     */
    List<Journalpost> hentJournalposterTilknyttetSak(HentJournalposterTilknyttetSakRequest hentJournalposterTilknyttetSakRequest);

    /**
     * Oppretter en journalpost  i Joark
     */
    String opprettJournalpost(OpprettJournalpost opprettJournalpost, boolean forsøkEndeligJfr);

    /**
     * Oppdaterer en journalpost og forsøker å ferdigstille hvis forsøkFerdigstill er satt
     */
    void oppdaterJournalpost(String journalpostID, JournalpostOppdatering journalpostOppdatering, boolean forsøkFerdigstill)
    ;

    /**
     * Valider at dokumenter tilhører sak og at tilgang er gitt
     */
    void validerDokumenterTilhørerSakOgHarTilgang(HentJournalposterTilknyttetSakRequest hentJournalposterTilknyttetSakRequest,
                                                  Collection<DokumentReferanse> dokumentReferanser);

    LocalDate hentMottaksDatoForJournalpost(String journalpostID);
}
