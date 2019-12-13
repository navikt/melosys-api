package no.nav.melosys.integrasjon.joark.journalpostapi.dto;

import java.time.LocalDate;

public class OppdaterJournalpostRequest {

    private final LocalDate datoMottat;

    public OppdaterJournalpostRequest(LocalDate datoMottat) {
        this.datoMottat = datoMottat;
    }

    public LocalDate getDatoMottat() {
        return datoMottat;
    }
}
