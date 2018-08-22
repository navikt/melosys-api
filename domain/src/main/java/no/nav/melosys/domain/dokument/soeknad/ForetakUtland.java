package no.nav.melosys.domain.dokument.soeknad;

import no.nav.melosys.domain.dokument.organisasjon.adresse.Gateadresse;

/**
 * Opplysninger om foretak i utlandet
 */
public class ForetakUtland {
    public String navn;
    public String orgnr;
    public StandardAdress adresse; // Definere ny adress klass
}
