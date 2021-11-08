package no.nav.melosys.domain.brev;

public class FritekstbrevBrevbestilling extends DokgenBrevbestilling {
    private String fritekstTittel;
    private String fritekst;
    private boolean kontaktopplysninger;
    private String navnFullmektig;

    public FritekstbrevBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    public FritekstbrevBrevbestilling(FritekstbrevBrevbestilling.Builder builder) {
        super(builder);
        this.fritekstTittel = builder.fritekstTittel;
        this.fritekst = builder.fritekst;
        this.kontaktopplysninger = builder.kontaktopplysninger;
        this.navnFullmektig = builder.navnFullmektig;
    }

    public String getFritekstTittel() {
        return fritekstTittel;
    }

    public String getFritekst() {
        return fritekst;
    }

    public boolean isKontaktopplysninger() {
        return kontaktopplysninger;
    }

    public String getNavnFullmektig() {
        return navnFullmektig;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String fritekstTittel;
        private String fritekst;
        private boolean kontaktopplysninger;
        private String navnFullmektig;

        public Builder() {
        }

        public Builder(FritekstbrevBrevbestilling fritekstbrevBrevbestilling) {
            super(fritekstbrevBrevbestilling);
            this.fritekstTittel = fritekstbrevBrevbestilling.fritekstTittel;
            this.fritekst = fritekstbrevBrevbestilling.fritekst;
            this.kontaktopplysninger = fritekstbrevBrevbestilling.kontaktopplysninger;
            this.navnFullmektig = fritekstbrevBrevbestilling.navnFullmektig;
        }

        public Builder medFritekstTittel(String fritekstTittel) {
            this.fritekstTittel = fritekstTittel;
            return this;
        }

        public Builder medFritekst(String fritekst) {
            this.fritekst = fritekst;
            return this;
        }

        public Builder medKontaktopplysninger(boolean kontaktopplysninger) {
            this.kontaktopplysninger = kontaktopplysninger;
            return this;
        }

        public Builder medNavnFullmektig(String navnFullmektig) {
            this.navnFullmektig = navnFullmektig;
            return this;
        }

        public FritekstbrevBrevbestilling build() {
            return new FritekstbrevBrevbestilling(this);
        }
    }
}
