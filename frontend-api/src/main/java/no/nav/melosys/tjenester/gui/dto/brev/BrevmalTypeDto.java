package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.List;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

public class BrevmalTypeDto {
    private final Produserbaredokumenter type;
    private final List<BrevmalFeltDto> felter;

    private BrevmalTypeDto(Produserbaredokumenter type, List<BrevmalFeltDto> felter) {
        this.type = type;
        this.felter = felter;
    }

    public Produserbaredokumenter getType() {
        return type;
    }

    public List<BrevmalFeltDto> getFelter() {
        return felter;
    }

    public static final class Builder {
        private Produserbaredokumenter type;
        private List<BrevmalFeltDto> felter;

        public Builder medType(Produserbaredokumenter type) {
            this.type = type;
            return this;
        }

        public Builder medFelter(List<BrevmalFeltDto> felter) {
            this.felter = felter;
            return this;
        }

        public BrevmalTypeDto build() {
            return new BrevmalTypeDto(type, felter);
        }
    }
}
