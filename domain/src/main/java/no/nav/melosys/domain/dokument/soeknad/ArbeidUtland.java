package no.nav.melosys.domain.dokument.soeknad;

import java.math.BigDecimal;

/**
 * Opplysninger om arbeid i utlandet
 *
 */

public class ArbeidUtland {
    public BigDecimal arbeidsandelNorge;
    public BigDecimal arbeidsandelUtland;
    public boolean arbeidUtlandHjemmekontor;
    public boolean arbeidUtlandErstatning;
    public StandardAdresse adresse;
}
