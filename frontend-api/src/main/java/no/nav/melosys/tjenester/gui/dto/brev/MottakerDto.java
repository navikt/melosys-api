package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.List;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

public class MottakerDto {
    private final String type;
    private final Aktoersroller rolle;
    private final String orgnr;
    private final boolean kanOverstyres;
    private final List<String> adresselinjer;

    private MottakerDto(Builder builder) {
        this.type = builder.type;
        this.rolle = builder.rolle;
        this.orgnr = builder.orgnr;
        this.kanOverstyres = builder.kanOverstyres;
        this.adresselinjer = builder.adresselinjer;
    }

    public static final class Builder {
        private String type;
        private Aktoersroller rolle;
        private String orgnr;
        private boolean kanOverstyres = false;
        private List<String> adresselinjer;

        public Builder medType(String type) {
            this.type = type;
            return this;
        }

        public Builder medRolle(Aktoersroller rolle) {
            this.rolle = rolle;
            return this;
        }

        public Builder medOrgnr(String orgnr) {
            this.orgnr = orgnr;
            return this;
        }

        public Builder kanOverstyres() {
            this.kanOverstyres = true;
            return this;
        }

        public Builder medAdresselinjer(List<String> adresselinjer) {
            this.adresselinjer = adresselinjer;
            return this;
        }

        public MottakerDto build() {
            return new MottakerDto(this);
        }
    }
}
