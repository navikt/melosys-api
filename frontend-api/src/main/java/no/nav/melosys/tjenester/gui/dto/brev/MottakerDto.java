package no.nav.melosys.tjenester.gui.dto.brev;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

import java.util.Collection;

public class MottakerDto {
    private final String type;
    private final Aktoersroller rolle;
    private final boolean egendefinert;
    private final Collection<MottakerAdresseDto> adresser;
    private final String feilmelding;

    private MottakerDto(Builder builder) {
        this.type = builder.type;
        this.rolle = builder.rolle;
        this.egendefinert = builder.egendefinert;
        this.adresser = builder.adresser;
        this.feilmelding = builder.feilmelding;
    }

    public String getType() {
        return type;
    }

    public Aktoersroller getRolle() {
        return rolle;
    }

    public boolean isEgendefinert() {
        return egendefinert;
    }

    public Collection<MottakerAdresseDto> getAdresser() {
        return adresser;
    }

    public String getFeilmelding() {
        return feilmelding;
    }

    public static final class Builder {
        private String type;
        private Aktoersroller rolle;
        private boolean egendefinert = false;
        private Collection<MottakerAdresseDto> adresser;
        private String feilmelding;

        public Builder medType(String type) {
            this.type = type;
            return this;
        }

        public Builder medRolle(Aktoersroller rolle) {
            this.rolle = rolle;
            return this;
        }

        public Builder egendefinert() {
            this.egendefinert = true;
            return this;
        }

        public Builder medAdresse(Collection<MottakerAdresseDto> adresser) {
            this.adresser = adresser;
            return this;
        }

        public Builder medFeilmelding(String feilmelding) {
            this.feilmelding = feilmelding;
            return this;
        }

        public MottakerDto build() {
            return new MottakerDto(this);
        }
    }
}
