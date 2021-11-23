package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.ArrayList;
import java.util.List;

public class BrevmalFeltDto {
    private final String kode;
    private final String beskrivelse;
    private final FeltType feltType;
    private final String hjelpetekst;
    private final boolean paakrevd;
    private final List<FeltvalgDto> valg;
    private final Integer tegnBegrensning; // TODO: Verifiser i kontrolleren

    private BrevmalFeltDto(Builder builder) {
        this.kode = builder.kode;
        this.beskrivelse = builder.beskrivelse;
        this.feltType = builder.feltType;
        this.hjelpetekst = builder.hjelpetekst;
        this.paakrevd = builder.paakrevd;
        this.valg = builder.valg;
        this.tegnBegrensning = builder.tegnBegrensning;
    }

    public String getKode() {
        return kode;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public FeltType getFeltType() {
        return feltType;
    }

    public String getHjelpetekst() {
        return hjelpetekst;
    }

    public boolean isPaakrevd() {
        return paakrevd;
    }

    public List<FeltvalgDto> getValg() {
        return valg;
    }

    public Integer getTegnBegrensning() {
        return tegnBegrensning;
    }

    public static final class Builder {
        private String kode;
        private String beskrivelse;
        private FeltType feltType;
        private String hjelpetekst;
        private boolean paakrevd = false;
        private List<FeltvalgDto> valg;
        private Integer tegnBegrensning;

        public Builder medKode(String kode) {
            this.kode = kode;
            return this;
        }

        public Builder medBeskrivelse(String beskrivelse) {
            this.beskrivelse = beskrivelse;
            return this;
        }

        public Builder medFeltType(FeltType feltType) {
            this.feltType = feltType;
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

        public Builder medValg(List<FeltvalgDto> valg) {
            this.valg = valg;
            return this;
        }

        public Builder medTegnBegrensning(Integer antallTegn) {
            this.tegnBegrensning = antallTegn;
            return this;
        }

        public Builder medValg(FeltvalgDto valg) {
            if (this.valg == null) {
                this.valg = new ArrayList<>();
            }
            this.valg.add(valg);
            return this;
        }

        public BrevmalFeltDto build() {
            return new BrevmalFeltDto(this);
        }
    }
}
