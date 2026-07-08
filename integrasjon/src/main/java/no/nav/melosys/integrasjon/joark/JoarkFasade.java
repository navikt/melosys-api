package no.nav.melosys.integrasjon.joark;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.KnyttTilAnnenSakRequest;
import org.springframework.retry.annotation.Retryable;

@Retryable
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
     * Oppdaterer en journalpost og forsøker å ferdigstille
     */
    void oppdaterOgFerdigstillJournalpost(String journalpostID, JournalpostOppdatering journalpostOppdatering);

    /**
     * Oppdaterer journalposter knyttet til en sak fra gammel aktørId til ny aktørId.
     */
    void oppdaterJournalposterMedNyAktørId(HentJournalposterTilknyttetSakRequest hentJournalposterTilknyttetSakRequest,
                                           String gammelAktørId,
                                           String nyAktørId);
    /**
     * Feilregistrerer sakstilknytning på journalpost. Skal brukes når en journalpost feilaktig har blitt knyttet til en sak.
     */
    void feilregistrerSakstilknytning(String journalpostId);

    /**
     * Knytter dokumentene på en kildejournalpost til en annen sak. Joark utfører selv tilgangssjekk,
     * validering og kopiering, og returnerer ID-en til den nye journalposten.
     */
    String knyttTilAnnenSak(String kildeJournalpostId, KnyttTilAnnenSakRequest request);

    void validerDokumenterTilhørerSakOgHarTilgang(HentJournalposterTilknyttetSakRequest hentJournalposterTilknyttetSakRequest,
                                                   Collection<DokumentReferanse> dokumentReferanser);

    LocalDate hentMottaksDatoForJournalpost(String journalpostID);
}
