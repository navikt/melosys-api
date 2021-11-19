package no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public record Barn(
    String navn,

    boolean omfattet,

    Medfolgende_barn_begrunnelser_ftrl begrunnelse,

    String fnr,

    String dnr,

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    LocalDate foedselsdato
) {
    public static class Builder {
        private String navn;

        private boolean omfattet;

        private Medfolgende_barn_begrunnelser_ftrl begrunnelse;

        private String fnr;

        private String dnr;

        private LocalDate foedselsdato;

        public Builder navn(String navn) {
            this.navn = navn;
            return this;
        }

        public Builder omfattet(boolean omfattet) {
            this.omfattet = omfattet;
            return this;
        }

        public Builder begrunnelse(Medfolgende_barn_begrunnelser_ftrl begrunnelse) {
            this.begrunnelse = begrunnelse;
            return this;
        }

        // Finn ut om dette er nødvedning eller om vi kan bruke
        // Medfolgende_barn_begrunnelser istenden
        public Builder begrunnelse(Medfolgende_barn_begrunnelser begrunnelse) {
            this.begrunnelse = Medfolgende_barn_begrunnelser_ftrl.valueOf(begrunnelse.getKode());
            return this;
        }

        public Builder fnr(String fnr) {
            this.fnr = fnr;
            return this;
        }

        public Builder dnr(String dnr) {
            this.dnr = dnr;
            return this;
        }


        public Builder foedselsdato(LocalDate foedselsdato) {
            this.foedselsdato = foedselsdato;
            return this;
        }

        public Barn build() {
            return new Barn(navn, omfattet, begrunnelse, fnr, dnr, foedselsdato);
        }
    }

}
