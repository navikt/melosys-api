package no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.attest;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public record Utsendelse(
    LovvalgBestemmelse artikkel,
    List<String> oppholdsadresse,

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(shape = STRING)
    LocalDate startdato,

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(shape = STRING)
    LocalDate sluttdato
) {
    public static Utsendelse av(no.nav.melosys.domain.brev.trygdeavtale.Utsendelse utsendelse) {
        if (utsendelse == null) return null;

        return new Utsendelse(
            utsendelse.artikkel(),
            utsendelse.oppholdsadresse(),
            utsendelse.startdato(),
            utsendelse.sluttdato()
        );
    }

    static public class Builder {
        private LovvalgBestemmelse artikkel;
        private List<String> oppholdsadresse;
        private LocalDate startdato;
        private LocalDate sluttdato;

        public static Builder builder() {
            return new Builder();
        }

        public Builder artikkel(LovvalgBestemmelse artikkel) {
            this.artikkel = artikkel;
            return this;
        }

        public Builder oppholdsadresse(List<String> oppholdsadresse) {
            this.oppholdsadresse = oppholdsadresse;
            return this;
        }

        public List<String> oppholdsadresse() {
            return oppholdsadresse;
        }

        public Builder startdato(LocalDate startdato) {
            this.startdato = startdato;
            return this;
        }

        public Builder sluttdato(LocalDate sluttdato) {
            this.sluttdato = sluttdato;
            return this;
        }

        public Utsendelse build() {
            return new Utsendelse(artikkel, oppholdsadresse, startdato, sluttdato);
        }
    }
}
