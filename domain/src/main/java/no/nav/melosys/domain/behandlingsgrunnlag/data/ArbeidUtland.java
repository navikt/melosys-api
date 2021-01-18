package no.nav.melosys.domain.behandlingsgrunnlag.data;

import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;

/**
 * Opplysninger om arbeid i utlandet
 *
 */

public class ArbeidUtland {
    public String foretakNavn;
    public String foretakOrgnr;
    public Boolean arbeidUtlandHjemmekontor;
    public StrukturertAdresse adresse = new StrukturertAdresse();
}
