package no.nav.melosys.domain.brev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

public class Mottakerliste {
    private final Aktoersroller hovedMottaker;
    private final Collection<Aktoersroller> kopiMottakere;
    private final Collection<FastMottaker> fasteMottakere;

    public Mottakerliste(Builder builder) {
        this.hovedMottaker = builder.hovedMottaker;
        this.kopiMottakere = builder.kopiMottakere;
        this.fasteMottakere = builder.fasteMottakere;
    }

    public Aktoersroller getHovedMottaker() {
        return hovedMottaker;
    }

    public Collection<Aktoersroller> getKopiMottakere() {
        return kopiMottakere;
    }

    public Collection<FastMottaker> getFasteMottakere() {
        return fasteMottakere;
    }

    public static class Builder {
        private Aktoersroller hovedMottaker;
        private Collection<Aktoersroller> kopiMottakere = new ArrayList<>();
        private Collection<FastMottaker> fasteMottakere = new ArrayList<>();

        public Builder medHovedMottaker(Aktoersroller hovedMottaker) {
            this.hovedMottaker = hovedMottaker;
            return this;
        }

        public Builder medKopiMottakere(Aktoersroller... kopiMottakere) {
            this.kopiMottakere = List.of(kopiMottakere);
            return this;
        }

        public Builder medFasteMottakere(FastMottaker... fasteMottakere) {
            this.fasteMottakere = List.of(fasteMottakere);
            return this;
        }

        public Mottakerliste build() {
            return new Mottakerliste(this);
        }

        public Builder medKopiMottakere(List<Aktoersroller> kopiMottakere) {
            this.kopiMottakere = kopiMottakere;
            return this;
        }

        public Builder medFasteMottakere(List<FastMottaker> fasteMottakere) {
            this.fasteMottakere = fasteMottakere;
            return this;
        }
    }
}
