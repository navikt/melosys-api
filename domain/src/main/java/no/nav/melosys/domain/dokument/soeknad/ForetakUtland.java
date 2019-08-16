package no.nav.melosys.domain.dokument.soeknad;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;

/**
 * Opplysninger om foretak i utlandet
 */
public class ForetakUtland {
    public String navn;
    public String orgnr;
    public StrukturertAdresse adresse = new StrukturertAdresse();

    @JsonIgnore
    public Boolean adresseErOgsåArbeidssted;
}
