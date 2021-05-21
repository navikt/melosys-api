package no.nav.melosys.domain.dokument;

import no.nav.melosys.domain.SaksopplysningType;
import org.springframework.util.Assert;

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

    private static final String SOB_MAPPE = "sob";

    public static final String TPS_MAPPE = "tps";

    public static final String UTBETAL_MAPPE = "utbetaling";

    /* Tjenester */
    private static final String ARBEIDSFORHOLD_TJENESTE = "arbeidsforhold";

    private static final String INNTEKT_TJENESTE = "inntekt";

    private static final String ORGANISASJON_TJENESTE = "organisasjon";

    private static final String PERSON_TJENESTE = "person";

    private static final String PERSONHISTORIKK_TJENESTE = "personhistorikk";

    private static final String SAKOGBEHANDLING_TJENESTE = "sakogbehandling";

    private static final String UTBETAL = "utbetaldata";

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
        return switch (type) {
            case PERSOPL -> XsltConfig.PERSON_TJENESTE;
            case PERSHIST -> XsltConfig.PERSONHISTORIKK_TJENESTE;
            case ORG -> XsltConfig.ORGANISASJON_TJENESTE;
            case ARBFORH -> XsltConfig.ARBEIDSFORHOLD_TJENESTE;
            case INNTK -> XsltConfig.INNTEKT_TJENESTE;
            case SOB_SAK -> XsltConfig.SAKOGBEHANDLING_TJENESTE;
            case UTBETAL -> XsltConfig.UTBETAL;
            default -> throw new IllegalStateException("SaksopplysningType " + type + " er ikke støttet");
        };
    }

    private static String getXsltMappe(SaksopplysningType type) {
        return switch (type) {
            case PERSOPL, PERSHIST -> XsltConfig.TPS_MAPPE;
            case ORG -> XsltConfig.EREG_MAPPE;
            case ARBFORH -> XsltConfig.AAREG_MAPPE;
            case INNTK -> XsltConfig.INNTK_MAPPE;
            case SOB_SAK -> XsltConfig.SOB_MAPPE;
            case UTBETAL -> XsltConfig.UTBETAL_MAPPE;
            default -> throw new IllegalStateException("SaksopplysningType " + type + " er ikke støttet");
        };
    }
}
