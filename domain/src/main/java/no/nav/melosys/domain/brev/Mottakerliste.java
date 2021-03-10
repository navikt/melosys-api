package no.nav.melosys.domain.brev;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

import static java.util.Optional.ofNullable;

public class Mottakerliste {
    private Aktoersroller hovedMottaker;
    private List<Aktoersroller> kopiMottakere;
    private List<FastMottaker> fasteMottakere;

    public Mottakerliste(Builder builder) {
        this.hovedMottaker = builder.hovedMottaker;
        this.kopiMottakere = builder.kopiMottakere;
        this.fasteMottakere = builder.fasteMottakere;
    }

    public Aktoersroller getHovedMottaker() {
        return hovedMottaker;
    }

    public List<Aktoersroller> getKopiMottakere() {
        return ofNullable(kopiMottakere).orElse(new ArrayList<>());
    }

    public List<FastMottaker> getFasteMottakere() {
        return ofNullable(fasteMottakere).orElse(new ArrayList<>());
    }

    public void setKopiMottakere(List<Aktoersroller> kopiMottakere) {
        this.kopiMottakere = kopiMottakere;
    }

    public void setFasteMottakere(List<FastMottaker> fasteMottakere) {
        this.fasteMottakere = fasteMottakere;
    }

    public static class Builder {
        private Aktoersroller hovedMottaker;
        private List<Aktoersroller> kopiMottakere;
        private List<FastMottaker> fasteMottakere;

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
