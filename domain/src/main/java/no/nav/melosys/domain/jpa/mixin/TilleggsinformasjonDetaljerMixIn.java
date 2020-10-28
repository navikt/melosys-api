package no.nav.melosys.domain.jpa.mixin;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(value = PensjonOgUfoere.class),
    @JsonSubTypes.Type(value = BarnepensjonOgUnderholdsbidrag.class),
    @JsonSubTypes.Type(value = BonusFraForsvaret.class),
    @JsonSubTypes.Type(value = Etterbetalingsperiode.class),
    @JsonSubTypes.Type(value = Inntjeningsforhold.class),
    @JsonSubTypes.Type(value = Svalbardinntekt.class),
    @JsonSubTypes.Type(value = ReiseKostOgLosji.class),
})
public interface TilleggsinformasjonDetaljerMixIn {}
