package no.nav.melosys.tjenester.gui.dto.brev;

public enum FeltType {
    /**
     *  TEKST tillater ikke linjeskift, og vises som et vanlig inputfelt
     */
    TEKST,

    /**
     * FRITEKST er et felttype som tillatter linjeskift, styling etc.
     */
    FRITEKST,

    /**
     * SJEKKBOKS er en checkbox som gir verdiene true/false.
     */
    SJEKKBOKS,
}
