package no.nav.melosys.service.trygdeavtale;

import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;

import java.util.List;

public record TrygdeavtaleResultat(
    String virksomhet,
    String bestemmelse,
    AvklarteMedfolgendeFamilie familie) {

    public static class Builder {
        private String virksomhet;
        private String bestemmelse;
        private AvklarteMedfolgendeFamilie familie;

        public Builder virksomhet(String virksomhet) {
            this.virksomhet = virksomhet;
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
                virksomhet,
                bestemmelse,
                familie
            );
        }
    }
}
