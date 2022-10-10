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
    /**
     * VEDLEGG er en vedleggskomponent som tillater saksbehandler å laste opp vedlegg som sendes med brevet
     */
    VEDLEGG,
    /**
     * FRITEKSTVEDLEGG er en vedleggskomponent som tillater saksbehandler å lage et fritekstdokument som
     * journalføres og sendes sammen med hovedbrevet
     */
    FRITEKSTVEDLEGG,
}
