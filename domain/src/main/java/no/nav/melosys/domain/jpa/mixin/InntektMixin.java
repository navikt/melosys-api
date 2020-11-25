package no.nav.melosys.domain.jpa.mixin;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.nav.melosys.domain.dokument.inntekt.inntektstype.Loennsinntekt;
import no.nav.melosys.domain.dokument.inntekt.inntektstype.Naeringsinntekt;
import no.nav.melosys.domain.dokument.inntekt.inntektstype.PensjonEllerTrygd;
import no.nav.melosys.domain.dokument.inntekt.inntektstype.YtelseFraOffentlige;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(value = Loennsinntekt.class),
    @JsonSubTypes.Type(value = Naeringsinntekt.class),
    @JsonSubTypes.Type(value = PensjonEllerTrygd.class),
    @JsonSubTypes.Type(value = YtelseFraOffentlige.class),
})
public interface InntektMixin {}
