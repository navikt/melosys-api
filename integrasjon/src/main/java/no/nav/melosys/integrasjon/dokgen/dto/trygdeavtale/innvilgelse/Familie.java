package no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.innvilgelse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({"minstEttOmfattetFamiliemedlem", "ektefelle", "barn"})
public record Familie(
    Ektefelle ektefelle,
    List<Barn> barn
) {
    @JsonProperty
    public boolean minstEttOmfattetFamiliemedlem() {
        if (ektefelle != null && ektefelle.omfattet()) return true;
        if (barn == null) return false;
        return barn.stream().anyMatch(Barn::omfattet);
    }

    public static class Builder {
        private Ektefelle ektefelle;
        private List<Barn> barn;

        public Builder ektefelle(Ektefelle ektefelle) {
            this.ektefelle = ektefelle;
            return this;
        }

        public Builder barn(List<Barn> barn) {
            this.barn = barn;
            return this;
        }

        public Familie build() {
            return new Familie(ektefelle, barn);
        }
    }
}
