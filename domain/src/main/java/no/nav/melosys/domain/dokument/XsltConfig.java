package no.nav.melosys.domain.dokument;

import org.springframework.util.Assert;

import no.nav.melosys.domain.SaksopplysningType;

/**
 * XsltConfig inneholder informasjon om filstrukturen til xslt-filene som brukes til å konvertere eksterne
 * opplysninger fra registre til en intern dokumentmodell.
 */
public final class XsltConfig {

    private XsltConfig() {
        throw new IllegalStateException("Utility class");
    }

    /* Mapper */
    public static final String AAREG_MAPPE = "aareg";

    public static final String EREG_MAPPE = "ereg";

    public static final String INNTK_MAPPE= "inntk";

    public static final String MEDL_MAPPE = "medl";

    private static final String SOB_MAPPE = "sob";

    public static final String SØKNAD_MAPPE = "soeknad";

    public static final String TPS_MAPPE = "tps";

    /* Tjenester */
    private static final String ARBEIDSFORHOLD_TJENESTE = "arbeidsforhold";

    private static final String INNTEKT_TJENESTE = "inntekt";

    private static final String MEDLEMSKAP_TJENESTE = "medlemskap";

    private static final String ORGANISASJON_TJENESTE = "organisasjon";

    private static final String PERSON_TJENESTE = "person";

    private static final String PERSONHISTORIKK_TJENESTE = "personhistorikk";

    private static final String SAKOGBEHANDLING_TJENESTE = "sakogbehandling";

    private static final String SØKNAD = "soeknad";

    /**
     * Returnerer en sti til xslt filen som brukes for å konvertere mot det felles domene.
     *
     * @param type Hvilken type saksopplysning brukes
     * @param versjon Versjonen av tjenesten som returnerer den xml som konverteres
     * @return en String som inneholder en sti til filen i resources
     */
    static String getXsltPath(SaksopplysningType type, String versjon) {
        Assert.notNull(type, "type må ikke være null");
        Assert.notNull(versjon, "versjon må ikke være null");

        return getXsltMappe(type) + "/" + getTjenesteNavn(type) + "_" + versjon + ".xslt";
    }

    private static String getTjenesteNavn(SaksopplysningType type) {
        switch (type){
            case PERSONOPPLYSNING: return XsltConfig.PERSON_TJENESTE;
            case PERSONHISTORIKK: return XsltConfig.PERSONHISTORIKK_TJENESTE;
            case ORGANISASJON: return XsltConfig.ORGANISASJON_TJENESTE;
            case ARBEIDSFORHOLD: return XsltConfig.ARBEIDSFORHOLD_TJENESTE;
            case INNTEKT: return XsltConfig.INNTEKT_TJENESTE;
            case MEDLEMSKAP: return XsltConfig.MEDLEMSKAP_TJENESTE;
            case SOB_SAK: return XsltConfig.SAKOGBEHANDLING_TJENESTE;
            case SØKNAD: return XsltConfig.SØKNAD;
            default: throw new IllegalStateException("SaksopplysningType " + type + " er ikke støttet");
        }
    }

    private static String getXsltMappe(SaksopplysningType type) {
        switch (type){
            case PERSONOPPLYSNING: case PERSONHISTORIKK: return XsltConfig.TPS_MAPPE;
            case ORGANISASJON: return XsltConfig.EREG_MAPPE;
            case ARBEIDSFORHOLD: return XsltConfig.AAREG_MAPPE;
            case INNTEKT: return XsltConfig.INNTK_MAPPE;
            case MEDLEMSKAP: return XsltConfig.MEDL_MAPPE;
            case SOB_SAK: return XsltConfig.SOB_MAPPE;
            case SØKNAD: return XsltConfig.SØKNAD_MAPPE;
            default: throw new IllegalStateException("SaksopplysningType " + type + " er ikke støttet");
        }
    }
}
