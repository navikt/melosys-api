package no.nav.melosys.domain.brev;

import no.nav.melosys.domain.kodeverk.Mottakerroller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Mottakerliste {
    private final Mottakerroller hovedMottaker;
    private final Collection<BrevkopiRegel> brevkopiRegler;
    private final Collection<Mottakerroller> kopiMottakere;
    private final Collection<NorskMyndighet> fasteMottakere;

    private Mottakerliste(Builder builder) {
        this.hovedMottaker = builder.hovedMottaker;
        this.brevkopiRegler = builder.brevkopiRegler;
        this.kopiMottakere = builder.kopiMottakere;
        this.fasteMottakere = builder.fasteMottakere;
    }

    public Mottakerroller getHovedMottaker() {
        return hovedMottaker;
    }

    public Collection<BrevkopiRegel> getBrevkopiRegler() {
        return brevkopiRegler;
    }

    public Collection<Mottakerroller> getKopiMottakere() {
        return kopiMottakere;
    }

    public Collection<NorskMyndighet> getFasteMottakere() {
        return fasteMottakere;
    }

    public boolean kanHaKopier() {
        return !brevkopiRegler.isEmpty();
    }

    public static class Builder {
        private Mottakerroller hovedMottaker;
        private Collection<BrevkopiRegel> brevkopiRegler = new ArrayList<>();
        private final Collection<Mottakerroller> kopiMottakere = new ArrayList<>();
        private final Collection<NorskMyndighet> fasteMottakere = new ArrayList<>();

        public Builder medHovedMottaker(Mottakerroller hovedMottaker) {
            this.hovedMottaker = hovedMottaker;
            return this;
        }

        public Builder medBrevkopiRegler(BrevkopiRegel... brevkopiRegler) {
            this.brevkopiRegler = List.of(brevkopiRegler);
            return this;
        }

        public Builder medKopiMottaker(Mottakerroller kopiMottaker) {
            this.kopiMottakere.add(kopiMottaker);
            return this;
        }

        public Builder medFastMottaker(NorskMyndighet fastMottaker) {
            this.fasteMottakere.add(fastMottaker);
            return this;
        }

        public Mottakerliste build() {
            return new Mottakerliste(this);
        }
    }
}
