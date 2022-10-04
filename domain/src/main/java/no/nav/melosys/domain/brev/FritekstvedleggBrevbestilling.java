package no.nav.melosys.domain.brev;

public class FritekstvedleggBrevbestilling extends DokgenBrevbestilling {
    private final String fritekstTittel;
    private final String fritekst;

    public FritekstvedleggBrevbestilling(FritekstvedleggBrevbestilling.Builder builder) {
        super(builder);
        this.fritekstTittel = builder.fritekstTittel;
        this.fritekst = builder.fritekst;
    }

    public String getFritekstTittel() {
        return fritekstTittel;
    }

    public String getFritekst() {
        return fritekst;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String fritekstTittel;
        private String fritekst;

        public Builder() {
        }

        public Builder(FritekstvedleggBrevbestilling fritekstvedleggBrevbestilling) {
            super(fritekstvedleggBrevbestilling);
            this.fritekstTittel = fritekstvedleggBrevbestilling.fritekstTittel;
            this.fritekst = fritekstvedleggBrevbestilling.fritekst;
        }

        public Builder medFritekstTittel(String fritekstTittel) {
            this.fritekstTittel = fritekstTittel;
            return this;
        }

        public Builder medFritekst(String fritekst) {
            this.fritekst = fritekst;
            return this;
        }

        public FritekstvedleggBrevbestilling build() {
            return new FritekstvedleggBrevbestilling(this);
        }
    }
}
