package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

import java.time.LocalDateTime;

public record RelevantDato(LocalDateTime dato, Datotype datotype) {
    public boolean erDatotypeJournalført() {
        return Datotype.DATO_JOURNALFOERT == datotype;
    }

    public boolean erDatotypeRegistrert() {
        return Datotype.DATO_REGISTRERT == datotype;
    }
}
