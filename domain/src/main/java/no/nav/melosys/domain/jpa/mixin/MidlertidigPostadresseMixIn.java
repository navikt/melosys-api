package no.nav.melosys.domain.jpa.mixin;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseUtland;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(value = MidlertidigPostadresseNorge.class),
    @JsonSubTypes.Type(value = MidlertidigPostadresseUtland.class),
})
public interface MidlertidigPostadresseMixIn {}
