package no.nav.melosys.tjenester.gui.dto.brev;

public class FeltvalgDto {
    private final String kode;
    private final String beskrivelse;

    private FeltvalgDto(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
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

        public FeltvalgDto build() {
            return new FeltvalgDto(kode, beskrivelse);
        }
    }
}
