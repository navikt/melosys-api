package no.nav.melosys.tjenester.gui.dto.brev;

/**
 * Informasjon om et felt som skal være med i malen.
 *
 * Dersom {@param valg} ikke er null, vil instans av dette feltet være usynlig med mindre brukeren velger
 * et valgalternativ fra {@param valg} som har {@link FeltvalgAlternativDto#isVisFelt()} = true.
 */
public class BrevmalFeltDto {
    private final String kode;
    private final String beskrivelse;
    private final FeltType feltType;
    private final String hjelpetekst;
    private final boolean paakrevd;
    private final FeltValgDto valg;
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

    public FeltValgDto getValg() {
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
        private FeltValgDto valg;
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

        public Builder medValg(FeltValgDto valg) {
            this.valg = valg;
            return this;
        }

        public Builder medTegnBegrensning(Integer antallTegn) {
            this.tegnBegrensning = antallTegn;
            return this;
        }

        public BrevmalFeltDto build() {
            return new BrevmalFeltDto(this);
        }
    }
}
