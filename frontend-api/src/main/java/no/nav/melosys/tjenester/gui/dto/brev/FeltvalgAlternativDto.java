package no.nav.melosys.tjenester.gui.dto.brev;

public class FeltvalgAlternativDto {
    private final String kode;
    private final String beskrivelse;

    public FeltvalgAlternativDto(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }

    public String getKode() {
        return kode;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public static final class Builder {
        private String kode;
        private String beskrivelse;

        public Builder medKode(String kode) {
            this.kode = kode;
            return this;
        }

        public Builder medBeskrivelse(String beskrivelse) {
            this.beskrivelse = beskrivelse;
            return this;
        }

        public FeltvalgAlternativDto build() {
            return new FeltvalgAlternativDto(kode, beskrivelse);
        }
    }
}
