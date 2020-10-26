package no.nav.melosys.domain.dokument;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.nav.melosys.domain.dokument.person.PersonDokument;

/**
 * Superklasse for alle dokumenter.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes( {
    // FIXME Migrere og legge til alle dokumenttyper + fjerne SaksopplysningListener
    @JsonSubTypes.Type(value = PersonDokument.class),
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface SaksopplysningDokument extends Dokument {

}
