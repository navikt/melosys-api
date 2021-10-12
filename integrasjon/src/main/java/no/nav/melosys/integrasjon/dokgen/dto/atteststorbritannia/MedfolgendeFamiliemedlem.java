package no.nav.melosys.integrasjon.dokgen.dto.atteststorbritannia;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public record MedfolgendeFamiliemedlem(
    String navn,

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    Instant foedselsdato,

    String fnr
) {
    public static MedfolgendeFamiliemedlem av(no.nav.melosys.domain.brev.storbritannia.MedfolgendeFamiliemedlem medfolgendeFamiliemedlem) {
        if (medfolgendeFamiliemedlem == null) return null;

        return new MedfolgendeFamiliemedlem(medfolgendeFamiliemedlem.navn(), medfolgendeFamiliemedlem.foedselsdato(), medfolgendeFamiliemedlem.fnr());
    }
}
