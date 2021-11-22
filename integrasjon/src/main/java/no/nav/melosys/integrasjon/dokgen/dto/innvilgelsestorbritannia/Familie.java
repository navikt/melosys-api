package no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia;

import java.util.List;

public record Familie(
    boolean minstEttOmfattetFamiliemedlem,
    Ektefelle ektefelle,
    List<Barn> barn
) {
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

        private boolean harMinstEttOmfattetFamiliemedlem() {
            if (ektefelle.omfattet()) return true;
            return barn.stream().anyMatch(Barn::omfattet);
        }

        public Familie build() {
            return new Familie(harMinstEttOmfattetFamiliemedlem(), ektefelle, barn);
        }
    }
}
