package no.nav.melosys.service.trygdeavtale;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;

import java.time.LocalDate;

public record TrygdeavtaleResultat(
    String virksomhet,
    String bestemmelse,
    LocalDate lovvalgsperiodeFom,
    LocalDate lovvalgsperiodeTom,
    AvklarteMedfolgendeFamilie familie) {

    public static class Builder {
        private String virksomhet;
        private String bestemmelse;
        private LocalDate lovvalgsperiodeFom;
        private LocalDate lovvalgsperiodeTom;
        private AvklarteMedfolgendeFamilie familie;

        public Builder virksomhet(String virksomhet) {
            this.virksomhet = virksomhet;
            return this;
        }

        public Builder bestemmelse(String bestemmelse) {
            this.bestemmelse = bestemmelse;
            return this;
        }

        public Builder lovvalgsperiodeFom(LocalDate lovvalgsperiodeFom) {
            this.lovvalgsperiodeFom = lovvalgsperiodeFom;
            return this;
        }

        public Builder lovvalgsperiodeTom(LocalDate lovvalgsperiodeTom) {
            this.lovvalgsperiodeTom = lovvalgsperiodeTom;
            return this;
        }

        public Builder lovvalgsperiodeOgBestemmelse(Lovvalgsperiode lovvalgsperiode) {
            if (lovvalgsperiode == null) return this;
            this.bestemmelse = lovvalgsperiode.getBestemmelse().getKode();
            this.lovvalgsperiodeFom = lovvalgsperiode.getFom();
            this.lovvalgsperiodeTom = lovvalgsperiode.getTom();
            return this;
        }

        public Builder familie(AvklarteMedfolgendeFamilie familie) {
            this.familie = familie;
            return this;
        }

        public TrygdeavtaleResultat build() {
            return new TrygdeavtaleResultat(
                virksomhet,
                bestemmelse,
                lovvalgsperiodeFom,
                lovvalgsperiodeTom,
                familie
            );
        }
    }
}
