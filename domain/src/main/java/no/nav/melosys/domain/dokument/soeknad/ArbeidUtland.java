package no.nav.melosys.domain.dokument.soeknad;

import java.math.BigDecimal;

import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;

/**
 * Opplysninger om arbeid i utlandet
 *
 */

public class ArbeidUtland {
    public BigDecimal arbeidsandelNorge;
    public BigDecimal arbeidsandelUtland;
    public boolean arbeidUtlandHjemmekontor;
    public boolean arbeidUtlandErstatning;
    public StrukturertAdresse adresse = new StrukturertAdresse();
}
