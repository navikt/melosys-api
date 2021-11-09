package no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public record Familie(
    boolean minstEttOmfattetFamiliemedlem,

    String begrunnelseFritekstBarn,

    String begrunnelseFritekstEktefelle,

    Ektefelle ektefelle,

    List<Barn> barn
) {
}
