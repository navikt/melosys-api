package no.nav.melosys.domain.behandlingsgrunnlag.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;

/**
 * Opplysninger om foretak i utlandet
 */
public class ForetakUtland {
    // Settes av frontend eller ved mapping fra SED for hvert foretak fordi orgnr ikke er påkrevd,
    // og defor ikke kan brukes som nøkkel
    public String uuid;

    public String navn;
    public String orgnr;
    public StrukturertAdresse adresse = new StrukturertAdresse();

    @JsonProperty("selvstendigNaeringsvirksomhet")
    public Boolean selvstendigNæringsvirksomhet;
}
