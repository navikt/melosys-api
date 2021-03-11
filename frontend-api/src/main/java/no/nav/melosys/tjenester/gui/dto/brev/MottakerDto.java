package no.nav.melosys.tjenester.gui.dto.brev;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

public class MottakerDto {
    private final String type;
    private final Aktoersroller rolle;
    private final boolean frittValg;

    private MottakerDto(Builder builder) {
        this.type = builder.type;
        this.rolle = builder.rolle;
        this.frittValg = builder.frittValg;
    }

    public String getType() {
        return type;
    }

    public Aktoersroller getRolle() {
        return rolle;
    }

    public boolean isFrittValg() {
        return frittValg;
    }

    public static final class Builder {
        private String type;
        private Aktoersroller rolle;
        private boolean frittValg = false;

        public Builder medType(String type) {
            this.type = type;
            return this;
        }

        public Builder medRolle(Aktoersroller rolle) {
            this.rolle = rolle;
            return this;
        }

        public Builder frittValg() {
            this.frittValg = true;
            return this;
        }

        public MottakerDto build() {
            return new MottakerDto(this);
        }
    }
}
