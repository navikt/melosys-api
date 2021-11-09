package no.nav.melosys.domain.brev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

public class Mottakerliste {
    private final Aktoersroller hovedMottaker;
    private final Collection<BrevkopiRegel> brevkopiRegler;
    private final Collection<Aktoersroller> kopiMottakere;
    private final Collection<FastMottaker> fasteMottakere;

    private Mottakerliste(Builder builder) {
        this.hovedMottaker = builder.hovedMottaker;
        this.brevkopiRegler = builder.brevkopiRegler;
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

    public Collection<BrevkopiRegel> getBrevkopiRegler() {
        return brevkopiRegler;
    }

    public boolean kanHaKopier() {
        return !brevkopiRegler.isEmpty();
    }

    public static class Builder {
        private Aktoersroller hovedMottaker;
        private Collection<BrevkopiRegel> brevkopiRegler = new ArrayList<>();
        private Collection<Aktoersroller> kopiMottakere = new ArrayList<>();
        private Collection<FastMottaker> fasteMottakere = new ArrayList<>();

        public Builder medHovedMottaker(Aktoersroller hovedMottaker) {
            this.hovedMottaker = hovedMottaker;
            return this;
        }

        public Builder medBrevkopiRegler(BrevkopiRegel... brevkopiRegler) {
            this.brevkopiRegler = List.of(brevkopiRegler);
            return this;
        }

        public Builder medKopiMottaker(Aktoersroller kopiMottaker) {
            this.kopiMottakere.add(kopiMottaker);
            return this;
        }

        public Builder medFastMottaker(FastMottaker fastMottaker) {
            this.fasteMottakere.add(fastMottaker);
            return this;
        }

        public Mottakerliste build() {
            return new Mottakerliste(this);
        }
    }
}
