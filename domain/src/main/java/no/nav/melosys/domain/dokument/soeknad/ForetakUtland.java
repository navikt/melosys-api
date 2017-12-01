package no.nav.melosys.domain.dokument.soeknad;

import no.nav.melosys.domain.dokument.organisasjon.adresse.Gateadresse;

/**
 * Opplysninger om foretak i utlandet
 */
public class ForetakUtland {
    public String foretakUtlandNavn;
    public String foretakUtlandOrgnr;
    public Gateadresse foretakUtlandAdresse; // TODO kan Gateadresse brukes?
}
