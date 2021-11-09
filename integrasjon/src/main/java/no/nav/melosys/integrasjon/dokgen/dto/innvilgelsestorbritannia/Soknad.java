package no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.brev.storbritannia.SoknadUKInnvilgelse;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public record Soknad(
    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    Instant soknadsdato,

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    Instant periodeFom,

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    Instant periodeTom,

    String virksomhetsnavn,

    String fritekstInnvilgelse,

    String fritekstBegrunnelse
) {
    public Soknad av (SoknadUKInnvilgelse soknad) {
        return new Soknad(
          soknad.
        );
    }
}
