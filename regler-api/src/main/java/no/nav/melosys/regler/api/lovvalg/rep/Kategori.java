package no.nav.melosys.regler.api.lovvalg.rep;

import static no.nav.melosys.regler.api.lovvalg.rep.Alvorlighetsgrad.*;

public enum Kategori {
    
    // Generelle feil
    TEKNISK_FEIL(FEIL, "Teknisk feil. Kontakt support."),
    IKKE_STOETTET(FEIL, "Det er ikke implementert maskinell støtte for denne forespørselen."),
    
    // Funksjonelle feil relatert til input
    VALIDERINGSFEIL(FEIL, "Ikke komplett eller inkonsistent input."),
    
    // Varsel
    DELVIS_STOETTET(VARSEL, "Det er kanskje ikke implementert maskinell støtte for denne forespørselen.");
    
    public final Alvorlighetsgrad alvorlighetsgrad;
    public final String melding;

    private Kategori(Alvorlighetsgrad alvorlighetsgrad, String melding) {
        this.alvorlighetsgrad = alvorlighetsgrad;
        this.melding = melding;
    }
 
}
