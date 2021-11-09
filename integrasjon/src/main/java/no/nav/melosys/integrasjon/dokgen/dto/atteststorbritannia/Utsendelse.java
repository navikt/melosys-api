package no.nav.melosys.integrasjon.dokgen.dto.atteststorbritannia;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public record Utsendelse(
    Lovvalgbestemmelser_trygdeavtale_uk artikkel,
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
            utsendelse.artikkel(),
            utsendelse.oppholdsadresseUK(),
            utsendelse.startdato(),
            utsendelse.sluttdato()
        );
    }

    static public class Builder {
        private Lovvalgbestemmelser_trygdeavtale_uk artikkel;
        private List<String> oppholdsadresseUK;
        private Instant startdato;
        private Instant sluttdato;

        public static Builder builder() {
            return new Builder();
        }

        public Builder artikkel(Lovvalgbestemmelser_trygdeavtale_uk artikkel) {
            this.artikkel = artikkel;
            return this;
        }

        public Builder oppholdsadresseUK(List<String> oppholdsadresseUK) {
            this.oppholdsadresseUK = oppholdsadresseUK;
            return this;
        }

        public List<String> oppholdsadresseUK() {
            return oppholdsadresseUK;
        }

        public Builder startdato(Instant startdato) {
            this.startdato = startdato;
            return this;
        }

        public Builder sluttdato(Instant sluttdato) {
            this.sluttdato = sluttdato;
            return this;
        }

        public Utsendelse build() {
            return new Utsendelse(artikkel, oppholdsadresseUK, startdato, sluttdato);
        }
    }
}
