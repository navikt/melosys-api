package no.nav.melosys.integrasjon.joark.journalpostapi.dto;

import no.nav.melosys.integrasjon.Konstanter;

public class FerdigstillJournalpostRequest {

    public final String journalfoerendeEnhet;

    public FerdigstillJournalpostRequest() {
        journalfoerendeEnhet = String.valueOf(Konstanter.MELOSYS_ENHET_ID);
    }
}