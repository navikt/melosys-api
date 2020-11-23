package no.nav.melosys.domain.jpa.mixin;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.nav.melosys.domain.dokument.organisasjon.adresse.Gateadresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(value = SemistrukturertAdresse.class),
    @JsonSubTypes.Type(value = Gateadresse.class),
})
public interface GeografiskAdresseMixIn {}
