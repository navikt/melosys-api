package no.nav.melosys.integrasjon.dokgen.dto.atteststorbritannia;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public record Utsendelse(
    boolean artikkel6_1,
    boolean artikkel7_3,
    boolean artikkel6_5,
    List<String> oppholdsadresseUK,

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    Instant startdato,

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    Instant sluttdato
) {
    public static Utsendelse av(no.nav.melosys.domain.brev.storbritannia.Utsendelse utsendelse) {
        if (utsendelse == null) return null;

        return new Utsendelse(
            utsendelse.artikkel6_1(),
            utsendelse.artikkel7_3(),
            utsendelse.artikkel6_5(),
            utsendelse.oppholdsadresseUK(),
            utsendelse.startdato(),
            utsendelse.sluttdato()
        );
    }
}
