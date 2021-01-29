package no.nav.melosys.tjenester.gui.dto.brev;

public class FeltvalgDto {
    private final String inputType;
    private final String kode;
    private final String beskrivelse;
    private final String hjelpetekst;
    private final boolean paakrevd;

    private FeltvalgDto(String inputType, String kode, String beskrivelse, String hjelpetekst, boolean paakrevd) {
        this.inputType = inputType;
        this.kode = kode;
        this.beskrivelse = beskrivelse;
        this.hjelpetekst = hjelpetekst;
        this.paakrevd = paakrevd;
    }

    public static final class Builder {
        private String inputType;
        private String kode;
        private String beskrivelse;
        private String hjelpetekst;
        private boolean paakrevd = false;

        public Builder medInputType(String inputType) {
            this.inputType = inputType;
            return this;
        }

        public Builder medKode(String kode) {
            this.kode = kode;
            return this;
        }

        public Builder medBeskrivelse(String beskrivelse) {
            this.beskrivelse = beskrivelse;
            return this;
        }

        public Builder medHjelpetekst(String hjelpetekst) {
            this.hjelpetekst = hjelpetekst;
            return this;
        }

        public Builder erPåkrevd() {
            this.paakrevd = true;
            return this;
        }

        public FeltvalgDto build() {
            return new FeltvalgDto(inputType, kode, beskrivelse, hjelpetekst, paakrevd);
        }
    }
}
