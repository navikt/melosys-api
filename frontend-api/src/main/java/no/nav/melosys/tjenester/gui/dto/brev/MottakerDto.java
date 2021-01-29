package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.List;

import no.nav.melosys.domain.kodeverk.Aktoersroller;

public class MottakerDto {
    private final String type;
    private final Aktoersroller rolle;
    private final boolean kanOverstyres;
    private final List<String> adresselinjer;

    private MottakerDto(String type, Aktoersroller rolle, boolean kanOverstyres, List<String> adresselinjer) {
        this.type = type;
        this.rolle = rolle;
        this.kanOverstyres = kanOverstyres;
        this.adresselinjer = adresselinjer;
    }

    public static final class Builder {
        private String type;
        private Aktoersroller rolle;
        private boolean kanOverstyres;
        private List<String> adresselinjer;

        public Builder medType(String type) {
            this.type = type;
            return this;
        }

        public Builder medRolle(Aktoersroller rolle) {
            this.rolle = rolle;
            return this;
        }

        public Builder medKanOverstyres(boolean kanOverstyres) {
            this.kanOverstyres = kanOverstyres;
            return this;
        }

        public Builder medAdresselinjer(List<String> adresselinjer) {
            this.adresselinjer = adresselinjer;
            return this;
        }

        public MottakerDto build() {
            return new MottakerDto(type, rolle, kanOverstyres, adresselinjer);
        }
    }
}
