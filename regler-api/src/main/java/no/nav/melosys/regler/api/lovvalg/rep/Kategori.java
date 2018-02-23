package no.nav.melosys.regler.api.lovvalg.rep;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.annotation.JsonFormat;
import no.nav.melosys.regler.api.lovvalg.rep.adapter.KategoriAdapter;

import static no.nav.melosys.regler.api.lovvalg.rep.Alvorlighetsgrad.FEIL;
import static no.nav.melosys.regler.api.lovvalg.rep.Alvorlighetsgrad.VARSEL;

@XmlJavaTypeAdapter(KategoriAdapter.class)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum Kategori {
    
    // Generelle feil
    TEKNISK_FEIL(FEIL, "Teknisk feil. Kontakt support."),
    IKKE_STOETTET(FEIL, "Det er ikke implementert maskinell støtte for denne forespørselen."),
    
    // Funksjonelle feil relatert til input
    VALIDERINGSFEIL(FEIL, "Ikke komplett eller inkonsistent input."),
    
    // Varsel
    DELVIS_STOETTET(VARSEL, "Det er implementert delvis maskinell støtte for denne forespørselen."); // FIXME: Teit navn og tekst
    
    public final Alvorlighetsgrad alvorlighetsgrad;
    public final String beskrivelse;

    private Kategori(Alvorlighetsgrad alvorlighetsgrad, String melding) {
        this.alvorlighetsgrad = alvorlighetsgrad;
        this.beskrivelse = melding;
    }
 
}
