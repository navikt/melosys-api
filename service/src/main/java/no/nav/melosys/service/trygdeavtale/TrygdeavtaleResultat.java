package no.nav.melosys.service.trygdeavtale;

import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;

import java.util.List;

public record TrygdeavtaleResultat(
    List<String> virksomheter,
    String bestemmelse,
    AvklarteMedfolgendeFamilie familie) {

    public static class Builder {
        private List<String> virksomheter;
        private String bestemmelse;
        private AvklarteMedfolgendeFamilie familie;

        public Builder virksomheter(List<String> virksomheter) {
            this.virksomheter = virksomheter;
            return this;
        }

        public Builder bestemmelse(String bestemmelse) {
            this.bestemmelse = bestemmelse;
            return this;
        }

        public Builder familie(AvklarteMedfolgendeFamilie familie) {
            this.familie = familie;
            return this;
        }

        public TrygdeavtaleResultat build() {
            return new TrygdeavtaleResultat(
                virksomheter,
                bestemmelse,
                familie
            );
        }
    }
}
