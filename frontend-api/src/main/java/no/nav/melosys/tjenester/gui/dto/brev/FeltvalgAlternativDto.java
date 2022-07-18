package no.nav.melosys.tjenester.gui.dto.brev;

public class FeltvalgAlternativDto {

    private final String kode;
    private final String beskrivelse;
    private final boolean visFelt;

    public FeltvalgAlternativDto(FeltvalgAlternativKode feltvalgAlternativKode) {
        this.kode = feltvalgAlternativKode.getKode();
        this.beskrivelse = feltvalgAlternativKode.getBeskrivelse();
        this.visFelt = false;
    }

    public FeltvalgAlternativDto(FeltvalgAlternativKode feltvalgAlternativKode, boolean visFelt) {
        this.kode = feltvalgAlternativKode.getKode();
        this.beskrivelse = feltvalgAlternativKode.getBeskrivelse();
        this.visFelt = visFelt;
    }

    public FeltvalgAlternativDto(String kode, String beskrivelse, boolean visFelt) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
        this.visFelt = visFelt;
    }

    public String getKode() {
        return kode;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public boolean isVisFelt() {
        return visFelt;
    }

    public static final class Builder {
        private String kode;
        private String beskrivelse;
        private boolean visFelt;

        public Builder medKode(String kode) {
            this.kode = kode;
            return this;
        }

        public Builder medBeskrivelse(String beskrivelse) {
            this.beskrivelse = beskrivelse;
            return this;
        }

        public Builder medVisFelt(boolean visFelt) {
            this.visFelt = visFelt;
            return this;
        }

        public FeltvalgAlternativDto build() {
            return new FeltvalgAlternativDto(kode, beskrivelse, visFelt);
        }
    }
}
