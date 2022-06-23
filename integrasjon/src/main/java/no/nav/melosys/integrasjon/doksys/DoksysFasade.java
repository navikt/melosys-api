package no.nav.melosys.integrasjon.doksys;

import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.arkiv.Distribusjonstype;

public interface DoksysFasade {

    byte[] produserDokumentutkast(Dokumentbestilling dokumentbestilling);

    DokumentbestillingResponse produserIkkeredigerbartDokument(Dokumentbestilling dokumentbestilling);

    String distribuerJournalpost(String journalpostId, StrukturertAdresse mottakeradresse, Distribusjonstype distribusjonstype);

    String distribuerJournalpost(String journalpostId, StrukturertAdresse mottakeradresse, Kontaktopplysning kontaktopplysning, String kontaktpersonNavn, Distribusjonstype distribusjonstype);

    String distribuerJournalpost(String journalpostId, Distribusjonstype distribusjonstype);
}
