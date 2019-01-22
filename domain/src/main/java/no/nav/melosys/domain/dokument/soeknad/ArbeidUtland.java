package no.nav.melosys.domain.dokument.soeknad;

import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;

/**
 * Opplysninger om arbeid i utlandet
 *
 */

public class ArbeidUtland {
    public String foretakNavn;
    public String foretakOrgnr;
    public boolean arbeidUtlandHjemmekontor;
    public StrukturertAdresse adresse = new StrukturertAdresse();
}
